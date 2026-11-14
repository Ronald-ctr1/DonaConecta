# DonaConecta (Android · Java · Supabase)

## Estructura de paquetes (`com.donaconecta.app`)
| Paquete | Responsabilidad |
|---|---|
| `auth` | Inicio de sesión, registro con roles, repositorio de Supabase Auth |
| `publicacion` | Flujo de donación: datos (paso 1), fotos (paso 2), publicación en Supabase |
| `ia` | Análisis de fotos y regla de rechazo (`ValidadorEscaneo`, `ServicioAnalisisIA`, paso 3) |
| `conexion` | Cliente HTTP de Supabase, configuración, sesión local, ejecutor de tareas |
| `modelos` | DTOs (`Perfil`, `Donacion`) |

## Puesta en marcha
1. Crea un proyecto en Supabase y ejecuta `supabase/migraciones/001_esquema_inicial.sql`.
2. Copia `local.properties.example` a `local.properties` y completa `SUPABASE_URL` y `SUPABASE_ANON_KEY`.
3. Abre la carpeta en Android Studio (sincroniza Gradle) y ejecuta.
4. Genera el wrapper una vez: `gradle wrapper --gradle-version 8.7` y súbelo a Git.

## CI (GitHub Actions)
Crea los secretos `SUPABASE_URL` y `SUPABASE_ANON_KEY`. El flujo ejecuta pruebas, lint y compila el APK.

## Flujo de rechazo por IA
`ActividadFotosDonacion` → `ActividadAnalisisIA`: si falta un ángulo obligatorio o hay baja iluminación /
sobreexposición, se muestra la tarjeta de rechazo, **"Continuar" queda deshabilitado** y solo queda
"Volver a tomar las fotografías". En el servidor, un trigger solo pasa la donación a `publicada` si existe un análisis aprobado.
