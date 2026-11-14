package com.donaconecta.app.modelos;

import com.google.gson.annotations.SerializedName;

/** DTO de la tabla public.perfiles (campos públicos: son simples contenedores de datos). */
public class Perfil {
    public String id;
    @SerializedName("nombre_completo") public String nombreCompleto;
    public String rol;
}
