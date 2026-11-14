package com.donaconecta.app.conexion;

import android.content.Context;
import android.content.SharedPreferences;

import com.donaconecta.app.modelos.Perfil;

/** Guarda la sesión de Supabase en el dispositivo. Para producción, migrar a EncryptedSharedPreferences. */
public final class SesionLocal {
    private static final String ARCHIVO = "sesion_donaconecta";
    private final SharedPreferences preferencias;

    private SesionLocal(Context contexto) {
        preferencias = contexto.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public static SesionLocal de(Context contexto) { return new SesionLocal(contexto); }

    public void guardarTokens(String tokenAcceso, String tokenRefresco, String usuarioId) {
        preferencias.edit()
                .putString("token_acceso", tokenAcceso)
                .putString("token_refresco", tokenRefresco)
                .putString("usuario_id", usuarioId)
                .apply();
    }

    public void guardarPerfil(Perfil perfil) {
        preferencias.edit()
                .putString("nombre", perfil.nombreCompleto)
                .putString("rol", perfil.rol)
                .apply();
    }

    public boolean haySesion() { return obtenerTokenAcceso() != null; }
    public String obtenerTokenAcceso() { return preferencias.getString("token_acceso", null); }
    public String obtenerUsuarioId() { return preferencias.getString("usuario_id", null); }
    public String obtenerNombre() { return preferencias.getString("nombre", ""); }
    public String obtenerRol() { return preferencias.getString("rol", "donante"); }
    public void cerrar() { preferencias.edit().clear().apply(); }
}
