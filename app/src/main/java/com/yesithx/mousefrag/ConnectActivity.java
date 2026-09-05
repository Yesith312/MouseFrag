package com.yesithx.mousefrag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ConnectActivity extends Activity {

    private ServerClient client;
    private TextView statusText;

    private EditText pairIp;
    private EditText pairPort;
    private EditText pairCode;

    private EditText connectIp;
    private EditText connectPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new ServerClient();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);

        // --- Paso 1: emparejar ---
        TextView step1 = sectionTitle("1. Emparejar (solo la primera vez)");
        TextView step1Help = helpText(
                "En tu telefono: Ajustes > Opciones de desarrollador > " +
                "Depuracion inalambrica > Emparejar dispositivo con codigo. " +
                "Copia aqui la IP, el puerto y el codigo que te muestre.");

        pairIp = inputField("IP de emparejamiento (ej: 192.168.1.5)");
        pairPort = inputField("Puerto de emparejamiento (ej: 41234)");
        pairCode = inputField("Codigo de 6 digitos");

        Button pairButton = new Button(this);
        pairButton.setText("Emparejar");
        pairButton.setOnClickListener(v -> doPair());

        // --- Paso 2: conectar ---
        TextView step2 = sectionTitle("2. Conectar");
        TextView step2Help = helpText(
                "Esta es la IP y puerto que aparecen en la pantalla principal " +
                "de Depuracion inalambrica (distinto al de emparejar).");

        connectIp = inputField("IP de conexion (ej: 192.168.1.5)");
        connectPort = inputField("Puerto de conexion (ej: 37021)");

        Button connectButton = new Button(this);
        connectButton.setText("Conectar");
        connectButton.setOnClickListener(v -> doConnect());

        Button statusButton = new Button(this);
        statusButton.setText("Ver estado del servidor");
        statusButton.setOnClickListener(v -> doStatus());

        Button goToControl = new Button(this);
        goToControl.setText("Ir al panel de control");
        goToControl.setOnClickListener(v ->
                startActivity(new Intent(ConnectActivity.this, ControlActivity.class)));

        statusText = new TextView(this);
        statusText.setText("Estado: sin acciones todavia");
        statusText.setPadding(0, dp(16), 0, dp(16));

        root.addView(step1);
        root.addView(step1Help);
        root.addView(pairIp);
        root.addView(pairPort);
        root.addView(pairCode);
        root.addView(pairButton);

        root.addView(step2);
        root.addView(step2Help);
        root.addView(connectIp);
        root.addView(connectPort);
        root.addView(connectButton);

        root.addView(statusButton);
        root.addView(statusText);
        root.addView(goToControl);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void doPair() {
        setStatus("Emparejando...");
        client.pair(
                pairIp.getText().toString().trim(),
                pairPort.getText().toString().trim(),
                pairCode.getText().toString().trim(),
                (ok, message) -> setStatus((ok ? "Emparejado. " : "Fallo al emparejar. ") + message)
        );
    }

    private void doConnect() {
        setStatus("Conectando...");
        client.connect(
                connectIp.getText().toString().trim(),
                connectPort.getText().toString().trim(),
                (ok, message) -> setStatus((ok ? "Conectado. " : "Fallo al conectar. ") + message)
        );
    }

    private void doStatus() {
        setStatus("Consultando servidor...");
        client.status((ok, message) -> setStatus(ok
                ? ("Servidor activo.\n" + message)
                : ("No se pudo hablar con el servidor.\n" + message
                    + "\n\nVerifica que corriste 'bash start_server.sh' en Termux.")));
    }

    private void setStatus(String text) {
        statusText.setText("Estado: " + text);
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setPadding(0, dp(20), 0, dp(4));
        return tv;
    }

    private TextView helpText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private EditText inputField(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
        return edit;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
