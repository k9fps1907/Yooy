package com.ds4.light;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.InputDevice;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    static final int N = 4;
    // static: sobrevive se a tela for recriada
    static final List<LightsManager> mgrs = new ArrayList<>();
    static final List<Light> lights = new ArrayList<>();
    static final List<LightsManager.LightsSession> sessions = new ArrayList<>();

    final EditText[][] campos = new EditText[N][3];
    TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        sv.addView(root);
        setContentView(sv);

        status = new TextView(this);
        status.setTextSize(16);
        root.addView(status);

        String[] rotulos = {"R", "G", "B"};
        for (int i = 0; i < N; i++) {
            TextView t = new TextView(this);
            t.setText("Controle " + (i + 1));
            t.setTextSize(18);
            t.setPadding(0, 40, 0, 10);
            root.addView(t);

            LinearLayout linha = new LinearLayout(this);
            for (int j = 0; j < 3; j++) {
                EditText e = new EditText(this);
                e.setHint(rotulos[j]);
                e.setInputType(InputType.TYPE_CLASS_NUMBER);
                e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                e.setGravity(Gravity.CENTER);
                linha.addView(e, new LinearLayout.LayoutParams(0, -2, 1));
                campos[i][j] = e;
            }
            root.addView(linha);
        }

        Button aplicar = new Button(this);
        aplicar.setText("Aplicar");
        aplicar.setOnClickListener(v -> aplicar());
        root.addView(aplicar);

        Button soltar = new Button(this);
        soltar.setText("Soltar tudo");
        soltar.setOnClickListener(v -> soltar());
        root.addView(soltar);

        achar();
    }

    void achar() {
        mgrs.clear();
        lights.clear();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || (d.getSources() & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD) continue;
            LightsManager m = d.getLightsManager();
            if (m == null) continue;
            for (Light l : m.getLights()) {
                if (l.hasRgbControl()) {
                    mgrs.add(m);
                    lights.add(l);
                    break;
                }
            }
        }
        status.setText("Controles com RGB conectados: " + lights.size()
                + "\nCampos vazios são ignorados (o controle não muda).");
    }

    int lerCampo(EditText e) {
        String t = e.getText().toString().trim();
        if (t.isEmpty()) return 0;
        return Math.min(255, Integer.parseInt(t));
    }

    void aplicar() {
        achar();
        fecharSessoes();
        int feitos = 0;
        for (int i = 0; i < N && i < lights.size(); i++) {
            boolean vazio = campos[i][0].length() == 0 && campos[i][1].length() == 0 && campos[i][2].length() == 0;
            if (vazio) continue;
            int r = lerCampo(campos[i][0]);
            int g = lerCampo(campos[i][1]);
            int b = lerCampo(campos[i][2]);
            try {
                LightsManager.LightsSession s = mgrs.get(i).openSession();
                LightState st = new LightState.Builder().setColor(Color.rgb(r, g, b)).build();
                s.requestLights(new LightsRequest.Builder().addLight(lights.get(i), st).build());
                sessions.add(s);
                feitos++;
            } catch (Exception ex) {
                status.setText("Erro: " + ex);
                return;
            }
        }
        if (feitos > 0) startForegroundService(new Intent(this, KeepAlive.class));
        status.setText("Cores aplicadas em " + feitos + " de " + lights.size() + " controle(s)");
    }

    void soltar() {
        fecharSessoes();
        stopService(new Intent(this, KeepAlive.class));
        status.setText("Luz solta (volta ao padrão)");
    }

    void fecharSessoes() {
        for (LightsManager.LightsSession s : sessions) {
            try { s.close(); } catch (Exception ignored) {}
        }
        sessions.clear();
    }
}
