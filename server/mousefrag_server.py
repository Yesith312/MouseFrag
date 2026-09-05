#!/usr/bin/env python3
"""
MouseFrag Server
-----------------
Corre en Termux y expone una API HTTP local (solo accesible desde el
mismo dispositivo) que la app Android MouseFrag usa para:
  - Emparejar con el modo "Depuracion inalambrica" (ADB pairing)
  - Conectar al dispositivo ya emparejado
  - Mandar toques (tap), deslizamientos (swipe) y teclas (keyevent)

No reemplaza a "adb": lo USA. Este script solo llama al binario adb
real que ya tienes en la carpeta platform-tools del SDK, porque ese
binario si sabe hacer el protocolo de emparejamiento (SPAKE2 + TLS)
que MouseFrag como app sola no puede replicar facilmente.

Uso:
    python mousefrag_server.py

Variables de entorno opcionales:
    ADB_PATH   Ruta al binario adb (si no se detecta automaticamente)
    PORT       Puerto donde escucha el servidor (default 8087)
"""

import json
import os
import shutil
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PORT = int(os.environ.get("PORT", "8087"))


def find_adb():
    """Busca el binario adb en ubicaciones tipicas de Termux + Android SDK."""
    env_path = os.environ.get("ADB_PATH")
    if env_path and os.path.isfile(env_path):
        return env_path

    which_adb = shutil.which("adb")
    if which_adb:
        return which_adb

    home = os.path.expanduser("~")
    candidates = [
        os.path.join(home, "android-sdk", "platform-tools", "adb"),
        os.path.join(home, "Android", "sdk", "platform-tools", "adb"),
    ]
    for c in candidates:
        if os.path.isfile(c):
            return c

    raise FileNotFoundError(
        "No encontre el binario adb. Define la variable de entorno "
        "ADB_PATH con la ruta completa, ej:\n"
        "  export ADB_PATH=$HOME/android-sdk/platform-tools/adb"
    )


ADB = find_adb()


def run_adb(args, timeout=20):
    """Corre un comando adb y devuelve (exit_code, stdout, stderr)."""
    try:
        result = subprocess.run(
            [ADB] + args,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        return result.returncode, result.stdout.strip(), result.stderr.strip()
    except subprocess.TimeoutExpired:
        return -1, "", "timeout esperando respuesta de adb"


def json_response(handler, status, payload):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


class Handler(BaseHTTPRequestHandler):
    # Silencia el log por defecto de BaseHTTPRequestHandler para no
    # llenar la terminal de Termux; comenta esto si quieres ver todo.
    def log_message(self, format, *args):
        pass

    def _read_json_body(self):
        length = int(self.headers.get("Content-Length", 0))
        if length == 0:
            return {}
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            return {}

    def do_GET(self):
        if self.path == "/status":
            code, out, err = run_adb(["devices"])
            json_response(self, 200, {"ok": code == 0, "devices": out, "error": err})
            return
        json_response(self, 404, {"ok": False, "error": "ruta no encontrada"})

    def do_POST(self):
        body = self._read_json_body()

        if self.path == "/pair":
            ip = body.get("ip")
            port = body.get("port")
            code = body.get("code")
            if not (ip and port and code):
                json_response(self, 400, {"ok": False, "error": "faltan ip, port o code"})
                return
            # adb pair espera el codigo por stdin cuando se le pasa
            # host:port como argumento, asi que se lo mandamos via stdin.
            try:
                result = subprocess.run(
                    [ADB, "pair", f"{ip}:{port}"],
                    input=f"{code}\n",
                    capture_output=True,
                    text=True,
                    timeout=20,
                )
                ok = "Successfully paired" in result.stdout
                json_response(self, 200, {
                    "ok": ok,
                    "output": result.stdout.strip(),
                    "error": result.stderr.strip(),
                })
            except subprocess.TimeoutExpired:
                json_response(self, 200, {"ok": False, "error": "timeout emparejando"})
            return

        if self.path == "/connect":
            ip = body.get("ip")
            port = body.get("port")
            if not (ip and port):
                json_response(self, 400, {"ok": False, "error": "faltan ip o port"})
                return
            code, out, err = run_adb(["connect", f"{ip}:{port}"])
            ok = "connected" in out.lower() and "failed" not in out.lower()
            json_response(self, 200, {"ok": ok, "output": out, "error": err})
            return

        if self.path == "/tap":
            x = body.get("x")
            y = body.get("y")
            if x is None or y is None:
                json_response(self, 400, {"ok": False, "error": "faltan x o y"})
                return
            code, out, err = run_adb(["shell", "input", "tap", str(x), str(y)])
            json_response(self, 200, {"ok": code == 0, "output": out, "error": err})
            return

        if self.path == "/swipe":
            x1, y1 = body.get("x1"), body.get("y1")
            x2, y2 = body.get("x2"), body.get("y2")
            duration = body.get("duration", 100)
            if None in (x1, y1, x2, y2):
                json_response(self, 400, {"ok": False, "error": "faltan coordenadas"})
                return
            code, out, err = run_adb([
                "shell", "input", "swipe",
                str(x1), str(y1), str(x2), str(y2), str(duration),
            ])
            json_response(self, 200, {"ok": code == 0, "output": out, "error": err})
            return

        if self.path == "/key":
            keycode = body.get("keycode")
            if keycode is None:
                json_response(self, 400, {"ok": False, "error": "falta keycode"})
                return
            code, out, err = run_adb(["shell", "input", "keyevent", str(keycode)])
            json_response(self, 200, {"ok": code == 0, "output": out, "error": err})
            return

        json_response(self, 404, {"ok": False, "error": "ruta no encontrada"})


def main():
    print(f"MouseFrag server usando adb en: {ADB}")
    print(f"Escuchando en http://127.0.0.1:{PORT}")
    server = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nDeteniendo servidor...")
        server.shutdown()


if __name__ == "__main__":
    main()
