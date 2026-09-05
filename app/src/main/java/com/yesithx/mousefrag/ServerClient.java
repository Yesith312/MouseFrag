package com.yesithx.mousefrag;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ServerClient habla por HTTP con mousefrag_server.py, que corre en
 * Termux en el mismo celular (http://127.0.0.1:PUERTO).
 *
 * Todas las llamadas de red se hacen en un hilo aparte (Android no deja
 * hacer red en el hilo principal) y el resultado se entrega en el hilo
 * principal a traves de un Callback, para que sea facil actualizar la UI.
 */
public class ServerClient {

    public interface Callback {
        void onResult(boolean ok, String message);
    }

    private static final int DEFAULT_PORT = 8087;
    private final String baseUrl;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ServerClient() {
        this(DEFAULT_PORT);
    }

    public ServerClient(int port) {
        this.baseUrl = "http://127.0.0.1:" + port;
    }

    public void pair(String ip, String port, String code, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("ip", ip);
            body.put("port", port);
            body.put("code", code);
        } catch (Exception ignored) {
        }
        post("/pair", body, callback);
    }

    public void connect(String ip, String port, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("ip", ip);
            body.put("port", port);
        } catch (Exception ignored) {
        }
        post("/connect", body, callback);
    }

    public void tap(int x, int y, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("x", x);
            body.put("y", y);
        } catch (Exception ignored) {
        }
        post("/tap", body, callback);
    }

    public void swipe(int x1, int y1, int x2, int y2, int durationMs, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("x1", x1);
            body.put("y1", y1);
            body.put("x2", x2);
            body.put("y2", y2);
            body.put("duration", durationMs);
        } catch (Exception ignored) {
        }
        post("/swipe", body, callback);
    }

    public void key(int keycode, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("keycode", keycode);
        } catch (Exception ignored) {
        }
        post("/key", body, callback);
    }

    public void status(Callback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(baseUrl + "/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);
                String response = readStream(conn.getInputStream());
                deliver(callback, true, response);
            } catch (Exception e) {
                deliver(callback, false, "No se pudo conectar al servidor: " + e.getMessage());
            }
        });
    }

    private void post(String path, JSONObject body, Callback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(baseUrl + path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }

                int status = conn.getResponseCode();
                InputStream stream = (status >= 200 && status < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();
                String response = readStream(stream);

                JSONObject json = new JSONObject(response);
                boolean ok = json.optBoolean("ok", false);
                String message = json.optString("output",
                        json.optString("error", response));
                deliver(callback, ok, message);
            } catch (Exception e) {
                deliver(callback, false, "Error de conexion: " + e.getMessage());
            }
        });
    }

    private String readStream(InputStream input) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    private void deliver(Callback callback, boolean ok, String message) {
        mainHandler.post(() -> callback.onResult(ok, message));
    }
}
