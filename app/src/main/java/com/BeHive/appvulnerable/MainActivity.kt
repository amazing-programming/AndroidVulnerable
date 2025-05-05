package com.BeHive.appvulnerable

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.BeHive.appvulnerable.integrity.IntegrityCheckFactory
import com.BeHive.appvulnerable.integrity.IntegrityResult
import com.BeHive.appvulnerable.ui.theme.AppVulnerableTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class MainActivity : ComponentActivity() {
    
    // Constante para log
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración de manejo de excepciones a nivel de proceso
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Error no capturado en el hilo $thread: ${throwable.message}", throwable)
            // Aquí podrías registrar el error en un servicio de monitoreo remoto
            // o realizar otras acciones antes de que la app finalice
        }
        
        setContent {
            AppVulnerableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntegrityCheckScreen(this)
                }
            }
        }
    }
}

@Composable
fun IntegrityCheckScreen(activity: ComponentActivity) {
    var integrityStatus by remember { mutableStateOf<IntegrityResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Handler para excepciones de coroutines
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("IntegrityCheck", "Error en coroutine: ${exception.message}", exception)
        isLoading = false
        errorMessage = "Error inesperado: ${exception.message}"
    }
    
    // Función para realizar la verificación de integridad
    fun performIntegrityCheck() {
        isLoading = true
        errorMessage = null
        
        // Usar SupervisorJob para que un error en una coroutine no cancele a las demás
        coroutineScope.launch(Dispatchers.Main + SupervisorJob() + exceptionHandler) {
            try {
                // Establecer un timeout global para toda la operación
                val result = withTimeoutOrNull(30000) { // 30 segundos máximo
                    try {
                        // Generar un nonce único para esta verificación
                        val nonce = UUID.randomUUID().toString()
                        
                        // Obtener el verificador de integridad más apropiado para este dispositivo
                        val integrityChecker = withContext(Dispatchers.IO) {
                            IntegrityCheckFactory.createIntegrityChecker(activity)
                        }
                        
                        // Realizar la verificación de integridad
                        val verificationResult = integrityChecker.verifyIntegrity(activity, nonce)
                        
                        // Agregar un pequeño retraso para evitar cambios de UI demasiado rápidos
                        delay(500)
                        
                        verificationResult
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e  // Permitir cancelación normal
                        Log.e("IntegrityCheck", "Error durante la verificación", e)
                        IntegrityResult.Error("Error durante el proceso: ${e.message}", e)
                    }
                }
                
                // Comprobar si se alcanzó el timeout
                if (result == null && isActive) {
                    integrityStatus = IntegrityResult.Error("La operación tardó demasiado tiempo")
                } else {
                    integrityStatus = result
                }
                
                // Registrar el resultado para fines de depuración
                Log.d("IntegrityCheck", "Resultado: $integrityStatus")
                
                // En un escenario real, aquí tomarías acciones basadas en el resultado
                when (val status = integrityStatus) {
                    is IntegrityResult.Valid -> {
                        // Continuar con la ejecución normal de la aplicación
                        Log.d("IntegrityCheck", "Verificación de integridad exitosa")
                    }
                    is IntegrityResult.Invalid -> {
                        // Tomar medidas para dispositivos no confiables
                        Log.e("IntegrityCheck", "Verificación de integridad fallida: ${status.reason}")
                        // Por ejemplo: limitar funciones, mostrar advertencias, etc.
                    }
                    is IntegrityResult.Error -> {
                        // Manejar errores de verificación
                        Log.e("IntegrityCheck", "Error en la verificación: ${status.message}", status.exception)
                        errorMessage = status.message
                    }
                    null -> {
                        errorMessage = "No se recibió respuesta"
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e  // Permitir cancelación normal
                Log.e("IntegrityCheck", "Error inesperado", e)
                errorMessage = "Error crítico: ${e.message}"
                integrityStatus = IntegrityResult.Error(errorMessage ?: "Error desconocido", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Verificando la integridad del dispositivo...")
            } else {
                when (val status = integrityStatus) {
                    is IntegrityResult.Valid -> {
                        Text(
                            text = "✅ El dispositivo pasó la verificación de integridad",
                            color = Color.Green,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    is IntegrityResult.Invalid -> {
                        Text(
                            text = "❌ Verificación de integridad fallida:\n${status.reason}",
                            color = Color.Red,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    is IntegrityResult.Error -> {
                        Text(
                            text = "⚠️ Error en la verificación de integridad:\n${status.message}",
                            color = Color(0xFFFFA500), // Orange
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    null -> {
                        if (errorMessage != null) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFFFA500), // Orange
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Presiona el botón para verificar la integridad",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { performIntegrityCheck() },
                    enabled = !isLoading
                ) {
                    Text("Verificar Integridad")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntegrityScreenPreview() {
    AppVulnerableTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✅ El dispositivo pasó la verificación de integridad",
                    color = Color.Green,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(onClick = { }) {
                    Text("Verificar Integridad")
                }
            }
        }
    }
}