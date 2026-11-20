package com.ds4.light;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.lights.Light;
import android.hardware.lights.LightState;
import android.hardware.lights.LightsManager;
import android.hardware.lights.LightsRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    static final int N = 4;
    static final List<LightsManager> mgrs = new ArrayList<>();
    static final List<Light> lights = new ArrayList<>();
    static final LightsManager.LightsSession[] sessoes = new LightsManager.LightsSession[N];
    static boolean escuro = false;

    final float[] hue = new float[N];
    final int[] brilho = new int[N];
    final int[] sat = new int[N];
    final boolean[] rgbAtivo = new boolean[N];
    final EditText[][] campos = new EditText[N][3];
    final SeekBar[] barrasMatiz = new SeekBar[N];
    final SeekBar[] barrasBrilho = new SeekBar[N];
    final Switch[] switches = new Switch[N];
    TextView status;
    boolean rodando = false;
    final Handler handler = new Handler(Looper.getMainLooper());

    final Runnable ciclo = new Runnable() {
        public void run() {
            boolean algum = false;
            for (int i = 0; i < N; i++) {
                if (!rgbAtivo[i]) continue;
                algum = true;
                hue[i] = (hue[i] + 2f) % 360f;
                enviar(i, corDe(i));
                barrasMatiz[i].setProgress((int) hue[i]);
                atualizarCampos(i);
            }
            if (algum) {
                handler.postDelayed(this, 60);
            } else {
                rodando = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        for (int i = 0; i < N; i++) { brilho[i] = 255; sat[i] = 255; hue[i] = 0f; }
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
        LinearLayout raiz = new LinearLayout(this);
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
            final int idx = i;
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
            card.addView(t);

            // barra de matiz (arco-íris horizontal)
            SeekBar sm = new SeekBar(this);
            sm.setMax(360);
            sm.setProgress((int) hue[i]);
            sm.setProgressDrawable(gradienteMatiz());
            sm.setPadding(0, 24, 0, 24);
            sm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    if (u && !rgbAtivo[idx]) { hue[idx] = p; atualizarCampos(idx); }
                }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            barrasMatiz[i] = sm;
            card.addView(sm);

            // barra de brilho (0 = apagado)
            TextView lb = new TextView(this);
            lb.setText("Brilho");
            lb.setTextColor(secundario);
            lb.setTextSize(12);
            card.addView(lb);

            SeekBar sb = new SeekBar(this);
            sb.setMax(255);
            sb.setProgress(brilho[i]);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    if (u) { brilho[idx] = p; atualizarCampos(idx); }
                }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            barrasBrilho[i] = sb;
            card.addView(sb);

            // linha: 3 campos + toggle RGB
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.CENTER_VERTICAL);
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

            Switch sw = new Switch(this);
            sw.setText("RGB");
            sw.setTextColor(texto);
            sw.setChecked(rgbAtivo[i]);
            sw.setPadding(24, 0, 0, 0);
            sw.setOnCheckedChangeListener((v, on) -> {
                rgbAtivo[idx] = on;
                setCamposAtivos(idx, !on);
                if (on) {
                    if (!rodando) { rodando = true; handler.post(ciclo); }
                    ligarServico();
                } else {
                    fecharSessao(idx);
                    atualizarCampos(idx);
                }
            });
            switches[i] = sw;
            linha.addView(sw);

            card.addView(linha);
            raiz.addView(card);
            setCamposAtivos(i, !rgbAtivo[i]);
            atualizarCampos(i);
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

    void setCamposAtivos(int i, boolean ativo) {
        for (EditText e : campos[i]) e.setEnabled(ativo);
    }

    GradientDrawable gradienteMatiz() {
        int[] cores = new int[7];
        for (int k = 0; k < 7; k++) cores[k] = Color.HSVToColor(new float[]{k * 60f, 1f, 1f});
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, cores);
        g.setCornerRadius(40);
        g.setSize(-1, 40);
        return g;
    }

    int corDe(int i) {
        return Color.HSVToColor(new float[]{hue[i], sat[i] / 255f, brilho[i] / 255f});
    }

    void atualizarCampos(int i) {
        int cor = corDe(i);
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

    void enviar(int i, int cor) {
        if (i >= lights.size()) return;
        try {
            if (sessoes[i] == null) sessoes[i] = mgrs.get(i).openSession();
            LightState st = new LightState.Builder().setColor(cor).build();
            sessoes[i].requestLights(new LightsRequest.Builder().addLight(lights.get(i), st).build());
        } catch (Exception ex) {
            status.setText("Erro: " + ex);
        }
    }

    void fecharSessao(int i) {
        if (sessoes[i] != null) {
            try { sessoes[i].close(); } catch (Exception ignored) {}
            sessoes[i] = null;
        }
    }

    void ligarServico() {
        startForegroundService(new Intent(this, KeepAlive.class));
    }

    void aplicarTudo() {
        achar();
        int feitos = 0;
        for (int i = 0; i < N && i < lights.size(); i++) {
            if (rgbAtivo[i]) continue;
            if (campos[i][0].length() == 0 && campos[i][1].length() == 0 && campos[i][2].length() == 0) continue;
            int r = lerCampo(campos[i][0]);
            int g = lerCampo(campos[i][1]);
            int b = lerCampo(campos[i][2]);
            fecharSessao(i);
            enviar(i, Color.rgb(r, g, b));
            feitos++;
        }
        if (feitos > 0) ligarServico();
        status.setText("Cores aplicadas em " + feitos + " de " + lights.size() + " controle(s)");
    }

    void soltar() {
        for (int i = 0; i < N; i++) {
            rgbAtivo[i] = false;
            fecharSessao(i);
        }
        stopService(new Intent(this, KeepAlive.class));
        construir();
        achar();
        status.setText("Luz solta (volta ao padrão)");
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

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ciclo);
        super.onDestroy();
    }
    }
