package com.example.focuslock
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocoAbsolutoApp()
        }
    }
}

@Composable
fun FocoAbsolutoApp() {
    var isFocoAtivo by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        if (!isFocoAtivo) {
            EcraConfiguracao(onIniciarClick = { isFocoAtivo = true })
        } else {
            EcraAtivo(onCancelarClick = { isFocoAtivo = false })
        }
    }
}

@Composable
fun EcraConfiguracao(onIniciarClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Foco Absoluto",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "25:00",
            color = Color(0xFF4CAF50),
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Button(
            onClick = onIniciarClick,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(text = "Iniciar Foco", fontSize = 20.sp, color = Color.White)
        }
    }
}

@Composable
fun EcraAtivo(onCancelarClick: () -> Unit) {
    // 1. Obter o Contexto e os Serviços do Android
    val context = LocalContext.current

    // 2. Variável de estado para saber se o telemóvel está virado para a mesa (tapado)
    var isEcraTapado by remember { mutableStateOf(false) }

    // 3. Efeito que liga e desliga o sensor automaticamente
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    // O sensor de proximidade mede em centímetros.
                    // Se o valor for menor que o máximo do sensor, significa que está tapado.
                    val distancia = event.values[0]
                    isEcraTapado = distancia < (proximitySensor?.maximumRange ?: 5f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Inicia a escuta do sensor
        sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Limpa (desliga) o sensor quando saímos deste ecrã
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // 4. Mudança de Design Visual baseada no sensor
    val corFundo = if (isEcraTapado) Color(0xFF0D47A1) else Color(0xFF121212) // Azul se tapado, Escuro normal se destapado
    val corTexto = if (isEcraTapado) Color.White else Color(0xFFF44336) // Branco se tapado, Vermelho se destapado
    val textoEstado = if (isEcraTapado) "Modo de Foco Ativo!\nContinua assim." else "Sessão Interrompida!\nVire o ecrã para baixo."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundo) // O fundo muda aqui
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = textoEstado,
            color = corTexto,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Button(
            onClick = onCancelarClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
        ) {
            Text(text = "Cancelar Sessão", fontSize = 16.sp, color = Color.White)
        }
    }
}