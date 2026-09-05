# MouseFrag

App para conectar mouse y teclado a juegos moviles via ADB inalambrico.
100% legal, sin anti-recoil ni auto-click: solo mapea input externo a
toques en pantalla, igual que Panda Mouse Pro u Octopus.

Desarrollada 100% desde el celular con Termux (sin computadora).

## Arquitectura

El proyecto tiene DOS partes que trabajan juntas:

1. **`server/`** — Un servidor Python que corre en Termux y usa el
   binario `adb` real (de platform-tools) para emparejar, conectar y
   mandar comandos de input. Se necesita el `adb` real porque el
   emparejamiento de "Depuracion inalambrica" (Android 11+) usa un
   protocolo criptografico (TLS + SPAKE2) que una app Android normal
   no puede replicar facilmente por su cuenta.

2. **`app/`** — La app Android MouseFrag en si. No habla ADB
   directamente: le habla al servidor de Termux por HTTP local
   (`http://127.0.0.1:8087`), y el servidor es quien de verdad mueve
   el `adb`.

```
[App MouseFrag] --HTTP local--> [servidor Python en Termux] --adb--> [tu juego]
```

## Requisitos (todo en Termux)

```bash
pkg install openjdk-21 wget unzip git python -y
```

Y el Android SDK command-line tools + platform-tools + build-tools ya
instalados (ver seccion "Setup del SDK" mas abajo si es una instalacion
nueva).

## Setup del SDK (solo la primera vez)

```bash
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
unzip commandlinetools-linux-15859902_latest.zip
mv cmdline-tools latest

echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## Fix necesario: aapt2 en Termux (arquitectura ARM)

Android Gradle Plugin trae un `aapt2` compilado para x86_64, pero tu
celular es ARM64. Hay que decirle a Gradle que use el aapt2 nativo de
Termux:

```bash
pkg install aapt2 -y
mkdir -p ~/.gradle
echo "android.aapt2FromMavenOverride=$(which aapt2)" >> ~/.gradle/gradle.properties
```

## Compilar la app

```bash
git clone <URL-DE-TU-REPO> ~/MouseFrag
cd ~/MouseFrag
echo "sdk.dir=$HOME/android-sdk" > local.properties

# Primera vez: genera el wrapper de Gradle
pkg install gradle -y
gradle wrapper --gradle-version 8.7
chmod +x gradlew

./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
Cópialo a Descargas e instálalo:

```bash
termux-setup-storage
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/mousefrag.apk
```

## Correr el servidor

Antes de abrir la app, en OTRA sesión de Termux (o con la app en
segundo plano):

```bash
cd ~/MouseFrag/server
bash start_server.sh
```

Déjalo corriendo. Si `ADB_PATH` quedó en otra ruta en tu celular,
edita `start_server.sh` o corre:

```bash
export ADB_PATH=/ruta/real/a/adb
python mousefrag_server.py
```

## Uso paso a paso

1. En tu celular (el mismo u otro): Ajustes > Opciones de
   desarrollador > Depuración inalámbrica > **Emparejar dispositivo
   con código**. Anota IP, puerto y código de 6 dígitos.
2. Abre MouseFrag > "Ir a conectar" > pega esos datos en la sección
   "1. Emparejar" > toca **Emparejar**.
3. Vuelve a la pantalla principal de Depuración inalámbrica, anota la
   IP y puerto que aparecen ahí (son distintos a los de emparejar).
4. Pégalos en la sección "2. Conectar" > toca **Conectar**.
5. Ve a "Panel de control" y prueba el botón de toque de prueba.

## Estructura de carpetas

```
MouseFrag/
├── server/
│   ├── mousefrag_server.py   # servidor HTTP que usa adb
│   └── start_server.sh       # script de arranque rapido
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/yesithx/mousefrag/
│       │   ├── MainActivity.java
│       │   ├── ConnectActivity.java
│       │   ├── ControlActivity.java
│       │   └── ServerClient.java   # cliente HTTP hacia el servidor
│       └── res/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Roadmap (fases)

**Fase 1 (actual):** conexion ADB, tap de prueba, estructura base.

**Fase 2:** editor visual de zonas de disparo/joystick, macros (combos
+ grabar/repetir), crosshair personalizable, overlay de FPS/ping,
sensibilidad configurable, perfiles guardados por juego.

**Fase 3:** grabacion de pantalla/clips, soporte multi-mouse/teclado,
sensibilidad avanzada separada por eje/zoom, compartir configuraciones
de sensibilidad (sin dar ventaja competitiva).

Todo lo que se agregue debe mantenerse dentro de lo permitido por
Play Store: sin anti-recoil, sin auto-click/turbo, sin nada que de
ventaja tipo cheat en juegos multijugador.
