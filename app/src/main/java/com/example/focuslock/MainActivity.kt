package com.example.focuslock

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlin.math.sqrt

enum class EstadoSessao {
    AGUARDAR, FOCO, INTERROMPIDO
}

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
    var tempoSelecionadoMinutos by remember { mutableStateOf(25) }
    var somSelecionado by remember { mutableStateOf("Lo-Fi") }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F0)) {
        if (!isFocoAtivo) {
            EcraConfiguracao(
                tempoAtual = tempoSelecionadoMinutos,
                somAtual = somSelecionado,
                onTempoChange = { tempoSelecionadoMinutos = it },
                onSomChange = { somSelecionado = it },
                onIniciarClick = { isFocoAtivo = true }
            )
        } else {
            EcraAtivo(
                tempoInicialMinutos = tempoSelecionadoMinutos,
                somSelecionado = somSelecionado,
                onCancelarClick = { isFocoAtivo = false }
            )
        }
    }
}

@Composable
fun EcraConfiguracao(
    tempoAtual: Int,
    somAtual: String,
    onTempoChange: (Int) -> Unit,
    onSomChange: (String) -> Unit,
    onIniciarClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("FOCUSLOCK", color = Color.Gray, fontSize = 16.sp, letterSpacing = 2.sp)
        Text("Nova sessão", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 40.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Duração do foco", color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OpcaoBotao("${25} min", tempoAtual == 25) { onTempoChange(25) }
                    OpcaoBotao("${50} min", tempoAtual == 50) { onTempoChange(50) }
                    OpcaoBotao("${90} min", tempoAtual == 90) { onTempoChange(90) }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ambiente sonoro", color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpcaoBotao("Lo-Fi", somAtual == "Lo-Fi") { onSomChange("Lo-Fi") }
                    OpcaoBotao("Natureza", somAtual == "Natureza") { onSomChange("Natureza") }
                    OpcaoBotao("Silêncio", somAtual == "Silêncio") { onSomChange("Silêncio") }
                }
            }
        }

        Button(
            onClick = onIniciarClick,
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Text("▶", fontSize = 32.sp, color = Color.White)
        }
    }
}

@Composable
fun OpcaoBotao(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val corFundo = if (selecionado) Color(0xFF1A1A1A) else Color.Transparent
    val corTexto = if (selecionado) Color.White else Color.Black
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = corFundo),
        shape = RoundedCornerShape(20.dp)
    ) { Text(texto, color = corTexto) }
}

