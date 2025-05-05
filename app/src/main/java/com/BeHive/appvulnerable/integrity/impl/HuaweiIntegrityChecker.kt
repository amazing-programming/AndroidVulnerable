package com.BeHive.appvulnerable.integrity.impl

import android.content.Context
import com.BeHive.appvulnerable.integrity.IntegrityChecker
import com.BeHive.appvulnerable.integrity.IntegrityResult
import com.BeHive.appvulnerable.integrity.util.DeviceUtils
import com.huawei.hmf.tasks.Tasks
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.api.entity.safetydetect.SysIntegrityRequest
import com.huawei.hms.support.api.safetydetect.SafetyDetect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Implementación de [IntegrityChecker] utilizando Huawei Safety Detect.
 * 
 * @property appId El ID de la aplicación en Huawei AppGallery.
 */
class HuaweiIntegrityChecker(private val appId: String) : IntegrityChecker {

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
                        // Crear la solicitud de verificación de integridad
                        val nonceData = nonce.toByteArray(StandardCharsets.UTF_8)
                        val request = SysIntegrityRequest()
                        request.nonce = nonceData
                        request.appId = appId
                        
                        // Realizar la verificación de integridad con timeout
                        val sysIntegrityResp = try {
                            Tasks.await(
                                SafetyDetect.getClient(context).sysIntegrity(request),
                                TIMEOUT_SECONDS,
                                TimeUnit.SECONDS
                            )
                        } catch (e: TimeoutException) {
                            return@withContext IntegrityResult.Error("Tiempo de espera agotado al solicitar la verificación", e)
                        } catch (e: ApiException) {
                            return@withContext IntegrityResult.Error("Error de API de Huawei: ${e.message} (código: ${e.statusCode})", e)
                        } catch (e: Exception) {
                            return@withContext IntegrityResult.Error("Error al solicitar verificación: ${e.message}", e)
                        }
                        
                        // Obtener el JWS (JSON Web Signature)
                        val jwsResponse = sysIntegrityResp.result ?: 
                            return@withContext IntegrityResult.Error("La respuesta de integridad es nula")
                            
                        val jwsParts = jwsResponse.split(".")
                        
                        if (jwsParts.size >= 2) {
                            try {
                                // Decodificar la carga útil (payload)
                                val payload = String(Base64.getUrlDecoder().decode(jwsParts[1]))
                                val jsonPayload = JSONObject(payload)
                                
                                // Verificar si la respuesta es básica o detallada
                                if (jsonPayload.has("basicIntegrity")) {
                                    // Verificación básica
                                    val basicIntegrity = jsonPayload.getBoolean("basicIntegrity")
                                    
                                    if (basicIntegrity) {
                                        return@withContext IntegrityResult.Valid
                                    } else {
                                        if (jsonPayload.has("advice")) {
                                            val advice = jsonPayload.getString("advice")
                                            return@withContext IntegrityResult.Invalid("Integridad comprometida: $advice")
                                        }
                                        return@withContext IntegrityResult.Invalid("El dispositivo no cumple con los requisitos de integridad")
                                    }
                                } else if (jsonPayload.has("isBasicIntegrityOK")) {
                                    // Verificación detallada (HMS Core 5.0+)
                                    val isBasicIntegrityOK = jsonPayload.getBoolean("isBasicIntegrityOK")
                                    val isCtsProfileMatch = jsonPayload.getBoolean("isCtsProfileMatch")
                                    
                                    if (isBasicIntegrityOK && isCtsProfileMatch) {
                                        return@withContext IntegrityResult.Valid
                                    } else {
                                        val reason = when {
                                            !isBasicIntegrityOK && !isCtsProfileMatch -> "El dispositivo está comprometido y no cumple con el perfil CTS"
                                            !isBasicIntegrityOK -> "El dispositivo está comprometido"
                                            !isCtsProfileMatch -> "El dispositivo no cumple con el perfil CTS"
                                            else -> "Error desconocido"
                                        }
                                        return@withContext IntegrityResult.Invalid(reason)
                                    }
                                }
                                
                                return@withContext IntegrityResult.Error("Formato de respuesta de integridad desconocido")
                            } catch (e: JSONException) {
                                return@withContext IntegrityResult.Error("Error al procesar la respuesta: ${e.message}", e)
                            } catch (e: IllegalArgumentException) {
                                return@withContext IntegrityResult.Error("Error al decodificar la respuesta: ${e.message}", e)
                            }
                        }
                        
                        IntegrityResult.Error("No se pudo verificar la respuesta de integridad: formato inválido")
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
            DeviceUtils.isHuaweiMobileServicesAvailable(context)
        } catch (e: Exception) {
            // Si hay algún error al verificar la disponibilidad, consideramos que no está disponible
            false
        }
    }
}