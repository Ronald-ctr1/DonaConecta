package com.donaconecta.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.donaconecta.app.R;
import com.donaconecta.app.conexion.EjecutorTareas;
import com.donaconecta.app.modelos.Perfil;
import com.donaconecta.app.publicacion.ActividadPanelPrincipal;
import com.google.android.material.textfield.TextInputEditText;

public class ActividadRegistro extends AppCompatActivity {
    private static final int LARGO_MINIMO_CLAVE = 6;

    private TextInputEditText campoNombre, campoCorreo, campoClave, campoConfirmarClave;
    private RadioGroup grupoRol;
    private CheckBox casillaTerminos;
    private Button botonCrearCuenta;
    private ProgressBar indicadorProgreso;
    private RepositorioAutenticacion repositorio;

    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        setContentView(R.layout.actividad_registro);
        repositorio = new RepositorioAutenticacion(this);
        campoNombre = findViewById(R.id.campoNombre);
        campoCorreo = findViewById(R.id.campoCorreo);
        campoClave = findViewById(R.id.campoClave);
        campoConfirmarClave = findViewById(R.id.campoConfirmarClave);
        grupoRol = findViewById(R.id.grupoRol);
        casillaTerminos = findViewById(R.id.casillaTerminos);
        botonCrearCuenta = findViewById(R.id.botonCrearCuenta);
        indicadorProgreso = findViewById(R.id.indicadorProgreso);
        botonCrearCuenta.setOnClickListener(v -> intentarRegistrar());
    }

    private void intentarRegistrar() {
        String nombre = textoDe(campoNombre);
        String correo = textoDe(campoCorreo);
        String clave = textoDe(campoClave);
        String error = validar(nombre, correo, clave, textoDe(campoConfirmarClave));
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }
        RolUsuario rol = rolSeleccionado();
        mostrarCargando(true);
        EjecutorTareas.ejecutar(() -> repositorio.registrar(nombre, correo, clave, rol),
                new EjecutorTareas.Respuesta<Perfil>() {
                    @Override public void alExito(Perfil perfil) {
                        if (perfil == null) { // Supabase exige confirmación por correo
                            mostrarCargando(false);
                            Toast.makeText(ActividadRegistro.this,
                                    "Revisa tu correo para confirmar la cuenta", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        Intent panel = new Intent(ActividadRegistro.this, ActividadPanelPrincipal.class);
                        panel.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(panel);
                    }
                    @Override public void alFallar(String mensaje) {
                        mostrarCargando(false);
                        Toast.makeText(ActividadRegistro.this, mensaje, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String validar(String nombre, String correo, String clave, String confirmacion) {
        if (nombre.isEmpty()) return "Ingresa tu nombre";
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) return "Correo no válido";
        if (clave.length() < LARGO_MINIMO_CLAVE) return "La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres";
        if (!clave.equals(confirmacion)) return "Las contraseñas no coinciden";
        if (!casillaTerminos.isChecked()) return "Debes aceptar la política de privacidad";
        return null;
    }

    private RolUsuario rolSeleccionado() {
        int id = grupoRol.getCheckedRadioButtonId();
        if (id == R.id.rolEmpresa) return RolUsuario.EMPRESA;
        if (id == R.id.rolFundacion) return RolUsuario.FUNDACION;
        return RolUsuario.DONANTE;
    }

    private void mostrarCargando(boolean cargando) {
        indicadorProgreso.setVisibility(cargando ? View.VISIBLE : View.GONE);
        botonCrearCuenta.setEnabled(!cargando);
    }

    private String textoDe(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
