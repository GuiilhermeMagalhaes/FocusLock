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
import kotlin.math.sqrt

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
    val context = LocalContext.current

    // Estados dos sensores
    var isEcraTapado by remember { mutableStateOf(false) }
    var isMovimentoBrusco by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // 1. Lógica do Sensor de Proximidade
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val distancia = event.values[0]
                    isEcraTapado = distancia < (proximitySensor?.maximumRange ?: 5f)

                    // Se o ecrã voltar a ser tapado, perdoamos o movimento brusco anterior
                    if (isEcraTapado) {
                        isMovimentoBrusco = false
                    }
                }

                // 2. Lógica do Acelerómetro
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calcular a magnitude da aceleração
                    val aceleracao = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    // A gravidade normal é ~9.81. Se for maior que 13, houve um abanão forte!
                    if (aceleracao > 13f) {
                        isMovimentoBrusco = true
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Registar ambos os sensores
        sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Lógica para decidir o que mostrar no ecrã
    val estaTudoCorreto = isEcraTapado && !isMovimentoBrusco

    val corFundo = if (estaTudoCorreto) Color(0xFF0D47A1) else Color(0xFF121212)
    val corTexto = if (estaTudoCorreto) Color.White else Color(0xFFF44336)

    val textoEstado = when {
        isMovimentoBrusco -> "Aviso!\nMovimento brusco detetado!"
        !isEcraTapado -> "Sessão Interrompida!\nVire o ecrã para baixo."
        else -> "Modo de Foco Ativo!\nContinua assim."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundo)
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