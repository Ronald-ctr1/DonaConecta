package com.donaconecta.app.ia;

import java.util.ArrayList;
import java.util.List;

/** Ángulos de fotografía que solicita el flujo de donación. */
public enum TipoAngulo {
    FRONTAL("frontal", "Frontal", true),
    TRASERA("trasera", "Trasera", true),
    DETALLE("detalle", "Detalle / Etiqueta", true),
    EXTRA("extra", "Extra", false);

    private final String clave;
    private final String etiqueta;
    private final boolean obligatorio;

    TipoAngulo(String clave, String etiqueta, boolean obligatorio) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.obligatorio = obligatorio;
    }

    public String getClave() { return clave; }
    public String getEtiqueta() { return etiqueta; }
    public boolean esObligatorio() { return obligatorio; }

    public static List<TipoAngulo> obligatorios() {
        List<TipoAngulo> lista = new ArrayList<>();
        for (TipoAngulo angulo : values()) if (angulo.obligatorio) lista.add(angulo);
        return lista;
    }
}
