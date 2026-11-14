package com.donaconecta.app.ia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Procesa las fotos: calcula el brillo medio de cada una y delega la decisión en {@link ValidadorEscaneo}.
 * Hoy es un análisis heurístico local; para IA real, reemplazar calcularBrillo por un modelo
 * (TensorFlow Lite / ML Kit / Edge Function) manteniendo la misma firma.
 */
public class ServicioAnalisisIA {
    private static final int FACTOR_REDUCCION = 8; // reduce la imagen 8x para analizar rápido
    private final ValidadorEscaneo validador = new ValidadorEscaneo();

    /** Síncrono: llamar desde EjecutorTareas. */
    public ResultadoAnalisis analizar(Context contexto, Map<TipoAngulo, Uri> fotos) {
        Map<TipoAngulo, Double> brillos = new EnumMap<>(TipoAngulo.class);
        for (Map.Entry<TipoAngulo, Uri> foto : fotos.entrySet()) {
            brillos.put(foto.getKey(), calcularBrillo(contexto, foto.getValue()));
        }
        return validador.validar(brillos);
    }

    /** Luminancia media (0-255) con la fórmula Rec. 601. Devuelve -1 si no se puede leer la imagen. */
    double calcularBrillo(Context contexto, Uri uri) {
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = FACTOR_REDUCCION;
        try (InputStream entrada = contexto.getContentResolver().openInputStream(uri)) {
            Bitmap mapa = BitmapFactory.decodeStream(entrada, null, opciones);
            if (mapa == null) return -1;
            int ancho = mapa.getWidth();
            int alto = mapa.getHeight();
            int[] pixeles = new int[ancho * alto];
            mapa.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto);
            mapa.recycle();
            double suma = 0;
            for (int p : pixeles) {
                int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
                suma += 0.299 * r + 0.587 * g + 0.114 * b;
            }
            return suma / pixeles.length;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }
}
