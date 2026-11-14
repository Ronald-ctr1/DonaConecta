package com.donaconecta.app.publicacion;

import android.content.Context;
import android.net.Uri;

import com.donaconecta.app.conexion.ClienteSupabase;
import com.donaconecta.app.conexion.ConfiguracionSupabase;
import com.donaconecta.app.conexion.ExcepcionSupabase;
import com.donaconecta.app.conexion.SesionLocal;
import com.donaconecta.app.ia.ResultadoAnalisis;
import com.donaconecta.app.ia.TipoAngulo;
import com.donaconecta.app.modelos.Donacion;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Publica una donación: 1) inserta la donación (queda en 'borrador'), 2) sube las fotos,
 * 3) registra el análisis. Un trigger en la base pasa la donación a 'publicada' solo si el análisis aprobó.
 */
public class RepositorioDonaciones {
    private final Context contexto;
    private final ClienteSupabase cliente;
    private final SesionLocal sesion;

    public RepositorioDonaciones(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.cliente = ClienteSupabase.obtener(contexto);
        this.sesion = SesionLocal.de(contexto);
    }

    public Donacion publicar(Donacion donacion, Map<TipoAngulo, Uri> fotos, ResultadoAnalisis analisis)
            throws ExcepcionSupabase {
        if (!analisis.isAprobado()) throw new ExcepcionSupabase("El análisis de fotos no fue aprobado", 0);

        String donanteId = sesion.obtenerUsuarioId();
        donacion.donanteId = donanteId;

        // 1) Donación
        JsonArray creada = cliente.solicitar("POST", "/rest/v1/donaciones", donacion,
                Collections.singletonMap("Prefer", "return=representation")).getAsJsonArray();
        String donacionId = creada.get(0).getAsJsonObject().get("id").getAsString();
        donacion.id = donacionId;

        // 2) Fotos (Storage) + filas en fotos_donacion
        JsonArray filasFotos = new JsonArray();
        for (Map.Entry<TipoAngulo, Uri> foto : fotos.entrySet()) {
            String ruta = donanteId + "/" + donacionId + "/" + foto.getKey().getClave() + ".jpg";
            cliente.subirArchivo(ConfiguracionSupabase.BUCKET_FOTOS, ruta, leerBytes(foto.getValue()), "image/jpeg");

            JsonObject fila = new JsonObject();
            fila.addProperty("donacion_id", donacionId);
            fila.addProperty("angulo", foto.getKey().getClave());
            fila.addProperty("url", cliente.urlPublica(ConfiguracionSupabase.BUCKET_FOTOS, ruta));
            Double brillo = analisis.getBrilloPorAngulo().get(foto.getKey());
            if (brillo != null) fila.addProperty("brillo", brillo);
            filasFotos.add(fila);
        }
        cliente.solicitar("POST", "/rest/v1/fotos_donacion", filasFotos, null);

        // 3) Análisis IA (el trigger publica la donación si aprobado = true)
        JsonObject filaAnalisis = new JsonObject();
        filaAnalisis.addProperty("donacion_id", donacionId);
        filaAnalisis.addProperty("calidad_foto", analisis.getCalidadFoto());
        filaAnalisis.addProperty("aprobado", analisis.isAprobado());
        filaAnalisis.add("motivos", cliente.gson().toJsonTree(analisis.getMotivosRechazo()));
        cliente.solicitar("POST", "/rest/v1/analisis_ia", filaAnalisis, null);

        return donacion;
    }

    private byte[] leerBytes(Uri uri) throws ExcepcionSupabase {
        try (InputStream entrada = contexto.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            byte[] bufer = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(bufer)) != -1) salida.write(bufer, 0, leidos);
            return salida.toByteArray();
        } catch (IOException | NullPointerException e) {
            throw new ExcepcionSupabase("No se pudo leer una fotografía", 0, e);
        }
    }
}
