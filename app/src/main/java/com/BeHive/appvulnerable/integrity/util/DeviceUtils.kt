package com.BeHive.appvulnerable.integrity.util

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Utilidades para detectar la disponibilidad de servicios en el dispositivo.
 */
object DeviceUtils {

    /**
     * Comprueba si los Google Play Services están disponibles en el dispositivo.
     * 
     * @param context El contexto de la aplicación.
     * @return true si los Google Play Services están disponibles, false en caso contrario.
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    /**
     * Comprueba si los Huawei Mobile Services están disponibles en el dispositivo.
     * 
     * @param context El contexto de la aplicación.
     * @return true si los Huawei Mobile Services están disponibles, false en caso contrario.
     */
    fun isHuaweiMobileServicesAvailable(context: Context): Boolean {
        return try {
            // Comprueba si el paquete de HMS Core está instalado
            context.packageManager.getPackageInfo("com.huawei.hwid", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Comprueba si el dispositivo está rooteado basándose en verificaciones básicas.
     * Nota: Esto es una verificación básica y no es infalible.
     * 
     * @return true si se detectan signos de root, false en caso contrario.
     */
    fun hasRootAccess(): Boolean {
        val potentialRootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su"
        )
        
        return potentialRootPaths.any { java.io.File(it).exists() }
    }
    
    /**
     * Comprueba si la aplicación se está ejecutando en un emulador.
     * 
     * @return true si se detectan signos de emulador, false en caso contrario.
     */
    fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")
                || "google_sdk" == android.os.Build.PRODUCT)
    }
}