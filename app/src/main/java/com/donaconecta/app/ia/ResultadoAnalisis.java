package com.donaconecta.app.ia;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resultado inmutable del análisis de las fotografías. */
public final class ResultadoAnalisis {
    private final boolean aprobado;
    private final double calidadFoto;                     // 0.0 a 10.0
    private final List<String> motivosRechazo;
    private final Map<TipoAngulo, Double> brilloPorAngulo; // 0 a 255

    public ResultadoAnalisis(boolean aprobado, double calidadFoto, List<String> motivosRechazo,
                             Map<TipoAngulo, Double> brilloPorAngulo) {
        this.aprobado = aprobado;
        this.calidadFoto = calidadFoto;
        this.motivosRechazo = Collections.unmodifiableList(motivosRechazo);
        Map<TipoAngulo, Double> copia = new EnumMap<>(TipoAngulo.class);
        copia.putAll(brilloPorAngulo);
        this.brilloPorAngulo = Collections.unmodifiableMap(copia);
    }

    public boolean isAprobado() { return aprobado; }
    public double getCalidadFoto() { return calidadFoto; }
    public List<String> getMotivosRechazo() { return motivosRechazo; }
    public Map<TipoAngulo, Double> getBrilloPorAngulo() { return brilloPorAngulo; }
}
