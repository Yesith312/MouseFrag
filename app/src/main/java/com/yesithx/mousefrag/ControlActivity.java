package com.yesithx.mousefrag;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Pantalla minima para probar que todo el flujo funciona:
 * app -> servidor Termux -> adb -> dispositivo conectado.
 *
 * Esta es la base sobre la que despues se construyen el editor de
 * zonas, las macros, el crosshair, etc. (Fase 2 en adelante).
 */
public class ControlActivity extends Activity {

    private ServerClient client;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new ServerClient();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Panel de control (prueba)");
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(16));

        Button tapTestButton = new Button(this);
        tapTestButton.setText("Mandar toque de prueba (500, 800)");
        tapTestButton.setOnClickListener(v -> {
            resultText.setText("Enviando...");
            client.tap(500, 800, (ok, message) ->
                    resultText.setText((ok ? "Toque enviado. " : "Fallo. ") + message));
        });

        resultText = new TextView(this);
        resultText.setGravity(Gravity.CENTER);
        resultText.setPadding(0, dp(16), 0, 0);

        root.addView(title);
        root.addView(tapTestButton);
        root.addView(resultText);

        setContentView(root);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
