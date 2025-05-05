package com.BeHive.appvulnerable.integrity

import android.content.Context
import android.util.Log
import com.BeHive.appvulnerable.integrity.config.IntegrityConfig
import com.BeHive.appvulnerable.integrity.impl.FallbackIntegrityChecker
import com.BeHive.appvulnerable.integrity.impl.GoogleIntegrityChecker
import com.BeHive.appvulnerable.integrity.impl.HuaweiIntegrityChecker
import com.BeHive.appvulnerable.integrity.util.DeviceUtils

/**
 * Fábrica que crea la implementación más apropiada de [IntegrityChecker] basada en las 
 * características del dispositivo.
 */
class IntegrityCheckFactory {

    companion object {
        private const val TAG = "IntegrityCheckFactory"
    
        /**
         * Crea y retorna la implementación más apropiada de [IntegrityChecker] basada en
         * las capacidades del dispositivo.
         * 
         * @param context El contexto de la aplicación.
         * @return La implementación más apropiada de [IntegrityChecker].
         */
        suspend fun createIntegrityChecker(context: Context): IntegrityChecker {
            try {
                // Intentar Google Play Services primero
                try {
                    val googleChecker = GoogleIntegrityChecker(IntegrityConfig.GOOGLE_CLOUD_PROJECT_NUMBER)
                    if (googleChecker.isAvailable(context)) {
                        Log.d(TAG, "Usando Google Play Integrity API")
                        return googleChecker
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al verificar disponibilidad de Google Play Services: ${e.message}")
                    // Continuar con la siguiente opción
                }
                
                // Si Google no está disponible, intentar Huawei HMS
                try {
                    val huaweiChecker = HuaweiIntegrityChecker(IntegrityConfig.HUAWEI_APP_ID)
                    if (huaweiChecker.isAvailable(context)) {
                        Log.d(TAG, "Usando Huawei Safety Detect")
                        return huaweiChecker
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al verificar disponibilidad de Huawei HMS: ${e.message}")
                    // Continuar con la siguiente opción
                }
                
                // Si ninguno de los anteriores está disponible, usar la implementación alternativa
                Log.d(TAG, "Usando verificación de integridad alternativa")
                return FallbackIntegrityChecker()
            } catch (e: Exception) {
                // Si ocurre algún error inesperado, usar la implementación alternativa como último recurso
                Log.e(TAG, "Error inesperado al seleccionar verificador de integridad: ${e.message}")
                return FallbackIntegrityChecker()
            }
        }
    }
}