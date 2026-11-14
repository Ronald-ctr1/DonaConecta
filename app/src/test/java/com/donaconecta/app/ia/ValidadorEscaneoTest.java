package com.donaconecta.app.ia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class ValidadorEscaneoTest {
    private final ValidadorEscaneo validador = new ValidadorEscaneo();

    private Map<TipoAngulo, Double> fotosCompletas(double brillo) {
        Map<TipoAngulo, Double> fotos = new EnumMap<>(TipoAngulo.class);
        for (TipoAngulo angulo : TipoAngulo.obligatorios()) fotos.put(angulo, brillo);
        return fotos;
    }

    @Test
    public void apruebaConTodosLosAngulosYBuenaLuz() {
        ResultadoAnalisis r = validador.validar(fotosCompletas(150));
        assertTrue(r.isAprobado());
        assertEquals(10.0, r.getCalidadFoto(), 0.01);
        assertTrue(r.getMotivosRechazo().isEmpty());
    }

    @Test
    public void rechazaPorBajaIluminacion() {
        Map<TipoAngulo, Double> fotos = fotosCompletas(150);
        fotos.put(TipoAngulo.TRASERA, 30.0);
        ResultadoAnalisis r = validador.validar(fotos);
        assertFalse(r.isAprobado());
        assertEquals(1, r.getMotivosRechazo().size());
    }

    @Test
    public void rechazaPorSobreexposicion() {
        assertFalse(validador.validar(fotosCompletas(250)).isAprobado());
    }

    @Test
    public void rechazaPorFaltaDeAngulo() {
        Map<TipoAngulo, Double> fotos = fotosCompletas(150);
        fotos.remove(TipoAngulo.DETALLE);
        ResultadoAnalisis r = validador.validar(fotos);
        assertFalse(r.isAprobado());
        assertTrue(r.getCalidadFoto() < 10.0);
    }

    @Test
    public void rechazaImagenIlegible() {
        Map<TipoAngulo, Double> fotos = fotosCompletas(150);
        fotos.put(TipoAngulo.FRONTAL, -1.0);
        assertFalse(validador.validar(fotos).isAprobado());
    }

    @Test
    public void sinFotosEsRechazoConCalidadCero() {
        ResultadoAnalisis r = validador.validar(new EnumMap<>(TipoAngulo.class));
        assertFalse(r.isAprobado());
        assertEquals(0.0, r.getCalidadFoto(), 0.01);
        assertEquals(3, r.getMotivosRechazo().size());
    }
}
