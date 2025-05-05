package com.BeHive.appvulnerable.integrity

/**
 * Clase que representa el resultado de una verificación de integridad.
 */
sealed class IntegrityResult {
    /**
     * El dispositivo y la aplicación pasaron la verificación de integridad.
     */
    object Valid : IntegrityResult()
    
    /**
     * La verificación de integridad falló.
     * @param reason La razón por la cual falló la verificación.
     */
    data class Invalid(val reason: String) : IntegrityResult()
    
    /**
     * Ocurrió un error durante la verificación de integridad.
     * @param message Mensaje de error.
     * @param exception Excepción que causó el error (opcional).
     */
    data class Error(val message: String, val exception: Exception? = null) : IntegrityResult()
}