package com.donaconecta.app.conexion;

import com.donaconecta.app.BuildConfig;

/** Valores de conexión. Se inyectan en compilación desde local.properties o variables de entorno (CI). */
public final class ConfiguracionSupabase {
    public static final String URL_BASE = BuildConfig.SUPABASE_URL.replaceAll("/+$", "");
    public static final String CLAVE_ANONIMA = BuildConfig.SUPABASE_ANON_KEY;
    public static final String BUCKET_FOTOS = "fotos-donaciones";

    private ConfiguracionSupabase() { }
}
