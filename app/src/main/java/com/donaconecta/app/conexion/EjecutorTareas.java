package com.donaconecta.app.conexion;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Ejecuta trabajo de red/CPU fuera del hilo principal y devuelve el resultado en el hilo de UI. */
public final class EjecutorTareas {

    public interface Tarea<T> { T ejecutar() throws Exception; }

    public interface Respuesta<T> {
        void alExito(T resultado);
        void alFallar(String mensaje);
    }

    private static final ExecutorService HILOS = Executors.newFixedThreadPool(3);
    private static final Handler HILO_PRINCIPAL = new Handler(Looper.getMainLooper());

    private EjecutorTareas() { }

    public static <T> void ejecutar(Tarea<T> tarea, Respuesta<T> respuesta) {
        HILOS.execute(() -> {
            try {
                T resultado = tarea.ejecutar();
                HILO_PRINCIPAL.post(() -> respuesta.alExito(resultado));
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
                HILO_PRINCIPAL.post(() -> respuesta.alFallar(mensaje));
            }
        });
    }
}
