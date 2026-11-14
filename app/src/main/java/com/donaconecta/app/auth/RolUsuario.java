package com.donaconecta.app.auth;

public enum RolUsuario {
    DONANTE("donante", "Donante individual"),
    EMPRESA("empresa", "Empresa / Comercio"),
    FUNDACION("fundacion", "Fundación / Organización");

    private final String valorBaseDatos;
    private final String etiqueta;

    RolUsuario(String valorBaseDatos, String etiqueta) {
        this.valorBaseDatos = valorBaseDatos;
        this.etiqueta = etiqueta;
    }

    public String getValorBaseDatos() { return valorBaseDatos; }
    public String getEtiqueta() { return etiqueta; }

    public static RolUsuario desdeValor(String valor) {
        for (RolUsuario rol : values()) if (rol.valorBaseDatos.equals(valor)) return rol;
        return DONANTE;
    }
}
