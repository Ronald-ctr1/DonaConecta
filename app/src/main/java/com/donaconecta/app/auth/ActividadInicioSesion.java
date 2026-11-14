package com.donaconecta.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donaconecta.app.R;
import com.donaconecta.app.conexion.EjecutorTareas;
import com.donaconecta.app.conexion.SesionLocal;
import com.donaconecta.app.modelos.Perfil;
import com.donaconecta.app.publicacion.ActividadPanelPrincipal;
import com.google.android.material.textfield.TextInputEditText;

public class ActividadInicioSesion extends AppCompatActivity {
    private TextInputEditText campoCorreo;
    private TextInputEditText campoClave;
    private Button botonIngresar;
    private ProgressBar indicadorProgreso;
    private RepositorioAutenticacion repositorio;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        if (SesionLocal.de(this).haySesion()) {
            irAlPanel();
            return;
        }
        setContentView(R.layout.actividad_inicio_sesion);
        repositorio = new RepositorioAutenticacion(this);
        campoCorreo = findViewById(R.id.campoCorreo);
        campoClave = findViewById(R.id.campoClave);
        botonIngresar = findViewById(R.id.botonIngresar);
        indicadorProgreso = findViewById(R.id.indicadorProgreso);

        botonIngresar.setOnClickListener(v -> intentarIngresar());
        findViewById(R.id.botonCrearCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, ActividadRegistro.class)));
    }

    private void intentarIngresar() {
        String correo = textoDe(campoCorreo);
        String clave = textoDe(campoClave);
        if (TextUtils.isEmpty(correo) || TextUtils.isEmpty(clave)) {
            Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarCargando(true);
        EjecutorTareas.ejecutar(() -> repositorio.iniciarSesion(correo, clave),
                new EjecutorTareas.Respuesta<Perfil>() {
                    @Override public void alExito(Perfil perfil) { irAlPanel(); }
                    @Override public void alFallar(String mensaje) {
                        mostrarCargando(false);
                        Toast.makeText(ActividadInicioSesion.this, mensaje, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarCargando(boolean cargando) {
        indicadorProgreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonIngresar.setEnabled(!cargando);
    }

    private void irAlPanel() {
        startActivity(new Intent(this, ActividadPanelPrincipal.class));
        finish();
    }

    private String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
