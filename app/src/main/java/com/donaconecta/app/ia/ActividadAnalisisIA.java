package com.donaconecta.app.ia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donaconecta.app.R;
import com.donaconecta.app.conexion.EjecutorTareas;
import com.donaconecta.app.modelos.Donacion;
import com.donaconecta.app.publicacion.ActividadFotosDonacion;
import com.donaconecta.app.publicacion.ActividadPanelPrincipal;
import com.donaconecta.app.publicacion.RepositorioDonaciones;

import java.util.EnumMap;
import java.util.Map;

public class ActividadAnalisisIA extends AppCompatActivity {
    private Donacion donacion;
    private final Map<TipoAngulo, Uri> fotos = new EnumMap<>(TipoAngulo.class);
    private ResultadoAnalisis resultado;

    private ProgressBar indicadorProgreso;
    private TextView textoCalidad, textoMotivos;
    private View tarjetaRechazo;
    private Button botonRetomar, botonContinuar;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        setContentView(R.layout.actividad_analisis_ia);
        indicadorProgreso = findViewById(R.id.indicadorProgreso);
        textoCalidad = findViewById(R.id.textoCalidad);
        textoMotivos = findViewById(R.id.textoMotivos);
        tarjetaRechazo = findViewById(R.id.tarjetaRechazo);
        botonRetomar = findViewById(R.id.botonRetomar);
        botonContinuar = findViewById(R.id.botonContinuar);   // nace deshabilitado en el layout

        donacion = (Donacion) getIntent().getSerializableExtra(ActividadFotosDonacion.EXTRA_DONACION);
        for (TipoAngulo angulo : TipoAngulo.values()) {
            String uri = getIntent().getStringExtra(angulo.getClave());
            if (uri != null) fotos.put(angulo, Uri.parse(uri));
        }

        botonRetomar.setOnClickListener(v -> volverATomarFotos());
        botonContinuar.setOnClickListener(v -> publicar());
        analizar();
    }

    private void analizar() {
        EjecutorTareas.ejecutar(() -> new ServicioAnalisisIA().analizar(this, fotos),
                new EjecutorTareas.Respuesta<ResultadoAnalisis>() {
                    @Override public void alExito(ResultadoAnalisis r) { mostrarResultado(r); }
                    @Override public void alFallar(String mensaje) {
                        indicadorProgreso.setVisibility(View.GONE);
                        Toast.makeText(ActividadAnalisisIA.this, mensaje, Toast.LENGTH_LONG).show();
                        volverATomarFotos();
                    }
                });
    }

    private void mostrarResultado(ResultadoAnalisis r) {
        resultado = r;
        indicadorProgreso.setVisibility(View.GONE);
        textoCalidad.setVisibility(View.VISIBLE);
        textoCalidad.setText(String.format("Calidad de foto: %.1f/10", r.getCalidadFoto()));

        boolean aprobado = r.isAprobado();
        botonContinuar.setEnabled(aprobado);                        // REGLA: rechazo => "Continuar" bloqueado
        tarjetaRechazo.setVisibility(aprobado ? View.GONE : View.VISIBLE);
        botonRetomar.setVisibility(aprobado ? View.GONE : View.VISIBLE);
        if (!aprobado) textoMotivos.setText("• " + String.join("\n• ", r.getMotivosRechazo()));
    }

    private void volverATomarFotos() {
        Intent intent = new Intent(this, ActividadFotosDonacion.class);
        intent.putExtra(ActividadFotosDonacion.EXTRA_DONACION, donacion);
        startActivity(intent);
        finish();
    }

    private void publicar() {
        if (resultado == null || !resultado.isAprobado()) return;   // defensa adicional
        botonContinuar.setEnabled(false);
        EjecutorTareas.ejecutar(() -> new RepositorioDonaciones(this).publicar(donacion, fotos, resultado),
                new EjecutorTareas.Respuesta<Donacion>() {
                    @Override public void alExito(Donacion publicada) {
                        Toast.makeText(ActividadAnalisisIA.this, "¡Donación publicada!", Toast.LENGTH_LONG).show();
                        Intent panel = new Intent(ActividadAnalisisIA.this, ActividadPanelPrincipal.class);
                        panel.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(panel);
                        finish();
                    }
                    @Override public void alFallar(String mensaje) {
                        botonContinuar.setEnabled(true);
                        Toast.makeText(ActividadAnalisisIA.this, mensaje, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
