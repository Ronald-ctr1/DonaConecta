package com.donaconecta.app.publicacion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.donaconecta.app.R;
import com.donaconecta.app.auth.ActividadInicioSesion;
import com.donaconecta.app.auth.RolUsuario;
import com.donaconecta.app.conexion.SesionLocal;

/** Panel inicial (base para el Donor Dashboard del diseño). */
public class ActividadPanelPrincipal extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        setContentView(R.layout.actividad_panel_principal);
        SesionLocal sesion = SesionLocal.de(this);
        RolUsuario rol = RolUsuario.desdeValor(sesion.obtenerRol());

        ((TextView) findViewById(R.id.textoSaludo)).setText("Hola, " + sesion.obtenerNombre());
        ((TextView) findViewById(R.id.textoRol)).setText(rol.getEtiqueta());

        View botonPublicar = findViewById(R.id.botonPublicarDonacion);
        botonPublicar.setVisibility(rol == RolUsuario.FUNDACION ? View.GONE : View.VISIBLE);
        botonPublicar.setOnClickListener(v -> startActivity(new Intent(this, ActividadPublicarDonacion.class)));

        findViewById(R.id.botonCerrarSesion).setOnClickListener(v -> {
            sesion.cerrar();
            Intent intent = new Intent(this, ActividadInicioSesion.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }
}
