package com.donaconecta.app.auth;

import android.content.Context;

import com.donaconecta.app.conexion.ClienteSupabase;
import com.donaconecta.app.conexion.ExcepcionSupabase;
import com.donaconecta.app.conexion.SesionLocal;
import com.donaconecta.app.modelos.Perfil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Acceso a Supabase Auth. Métodos síncronos: invocar desde EjecutorTareas. */
public class RepositorioAutenticacion {
    private final ClienteSupabase cliente;
    private final SesionLocal sesion;

    public RepositorioAutenticacion(Context contexto) {
        this.cliente = ClienteSupabase.obtener(contexto);
        this.sesion = SesionLocal.de(contexto);
    }

    public Perfil iniciarSesion(String correo, String clave) throws ExcepcionSupabase {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("email", correo);
        cuerpo.addProperty("password", clave);
        JsonObject respuesta = cliente.solicitar("POST", "/auth/v1/token?grant_type=password", cuerpo, null).getAsJsonObject();
        return completarSesion(respuesta);
    }

    /**
     * Registra al usuario. El rol y el nombre viajan en los metadatos y un trigger de base de datos
     * crea la fila en public.perfiles.
     * @return el perfil si la sesión quedó iniciada; null si Supabase exige confirmar el correo.
     */
    public Perfil registrar(String nombre, String correo, String clave, RolUsuario rol) throws ExcepcionSupabase {
        JsonObject metadatos = new JsonObject();
        metadatos.addProperty("nombre_completo", nombre);
        metadatos.addProperty("rol", rol.getValorBaseDatos());
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("email", correo);
        cuerpo.addProperty("password", clave);
        cuerpo.add("data", metadatos);

        JsonObject respuesta = cliente.solicitar("POST", "/auth/v1/signup", cuerpo, null).getAsJsonObject();
        if (!respuesta.has("access_token")) return null;
        return completarSesion(respuesta);
    }

    public void cerrarSesion() { sesion.cerrar(); }

    private Perfil completarSesion(JsonObject respuesta) throws ExcepcionSupabase {
        String usuarioId = respuesta.getAsJsonObject("user").get("id").getAsString();
        String refresco = respuesta.has("refresh_token") ? respuesta.get("refresh_token").getAsString() : null;
        sesion.guardarTokens(respuesta.get("access_token").getAsString(), refresco, usuarioId);
        Perfil perfil = obtenerPerfil(usuarioId);
        sesion.guardarPerfil(perfil);
        return perfil;
    }

    private Perfil obtenerPerfil(String usuarioId) throws ExcepcionSupabase {
        JsonElement r = cliente.solicitar("GET", "/rest/v1/perfiles?id=eq." + usuarioId + "&select=*", null, null);
        JsonArray filas = r.getAsJsonArray();
        if (filas.size() == 0) throw new ExcepcionSupabase("No se encontró el perfil del usuario", 404);
        return cliente.gson().fromJson(filas.get(0), Perfil.class);
    }
}
