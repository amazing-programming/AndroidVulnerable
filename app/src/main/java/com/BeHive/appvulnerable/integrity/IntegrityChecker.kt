package com.BeHive.appvulnerable.integrity

import android.content.Context

/**
 * Interfaz que define el contrato para los verificadores de integridad.
 */
interface IntegrityChecker {
    /**
     * Verifica la integridad del dispositivo y la aplicación.
     * 
     * @param context El contexto de la aplicación.
     * @param nonce Un valor único para esta verificación, para prevenir ataques de repetición.
     * @return Un [IntegrityResult] que indica el resultado de la verificación.
     */
    suspend fun verifyIntegrity(context: Context, nonce: String): IntegrityResult
    
    /**
     * Comprueba si este verificador de integridad está disponible en el dispositivo actual.
     * 
     * @param context El contexto de la aplicación.
     * @return true si este verificador puede funcionar en el dispositivo actual, false en caso contrario.
     */
    suspend fun isAvailable(context: Context): Boolean
}