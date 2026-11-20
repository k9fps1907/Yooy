package com.ds4.light;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    static final int N = 4;
    static final List<LightsManager> mgrs = new ArrayList<>();
    static final List<Light> lights = new ArrayList<>();
    static final List<LightsManager.LightsSession> sessions = new ArrayList<>();
    static boolean escuro = false;

    final int[] matiz = new int[N];
    final int[] brilho = new int[N];
    final int[] sat = new int[N];
    final EditText[][] campos = new EditText[N][3];
    final TextView[] titulos = new TextView[N];
    final SeekBar[] barrasMatiz = new SeekBar[N];
    final SeekBar[] barrasBrilho = new SeekBar[N];
    LinearLayout raiz;
    TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        for (int i = 0; i < N; i++) { brilho[i] = 255; sat[i] = 255; matiz[i] = 0; }
        setTitle("");
        construir();
        achar();
    }

    void construir() {
        boolean dark = escuro;
        int fundo = dark ? Color.rgb(18, 18, 20) : Color.rgb(248, 248, 250);
        int texto = dark ? Color.WHITE : Color.rgb(30, 30, 30);
        int secundario = dark ? Color.rgb(150, 150, 155) : Color.rgb(110, 110, 120);
        int cartao = dark ? Color.rgb(30, 30, 34) : Color.WHITE;

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(fundo);
        raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(48, 48, 48, 48);
        sv.addView(raiz);
        setContentView(sv);

        status = new TextView(this);
        status.setTextSize(14);
        status.setTextColor(secundario);
        status.setPadding(0, 0, 0, 24);
        raiz.addView(status);

        for (int i = 0; i < N; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(40, 32, 40, 32);
            card.setBackgroundColor(cartao);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, 28);
            card.setLayoutParams(lp);

            TextView t = new TextView(this);
            t.setText("Controle " + (i + 1));
            t.setTextSize(16);
            t.setTextColor(texto);
            titulos[i] = t;
            card.addView(t);

            // barra de matiz (arco-íris horizontal)
            SeekBar sm = new SeekBar(this);
            sm.setMax(360);
            sm.setProgress(0);
            sm.setProgressDrawable(gradienteMatiz());
            sm.setPadding(0, 24, 0, 24);
            final int idx = i;
            sm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean u) { if (u) { matiz[idx] = p; atualizar(idx); } }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            barrasMatiz[i] = sm;
            card.addView(sm);

            // barra de brilho (0 = apagado)
            SeekBar sb = new SeekBar(this);
            sb.setMax(255);
            sb.setProgress(255);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean u) { if (u) { brilho[idx] = p; atualizar(idx); } }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            barrasBrilho[i] = sb;
            TextView lb = new TextView(this);
            lb.setText("Brilho");
            lb.setTextColor(secundario);
            lb.setTextSize(12);
            card.addView(lb);
            card.addView(sb);

            // campos R G B
            LinearLayout linha = new LinearLayout(this);
            String[] rot = {"R", "G", "B"};
            for (int j = 0; j < 3; j++) {
                EditText e = new EditText(this);
                e.setHint(rot[j]);
                e.setTextColor(texto);
                e.setHintTextColor(secundario);
                e.setInputType(InputType.TYPE_CLASS_NUMBER);
                e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                e.setGravity(Gravity.CENTER);
                e.setTextSize(14);
                linha.addView(e, new LinearLayout.LayoutParams(0, -2, 1));
                campos[i][j] = e;
            }
            card.addView(linha);
            raiz.addView(card);
        }

        Button aplicar = new Button(this);
        aplicar.setText("Aplicar");
        aplicar.setAllCaps(false);
        aplicar.setOnClickListener(v -> aplicarTudo());
        raiz.addView(aplicar);

        Button soltar = new Button(this);
        soltar.setText("Soltar tudo");
        soltar.setAllCaps(false);
        soltar.setOnClickListener(v -> soltar());
        raiz.addView(soltar);
    }

    // gradiente horizontal de todas as cores
    android.graphics.drawable.Drawable gradienteMatiz() {
        int[] cores = new int[7];
        for (int k = 0; k < 7; k++) cores[k] = Color.HSVToColor(new float[]{k * 60f, 1f, 1f});
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, cores);
        g.setCornerRadius(40);
        g.setSize(-1, 40);
        return g;
    }

    // converte matiz/brilho/saturação do índice em RGB e joga nos campos
    void atualizar(int i) {
        int cor = Color.HSVToColor(new float[]{matiz[i], sat[i] / 255f, brilho[i] / 255f});
        campos[i][0].setText(String.valueOf(Color.red(cor)));
        campos[i][1].setText(String.valueOf(Color.green(cor)));
        campos[i][2].setText(String.valueOf(Color.blue(cor)));
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
        status.setText("Controles conectados: " + lights.size());
    }

    int lerCampo(EditText e) {
        String t = e.getText().toString().trim();
        if (t.isEmpty()) return 0;
        return Math.min(255, Integer.parseInt(t));
    }

    void aplicarTudo() {
        achar();
        fecharSessoes();
        int feitos = 0;
        for (int i = 0; i < N && i < lights.size(); i++) {
            int r = lerCampo(campos[i][0]);
            int g = lerCampo(campos[i][1]);
            int b = lerCampo(campos[i][2]);
            if (campos[i][0].length() == 0 && campos[i][1].length() == 0 && campos[i][2].length() == 0) continue;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, escuro ? "Tema claro" : "Tema escuro");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            escuro = !escuro;
            construir();
            achar();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
            }
