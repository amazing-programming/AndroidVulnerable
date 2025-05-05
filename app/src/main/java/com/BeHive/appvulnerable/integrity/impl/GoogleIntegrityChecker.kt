package com.BeHive.appvulnerable.integrity.impl

import android.content.Context
import com.BeHive.appvulnerable.integrity.IntegrityChecker
import com.BeHive.appvulnerable.integrity.IntegrityResult
import com.BeHive.appvulnerable.integrity.util.DeviceUtils
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Implementación de [IntegrityChecker] utilizando Google Play Integrity API.
 * 
 * @property cloudProjectNumber El número del proyecto en Google Cloud.
 */
class GoogleIntegrityChecker(private val cloudProjectNumber: Long) : IntegrityChecker {

    companion object {
        private const val TIMEOUT_SECONDS = 15L
        private const val COROUTINE_TIMEOUT_MS = 20000L // 20 segundos
    }

    override suspend fun verifyIntegrity(context: Context, nonce: String): IntegrityResult {
        return try {
            // Usar withTimeoutOrNull para evitar bloqueos indefinidos
            withTimeoutOrNull(COROUTINE_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    try {
                        // Crear un IntegrityManager
                        val integrityManager = IntegrityManagerFactory.create(context)
                        
                        // Construir la solicitud del token de integridad
                        val request = IntegrityTokenRequest.builder()
                            .setNonce(nonce)
                            .setCloudProjectNumber(cloudProjectNumber)
                            .build()
                        
                        // Obtener el token de integridad con timeout
                        val integrityTokenResponse = try {
                            Tasks.await(
                                integrityManager.requestIntegrityToken(request),
                                TIMEOUT_SECONDS,
                                TimeUnit.SECONDS
                            )
                        } catch (e: TimeoutException) {
                            return@withContext IntegrityResult.Error("Tiempo de espera agotado al solicitar el token de integridad", e)
                        } catch (e: Exception) {
                            return@withContext IntegrityResult.Error("Error al solicitar token: ${e.message}", e)
                        }
                        
                        // Decodificar y verificar el token (payload parte de JWT)
                        val token = integrityTokenResponse.token()
                        val tokenParts = token.split(".")
                        if (tokenParts.size >= 2) {
                            try {
                                val payload = String(Base64.getUrlDecoder().decode(tokenParts[1]))
                                val jsonPayload = JSONObject(payload)
                                
                                // Verificar el estado de la aplicación
                                val appIntegrity = jsonPayload.optJSONObject("appIntegrity")
                                if (appIntegrity == null) {
                                    return@withContext IntegrityResult.Error("Formato de token inválido: falta appIntegrity")
                                }
                                
                                // Verificar si la aplicación es genuina
                                val appRecognitionVerdict = appIntegrity.optString("appRecognitionVerdict")
                                if (appRecognitionVerdict != "PLAY_RECOGNIZED") {
                                    return@withContext IntegrityResult.Invalid("La aplicación no es genuina o ha sido manipulada")
                                }
                                
                                // Verificar el estado del dispositivo
                                val deviceIntegrity = jsonPayload.optJSONObject("deviceIntegrity")
                                if (deviceIntegrity == null) {
                                    return@withContext IntegrityResult.Error("Formato de token inválido: falta deviceIntegrity")
                                }
                                
                                val deviceRecognitionVerdict = deviceIntegrity.optJSONArray("deviceRecognitionVerdict")
                                if (deviceRecognitionVerdict == null || deviceRecognitionVerdict.length() == 0) {
                                    return@withContext IntegrityResult.Error("Formato de token inválido: falta deviceRecognitionVerdict")
                                }
                                
                                // Comprobar si el dispositivo está comprometido
                                for (i in 0 until deviceRecognitionVerdict.length()) {
                                    val verdict = deviceRecognitionVerdict.optString(i)
                                    if (verdict == "MEETS_DEVICE_INTEGRITY") {
                                        // El dispositivo pasó la verificación de integridad
                                        return@withContext IntegrityResult.Valid
                                    }
                                }
                                
                                return@withContext IntegrityResult.Invalid("El dispositivo no cumple con los requisitos de integridad")
                            } catch (e: JSONException) {
                                return@withContext IntegrityResult.Error("Error al procesar el token: ${e.message}", e)
                            } catch (e: IllegalArgumentException) {
                                return@withContext IntegrityResult.Error("Error al decodificar el token: ${e.message}", e)
                            }
                        }
                        
                        IntegrityResult.Error("No se pudo verificar el token de integridad: formato inválido")
                    } catch (e: Exception) {
                        IntegrityResult.Error("Error inesperado al verificar la integridad: ${e.message}", e)
                    }
                }
            } ?: IntegrityResult.Error("Tiempo de espera agotado durante la verificación")
        } catch (e: Exception) {
            // Captura cualquier excepción no controlada en el nivel superior
            IntegrityResult.Error("Error crítico en la verificación: ${e.message}", e)
        }
    }

    override suspend fun isAvailable(context: Context): Boolean {
        return try {
            DeviceUtils.isGooglePlayServicesAvailable(context)
        } catch (e: Exception) {
            // Si hay algún error al verificar la disponibilidad, consideramos que no está disponible
            false
        }
    }
}