@Composable
fun EcraAtivo(tempoInicialMinutos: Int, somSelecionado: String, onCancelarClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    var estadoSessao by remember { mutableStateOf(EstadoSessao.AGUARDAR) }
    var isEcraTapado by remember { mutableStateOf(false) }
    var isMovimentoBrusco by remember { mutableStateOf(false) }

    val tempoTotalSegundos = tempoInicialMinutos * 60
    var tempoRestanteSegundos by remember { mutableStateOf(tempoTotalSegundos) }

    var somFundoPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var alarmePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    DisposableEffect(somSelecionado) {
        alarmePlayer = MediaPlayer.create(context, R.raw.alarme)?.apply {
            isLooping = true
            setVolume(1.0f, 1.0f)
        }

        val playlistLofi = listOf(R.raw.lofi1, R.raw.lofi2, R.raw.lofi3)
        var indexMusicaAtual = 0

        fun carregarProximaMusica() {
            somFundoPlayer?.release()
            val novaMusica = MediaPlayer.create(context, playlistLofi[indexMusicaAtual])
            novaMusica?.setOnCompletionListener {
                indexMusicaAtual = (indexMusicaAtual + 1) % playlistLofi.size
                carregarProximaMusica()
                if (estadoSessao == EstadoSessao.FOCO) somFundoPlayer?.start()
            }
            somFundoPlayer = novaMusica
        }

        if (somSelecionado == "Lo-Fi") carregarProximaMusica()
        else if (somSelecionado == "Natureza") {
            val resId = context.resources.getIdentifier("natureza", "raw", context.packageName)
            if (resId != 0) somFundoPlayer = MediaPlayer.create(context, resId)?.apply { isLooping = true }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val tapado = event.values[0] < (proximitySensor?.maximumRange ?: 5f)
                    isEcraTapado = tapado

                    // CORREÇÃO CRÍTICA AQUI: "Perdoa" o movimento se voltar a tapar!
                    if (tapado) {
                        isMovimentoBrusco = false
                    }
                }
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val aceleracao = sqrt(event.values.map { it * it }.sum().toDouble()).toFloat()
                    if (aceleracao > 19f) isMovimentoBrusco = true
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            somFundoPlayer?.release()
            alarmePlayer?.release()
            sensorManager.unregisterListener(listener)
        }
    }

    // LÓGICA DA MÁQUINA DE ESTADOS COM RETOMA DE FOCO
    LaunchedEffect(isEcraTapado, isMovimentoBrusco) {
        if (isEcraTapado && !isMovimentoBrusco) {
            // Se está tapado e sem movimento, retoma o FOCO sempre!
            estadoSessao = EstadoSessao.FOCO
        } else if (estadoSessao == EstadoSessao.FOCO) {
            // Se estava no foco e quebrou as regras...
            if (tempoRestanteSegundos < tempoTotalSegundos) {
                estadoSessao = EstadoSessao.INTERROMPIDO
            } else {
                estadoSessao = EstadoSessao.AGUARDAR
            }
        }
    }

    LaunchedEffect(estadoSessao, tempoRestanteSegundos) {
        if (estadoSessao == EstadoSessao.FOCO && tempoRestanteSegundos > 0) {
            delay(1000L)
            tempoRestanteSegundos -= 1
        }
    }

    LaunchedEffect(estadoSessao) {
        when (estadoSessao) {
            EstadoSessao.AGUARDAR -> {
                somFundoPlayer?.pause()
                alarmePlayer?.pause()
                alarmePlayer?.seekTo(0)
            }
            EstadoSessao.FOCO -> {
                alarmePlayer?.pause()
                alarmePlayer?.seekTo(0)
                somFundoPlayer?.start()
            }
            EstadoSessao.INTERROMPIDO -> {
                somFundoPlayer?.pause()
                alarmePlayer?.seekTo(0)
                alarmePlayer?.start()
            }
        }
    }

    val tituloSessao = when (estadoSessao) {
        EstadoSessao.AGUARDAR -> "A AGUARDAR..."
        EstadoSessao.FOCO -> "SESSÃO ATIVA"
        EstadoSessao.INTERROMPIDO -> "SESSÃO EM PAUSA"
    }

    val corFundo = if (estadoSessao == EstadoSessao.FOCO) Color(0xFF121212) else Color(0xFF1C1C1C)
    val corTexto = if (estadoSessao == EstadoSessao.INTERROMPIDO) Color(0xFFF44336) else Color.White
    val tempoFormatado = "%02d:%02d".format(tempoRestanteSegundos / 60, tempoRestanteSegundos % 60)

    Column(
        modifier = Modifier.fillMaxSize().background(corFundo).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(tituloSessao, color = Color.Gray, fontSize = 14.sp, letterSpacing = 2.sp)
        Text(tempoFormatado, color = corTexto, fontSize = 80.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (estadoSessao == EstadoSessao.AGUARDAR) "Vire o telemóvel para baixo" else if (estadoSessao == EstadoSessao.FOCO) "Foco profundo..." else "Interrupção detetada!",
            color = corTexto.copy(alpha = 0.7f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Button(onClick = onCancelarClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), modifier = Modifier.padding(top = 100.dp)) {
            Text("Terminar Sessão", fontSize = 16.sp, color = Color.Gray)
        }
    }
}