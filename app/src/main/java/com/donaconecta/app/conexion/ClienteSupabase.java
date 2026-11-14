package com.donaconecta.app.conexion;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Cliente HTTP mínimo para Supabase (Auth/GoTrue, PostgREST y Storage).
 * Todos los métodos son SÍNCRONOS: llamarlos desde {@link EjecutorTareas}, nunca desde el hilo de UI.
 */
public final class ClienteSupabase {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson GSON = new Gson();
    private static ClienteSupabase instancia;

    private final OkHttpClient http;
    private final SesionLocal sesion;

    private ClienteSupabase(Context contexto) {
        this.sesion = SesionLocal.de(contexto);
        this.http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ClienteSupabase obtener(Context contexto) {
        if (instancia == null) instancia = new ClienteSupabase(contexto.getApplicationContext());
        return instancia;
    }

    public Gson gson() { return GSON; }

    /**
     * @param metodo   GET, POST, PATCH, DELETE
     * @param ruta     ej. "/rest/v1/donaciones?select=*" o "/auth/v1/signup"
     * @param cuerpo   objeto serializable a JSON (o null)
     * @param cabeceras cabeceras extra (ej. Prefer: return=representation) o null
     */
    public JsonElement solicitar(String metodo, String ruta, Object cuerpo,
                                 Map<String, String> cabeceras) throws ExcepcionSupabase {
        RequestBody cuerpoHttp = null;
        if (cuerpo != null) cuerpoHttp = RequestBody.create(GSON.toJson(cuerpo), JSON);
        else if (!metodo.equals("GET") && !metodo.equals("DELETE")) cuerpoHttp = RequestBody.create("", JSON);

        Request.Builder peticion = nuevaPeticion(ruta, ruta.startsWith("/auth/v1/"))
                .method(metodo, cuerpoHttp)
                .header("Content-Type", "application/json");
        if (cabeceras != null) for (Map.Entry<String, String> e : cabeceras.entrySet()) peticion.header(e.getKey(), e.getValue());
        return ejecutar(peticion.build());
    }

    /** Sube un archivo binario a Supabase Storage con la sesión del usuario. */
    public void subirArchivo(String bucket, String rutaObjeto, byte[] datos, String tipoMime) throws ExcepcionSupabase {
        RequestBody cuerpo = RequestBody.create(datos, MediaType.get(tipoMime));
        Request peticion = nuevaPeticion("/storage/v1/object/" + bucket + "/" + rutaObjeto, false)
                .post(cuerpo).header("x-upsert", "true").build();
        ejecutar(peticion);
    }

    public String urlPublica(String bucket, String rutaObjeto) {
        return ConfiguracionSupabase.URL_BASE + "/storage/v1/object/public/" + bucket + "/" + rutaObjeto;
    }

    // ---------- internos ----------

    private Request.Builder nuevaPeticion(String ruta, boolean usarClaveAnonima) {
        String token = (!usarClaveAnonima && sesion.haySesion())
                ? sesion.obtenerTokenAcceso() : ConfiguracionSupabase.CLAVE_ANONIMA;
        return new Request.Builder()
                .url(ConfiguracionSupabase.URL_BASE + ruta)
                .header("apikey", ConfiguracionSupabase.CLAVE_ANONIMA)
                .header("Authorization", "Bearer " + token);
    }

    private JsonElement ejecutar(Request peticion) throws ExcepcionSupabase {
        try (Response respuesta = http.newCall(peticion).execute()) {
            String texto = respuesta.body() != null ? respuesta.body().string() : "";
            if (!respuesta.isSuccessful()) throw new ExcepcionSupabase(extraerMensaje(texto, respuesta.code()), respuesta.code());
            return texto.isEmpty() ? JsonNull.INSTANCE : JsonParser.parseString(texto);
        } catch (IOException e) {
            throw new ExcepcionSupabase("No hay conexión con el servidor", 0, e);
        } catch (RuntimeException e) {
            throw new ExcepcionSupabase("Respuesta inesperada del servidor", 0, e);
        }
    }

    private String extraerMensaje(String texto, int codigo) {
        try {
            JsonObject json = JsonParser.parseString(texto).getAsJsonObject();
            for (String clave : new String[]{"error_description", "msg", "message", "error"}) {
                if (json.has(clave) && !json.get(clave).isJsonNull()) return json.get(clave).getAsString();
            }
        } catch (RuntimeException ignorada) { /* cuerpo no es JSON */ }
        return "Error del servidor (" + codigo + ")";
    }
}
