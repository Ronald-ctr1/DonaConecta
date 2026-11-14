package com.donaconecta.app.conexion;

public class ExcepcionSupabase extends Exception {
    private final int codigoHttp;

    public ExcepcionSupabase(String mensaje, int codigoHttp) {
        super(mensaje);
        this.codigoHttp = codigoHttp;
    }

    public ExcepcionSupabase(String mensaje, int codigoHttp, Throwable causa) {
        super(mensaje, causa);
        this.codigoHttp = codigoHttp;
    }

    public int getCodigoHttp() { return codigoHttp; }
}
