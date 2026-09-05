package com.yesithx.mousefrag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("MouseFrag");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Antes de empezar, corre el servidor en Termux:\n\n" +
                "cd ~/MouseFrag/server\nbash start_server.sh");
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(16), 0, dp(24));

        Button connectButton = new Button(this);
        connectButton.setText("Ir a conectar");
        connectButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ConnectActivity.class)));

        root.addView(title);
        root.addView(subtitle);
        root.addView(connectButton);

        setContentView(root);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
