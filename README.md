# Android Integrity Check

Una aplicación Android que implementa verificaciones de integridad usando múltiples APIs:
- Google Play Integrity API para dispositivos con Google Play Services
- Huawei Safety Detect para dispositivos Huawei
- Verificaciones nativas como fallback para otros dispositivos

## Configuración del proyecto

### 1. Configurar credenciales de integridad

Para poder utilizar las APIs de verificación de integridad, debes configurar las credenciales:

1. Copia el archivo `app/src/main/java/com/BeHive/appvulnerable/integrity/config/IntegrityConfig.kt.example` como `IntegrityConfig.kt` en el mismo directorio.
2. Edita el archivo `IntegrityConfig.kt` con tus propias credenciales:

```kotlin
// Configura tu número de proyecto de Google Cloud 
// Obtenlo desde: https://console.cloud.google.com/
const val GOOGLE_CLOUD_PROJECT_NUMBER = 123456789L

// Configura tu ID de app de Huawei
// Obtenlo desde: https://developer.huawei.com/consumer/en/console
const val HUAWEI_APP_ID = "your_huawei_app_id"
```

### 2. Obtener credenciales de Google Play Integrity API

1. Accede a la [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona uno existente
3. Habilita la API de Google Play Integrity
4. Obtén el número de proyecto (es un valor numérico largo)
5. Configura las protecciones de aplicación en la [Google Play Console](https://play.google.com/console)

### 3. Obtener credenciales de Huawei Safety Detect

1. Regístrate en la [Huawei Developer Console](https://developer.huawei.com/consumer/en/console)
2. Crea un nuevo proyecto y una aplicación
3. Habilita SafetyDetect en la sección "Manage APIs"
4. Obtén el App ID para tu aplicación

## Características

- Verificación de integridad de dispositivo y aplicación
- Adaptación automática según plataforma (Google, Huawei, otros)
- Manejo robusto de errores y excepciones
- Interface de usuario para mostrar resultados de verificación

## Requisitos

- Android Studio Arctic Fox (2020.3.1) o posterior
- Android SDK 21+
- Kotlin 1.5.0+