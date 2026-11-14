package com.donaconecta.app.modelos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/** DTO de la tabla public.donaciones. El campo "estado" lo gestiona el servidor (no se envía). */
public class Donacion implements Serializable {
    public String id;
    @SerializedName("donante_id") public String donanteId;
    public String titulo;
    public String categoria;
    public String descripcion;
    public int cantidad = 1;
    public String unidad = "unidad";
    public String condicion = "nuevo";                 // nuevo | muy_bueno | bueno | usado
    @SerializedName("ubicacion_aproximada") public String ubicacionAproximada;
    @SerializedName("tipo_entrega") public String tipoEntrega = "retiro"; // retiro | punto_encuentro
}
