package com.donaconecta.app.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REGLA DE NEGOCIO: una donación solo avanza si TODAS las fotos obligatorias existen
 * y cada foto tiene iluminación aceptable. Clase Java pura (sin Android) => probada con JUnit en CI.
 */
public class ValidadorEscaneo {
    public static final double BRILLO_MINIMO = 70.0;        // por debajo: baja iluminación
    public static final double BRILLO_MAXIMO = 235.0;       // por encima: sobreexposición
    public static final double BRILLO_OPTIMO_DESDE = 100.0;
    public static final double BRILLO_OPTIMO_HASTA = 200.0;
    private static final double PENALIZACION_POR_ANGULO_FALTANTE = 2.0;

    /**
     * @param brilloPorAngulo brillo medio (0-255) por ángulo. Ángulo ausente = no se tomó la foto.
     *                        Valor negativo = la imagen no pudo leerse.
     */
    public ResultadoAnalisis validar(Map<TipoAngulo, Double> brilloPorAngulo) {
        List<String> motivos = new ArrayList<>();
        double sumaPuntajes = 0;
        int fotosEvaluadas = 0;
        int angulosFaltantes = 0;

        for (TipoAngulo angulo : TipoAngulo.values()) {
            Double brillo = brilloPorAngulo.get(angulo);
            if (brillo == null) {
                if (angulo.esObligatorio()) {
                    angulosFaltantes++;
                    motivos.add("Falta la fotografía: " + angulo.getEtiqueta());
                }
                continue;
            }
            if (brillo < 0) {
                motivos.add("No se pudo leer la fotografía: " + angulo.getEtiqueta());
                continue;
            }
            fotosEvaluadas++;
            sumaPuntajes += puntajeDeBrillo(brillo);
            if (brillo < BRILLO_MINIMO) {
                motivos.add("Baja iluminación en la foto: " + angulo.getEtiqueta());
            } else if (brillo > BRILLO_MAXIMO) {
                motivos.add("Sobreexposición (demasiada luz) en la foto: " + angulo.getEtiqueta());
            }
        }

        double calidad = fotosEvaluadas == 0 ? 0 : sumaPuntajes / fotosEvaluadas;
        calidad = limitar(calidad - PENALIZACION_POR_ANGULO_FALTANTE * angulosFaltantes, 0, 10);
        calidad = Math.round(calidad * 10.0) / 10.0;

        return new ResultadoAnalisis(motivos.isEmpty(), calidad, motivos, brilloPorAngulo);
    }

    /** 10 dentro del rango óptimo; decrece linealmente hacia 0 en los extremos (negro / blanco total). */
    static double puntajeDeBrillo(double brillo) {
        if (brillo >= BRILLO_OPTIMO_DESDE && brillo <= BRILLO_OPTIMO_HASTA) return 10.0;
        if (brillo < BRILLO_OPTIMO_DESDE) return limitar(10.0 * brillo / BRILLO_OPTIMO_DESDE, 0, 10);
        return limitar(10.0 * (255.0 - brillo) / (255.0 - BRILLO_OPTIMO_HASTA), 0, 10);
    }

    private static double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }
}
