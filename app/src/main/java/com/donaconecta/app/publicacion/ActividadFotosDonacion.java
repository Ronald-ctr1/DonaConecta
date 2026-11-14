package com.donaconecta.app.publicacion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.donaconecta.app.R;
import com.donaconecta.app.ia.ActividadAnalisisIA;
import com.donaconecta.app.ia.TipoAngulo;
import com.donaconecta.app.modelos.Donacion;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Paso 2 de 3: captura de fotos por ángulo (Frontal, Trasera, Detalle y Extra opcional). */
public class ActividadFotosDonacion extends AppCompatActivity {
    public static final String EXTRA_DONACION = "donacion";

    private final Map<TipoAngulo, Uri> fotos = new EnumMap<>(TipoAngulo.class);
    private final Map<TipoAngulo, ImageView> casillas = new EnumMap<>(TipoAngulo.class);
    private TipoAngulo anguloEnCaptura;
    private Uri uriPendiente;
    private Donacion donacion;
    private ActivityResultLauncher<Uri> lanzadorCamara;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        setContentView(R.layout.actividad_fotos_donacion);
        donacion = (Donacion) getIntent().getSerializableExtra(EXTRA_DONACION);

        lanzadorCamara = registerForActivityResult(new ActivityResultContracts.TakePicture(), tomada -> {
            if (tomada && anguloEnCaptura != null) mostrarFoto(anguloEnCaptura, uriPendiente);
        });

        vincularCasilla(R.id.casillaFrontal, TipoAngulo.FRONTAL);
        vincularCasilla(R.id.casillaTrasera, TipoAngulo.TRASERA);
        vincularCasilla(R.id.casillaDetalle, TipoAngulo.DETALLE);
        vincularCasilla(R.id.casillaExtra, TipoAngulo.EXTRA);
        findViewById(R.id.botonAnalizar).setOnClickListener(v -> irAlAnalisis());
    }

    private void vincularCasilla(int idVista, TipoAngulo angulo) {
        ImageView casilla = findViewById(idVista);
        casillas.put(angulo, casilla);
        casilla.setOnClickListener(v -> tomarFoto(angulo));
    }

    private void tomarFoto(TipoAngulo angulo) {
        try {
            File carpeta = new File(getCacheDir(), "fotos");
            if (!carpeta.exists() && !carpeta.mkdirs()) throw new IOException("Sin acceso al almacenamiento");
            File archivo = File.createTempFile(angulo.getClave() + "_", ".jpg", carpeta);
            uriPendiente = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            anguloEnCaptura = angulo;
            lanzadorCamara.launch(uriPendiente);
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarFoto(TipoAngulo angulo, Uri uri) {
        fotos.put(angulo, uri);
        ImageView casilla = casillas.get(angulo);
        casilla.setPadding(0, 0, 0, 0);
        casilla.setScaleType(ImageView.ScaleType.CENTER_CROP);
        casilla.setImageURI(uri);
    }

    /** No se bloquea aquí: la IA decide y rechaza si faltan ángulos obligatorios. */
    private void irAlAnalisis() {
        Intent intent = new Intent(this, ActividadAnalisisIA.class);
        intent.putExtra(EXTRA_DONACION, donacion);
        for (Map.Entry<TipoAngulo, Uri> foto : fotos.entrySet()) {
            intent.putExtra(foto.getKey().getClave(), foto.getValue().toString());
        }
        startActivity(intent);
        finish();
    }
}
