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
import android.view.View;
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
    static final List<String> nomesReais = new ArrayList<>();
    static final LightsManager.LightsSession[] sessoes = new LightsManager.LightsSession[N];
    static boolean preto = true;

    final float[] hue = new float[N];
    final int[] brilho = new int[N];
    final int[] vel = new int[N];
    final boolean[] rgbAtivo = new boolean[N];
    final EditText[][] campos = new EditText[N][3];
    final SeekBar[] barrasMatiz = new SeekBar[N];
    final TextView[] titulos = new TextView[N];
    TextView status;
    boolean rodando = false;
    final Handler handler = new Handler(Looper.getMainLooper());

    final Runnable ciclo = new Runnable() {
        public void run() {
            boolean algum = false;
            for (int i = 0; i < N; i++) {
                if (!rgbAtivo[i]) continue;
                algum = true;
                hue[i] = (hue[i] + vel[i]) % 360f;
                enviar(i, corDe(i));
                if (barrasMatiz[i] != null) barrasMatiz[i].setProgress((int) hue[i]);
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
        for (int i = 0; i < N; i++) {
            brilho[i] = 255;
            vel[i] = 2;
            hue[i] = 0f;
        }
        setTitle("");
        achar();
        construir();
    }

    void construir() {
        int fundo = preto ? Color.BLACK : Color.WHITE;
        int texto = preto ? Color.WHITE : Color.BLACK;
        int secundario = preto ? Color.rgb(160, 160, 165) : Color.rgb(100, 100, 105);
        int divisor = preto ? Color.rgb(50, 50, 55) : Color.rgb(215, 215, 220);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(fundo);
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(56, 56, 56, 56);
        sv.addView(raiz);
        setContentView(sv);

        status = new TextView(this);
        status.setTextSize(14);
        status.setTextColor(secundario);
        status.setPadding(0, 0, 0, 16);
        raiz.addView(status);

        for (int i = 0; i < N; i++) {
            final int idx = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(0, 36, 0, 36);

            // título: "Controle N" + nome real do dispositivo
            TextView t = new TextView(this);
            t.setTextSize(18);
            t.setTextColor(texto);
            t.setText(tituloDe(i));
            titulos[i] = t;
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

            // brilho
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
            card.addView(sb);

            // velocidade do RGB
            TextView lv = new TextView(this);
            lv.setText("Velocidade RGB");
            lv.setTextColor(secundario);
            lv.setTextSize(12);
            card.addView(lv);

            SeekBar sv2 = new SeekBar(this);
            sv2.setMax(19);
            sv2.setProgress(vel[i] - 1);
            sv2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    if (u) vel[idx] = p + 1;
                }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            card.addView(sv2);

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
            linha.addView(sw);

            card.addView(linha);
            raiz.addView(card);

            if (i < N - 1) {
                View linhaDiv = new View(this);
                linhaDiv.setBackgroundColor(divisor);
                raiz.addView(linhaDiv, new LinearLayout.LayoutParams(-1, 2));
            }

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

    // "Controle 1 · Wireless Controller" (ou só "Controle 1" se não houver controle)
    String tituloDe(int i) {
        String nome = i < nomesReais.size() ? nomesReais.get(i) : "sem controle";
        return "Controle " + (i + 1) + "  ·  " + nome;
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
        return Color.HSVToColor(new float[]{hue[i], 1f, brilho[i] / 255f});
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
        nomesReais.clear();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || (d.getSources() & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD) continue;
            LightsManager m = d.getLightsManager();
            if (m == null) continue;
            for (Light l : m.getLights()) {
                if (l.hasRgbControl()) {
                    mgrs.add(m);
                    lights.add(l);
                    nomesReais.add(d.getName());
                    break;
                }
            }
        }
        if (status != null) status.setText("Controles conectados: " + lights.size());
        for (int i = 0; i < N; i++) {
            if (titulos[i] != null) titulos[i].setText(tituloDe(i));
        }
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
        menu.add(0, 1, 0, preto ? "Tema branco" : "Tema preto");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            preto = !preto;
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
