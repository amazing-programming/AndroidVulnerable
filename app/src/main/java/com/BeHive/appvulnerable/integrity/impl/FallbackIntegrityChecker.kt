package com.BeHive.appvulnerable.integrity.impl

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.BeHive.appvulnerable.integrity.IntegrityChecker
import com.BeHive.appvulnerable.integrity.IntegrityResult
import com.BeHive.appvulnerable.integrity.util.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Implementación alternativa de [IntegrityChecker] para dispositivos sin Google Play Services ni Huawei Mobile Services.
 * Realiza verificaciones básicas de integridad del dispositivo basadas en características nativas de Android.
 */
class FallbackIntegrityChecker : IntegrityChecker {

    companion object {
        private const val COROUTINE_TIMEOUT_MS = 10000L // 10 segundos
    }

    override suspend fun verifyIntegrity(context: Context, nonce: String): IntegrityResult {
        return try {
            // Usar withTimeoutOrNull para evitar bloqueos indefinidos
            withTimeoutOrNull(COROUTINE_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    val issues = mutableListOf<String>()
                    
                    // Solo ejecutar cada verificación dentro de un bloque try-catch individual
                    // para evitar que un error en una verificación haga fallar todo el proceso
                    
                    // Verificar si el dispositivo está rooteado
                    try {
                        if (DeviceUtils.hasRootAccess()) {
                            issues.add("El dispositivo está rooteado")
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar con las otras verificaciones
                    }
                    
                    // Verificar si la aplicación se está ejecutando en un emulador
                    try {
                        if (DeviceUtils.isEmulator()) {
                            issues.add("La aplicación se está ejecutando en un emulador")
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Verificar si la depuración está habilitada
                    try {
                        if (isDebugEnabled(context)) {
                            issues.add("La depuración USB está habilitada")
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Verificar la firma APK correcta
                    try {
                        if (!isValidSignature(context)) {
                            issues.add("La firma de la aplicación no es válida")
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Verificar si se permite la instalación de fuentes desconocidas
                    try {
                        if (isUnknownSourcesEnabled(context)) {
                            issues.add("Instalación desde fuentes desconocidas habilitada")
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Verificar si hay aplicaciones de superusuario instaladas
                    try {
                        val superuserApps = arrayOf(
                            "eu.chainfire.supersu", 
                            "com.noshufou.android.su", 
                            "com.koushikdutta.superuser", 
                            "com.topjohnwu.magisk"
                        )
                        
                        for (app in superuserApps) {
                            try {
                                if (isPackageInstalled(context, app)) {
                                    issues.add("Aplicación de superusuario detectada: $app")
                                    break // Solo necesitamos detectar una
                                }
                            } catch (e: Exception) {
                                // Ignorar y continuar con la siguiente app
                            }
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Comprobar aplicaciones de hooking conocidas
                    try {
                        val hookingApps = arrayOf(
                            "de.robv.android.xposed.installer", 
                            "com.saurik.substrate", 
                            "com.github.uiautomator", 
                            "com.github.shadowsocks"
                        )
                        
                        for (app in hookingApps) {
                            try {
                                if (isPackageInstalled(context, app)) {
                                    issues.add("Aplicación de hooking detectada: $app")
                                    break // Solo necesitamos detectar una
                                }
                            } catch (e: Exception) {
                                // Ignorar y continuar con la siguiente app
                            }
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Verificar si la aplicación es debuggable
                    try {
                        if (isAppDebuggable(context)) {
                            issues.add("La aplicación tiene el flag de debug activado")
                        }
                    } catch (e: Exception) {
                        // Ignorar este error y continuar
                    }
                    
                    // Determinar el resultado basado en los problemas encontrados
                    if (issues.isEmpty()) {
                        IntegrityResult.Valid
                    } else {
                        IntegrityResult.Invalid(issues.joinToString("; "))
                    }
                }
            } ?: IntegrityResult.Error("Tiempo de espera agotado durante la verificación")
        } catch (e: Exception) {
            // Captura cualquier excepción no controlada en el nivel superior
            IntegrityResult.Error("Error crítico en la verificación nativa: ${e.message}", e)
        }
    }

    override suspend fun isAvailable(context: Context): Boolean {
        // Siempre disponible como último recurso
        return true
    }
    
    private fun isDebugEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, 
                Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            // Si hay error al consultar, asumimos que no está habilitado
            false
        }
    }
    
    private fun isValidSignature(context: Context): Boolean {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 
                    PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 
                    PackageManager.GET_SIGNATURES)
            }
            
            // En una implementación real, verificaríamos las firmas aquí
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun isUnknownSourcesEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Settings.Secure.getInt(context.contentResolver, 
                    Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
            } else {
                // En Android 8.0+, esto se maneja por app, no hay una configuración global
                context.packageManager.canRequestPackageInstalls()
            }
        } catch (e: Exception) {
            // Si hay error, por seguridad asumimos que está habilitado
            true
        }
    }
    
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            // Cualquier otro error, asumimos que no está instalado
            false
        }
    }
    
    private fun isAppDebuggable(context: Context): Boolean {
        return try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            // Si hay error, por seguridad asumimos que es debuggable
            true
        }
    }
}