#!/data/data/com.termux/files/usr/bin/bash
# Arranca el servidor de MouseFrag.
# Ajusta ADB_PATH si tu SDK quedo en otra ruta.

export ADB_PATH="$HOME/android-sdk/platform-tools/adb"
export PORT=8087

cd "$(dirname "$0")"
python mousefrag_server.py
