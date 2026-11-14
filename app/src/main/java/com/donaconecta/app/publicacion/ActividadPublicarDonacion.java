package com.donaconecta.app.publicacion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donaconecta.app.R;
import com.donaconecta.app.modelos.Donacion;
import com.google.android.material.textfield.TextInputEditText;

/** Paso 1 de 3: datos del objeto. */
public class ActividadPublicarDonacion extends AppCompatActivity {
    private TextInputEditText campoTitulo, campoDescripcion, campoCantidad, campoUbicacion;
    private Spinner selectorCategoria;
    private RadioGroup grupoCondicion;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        setContentView(R.layout.actividad_publicar_donacion);
        campoTitulo = findViewById(R.id.campoTitulo);
        campoDescripcion = findViewById(R.id.campoDescripcion);
        campoCantidad = findViewById(R.id.campoCantidad);
        campoUbicacion = findViewById(R.id.campoUbicacion);
        selectorCategoria = findViewById(R.id.selectorCategoria);
        grupoCondicion = findViewById(R.id.grupoCondicion);
        findViewById(R.id.botonSiguiente).setOnClickListener(v -> irAFotos());
    }

    private void irAFotos() {
        String titulo = textoDe(campoTitulo);
        int cantidad = leerCantidad();
        if (titulo.isEmpty()) {
            Toast.makeText(this, "Ingresa un título", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cantidad <= 0) {
            Toast.makeText(this, "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }
        Donacion donacion = new Donacion();
        donacion.titulo = titulo;
        donacion.categoria = String.valueOf(selectorCategoria.getSelectedItem());
        donacion.descripcion = textoDe(campoDescripcion);
        donacion.cantidad = cantidad;
        donacion.condicion = condicionSeleccionada();
        donacion.ubicacionAproximada = textoDe(campoUbicacion);

        Intent intent = new Intent(this, ActividadFotosDonacion.class);
        intent.putExtra(ActividadFotosDonacion.EXTRA_DONACION, donacion);
        startActivity(intent);
    }

    private String condicionSeleccionada() {
        int id = grupoCondicion.getCheckedRadioButtonId();
        if (id == R.id.condicionMuyBueno) return "muy_bueno";
        if (id == R.id.condicionBueno) return "bueno";
        if (id == R.id.condicionUsado) return "usado";
        return "nuevo";
    }

    private int leerCantidad() {
        try { return Integer.parseInt(textoDe(campoCantidad)); } catch (NumberFormatException e) { return 0; }
    }

    private String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
