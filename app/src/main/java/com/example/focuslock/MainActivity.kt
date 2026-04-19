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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// 1. NOVO ESTADO: CONCLUIDO
enum class EstadoSessao { AGUARDAR, FOCO, INTERROMPIDO, CONCLUIDO }

enum class NomeEcra { CONFIGURACAO, ATIVO, ESTATISTICAS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FocoAbsolutoApp() }
    }
}

@Composable
fun FocoAbsolutoApp() {
    var ecraAtual by remember { mutableStateOf(NomeEcra.CONFIGURACAO) }
    var tempoSelecionadoMinutos by remember { mutableStateOf(25) }
    var somSelecionado by remember { mutableStateOf("Lo-Fi") }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F0)) {
        when (ecraAtual) {
            NomeEcra.CONFIGURACAO -> {
                EcraConfiguracao(
                    tempoAtual = tempoSelecionadoMinutos,
                    somAtual = somSelecionado,
                    onTempoChange = { tempoSelecionadoMinutos = it },
                    onSomChange = { somSelecionado = it },
                    onIniciarClick = { ecraAtual = NomeEcra.ATIVO },
                    onHistoricoClick = { ecraAtual = NomeEcra.ESTATISTICAS }
                )
            }
            NomeEcra.ATIVO -> {
                EcraAtivo(
                    tempoInicialMinutos = tempoSelecionadoMinutos,
                    somSelecionado = somSelecionado,
                    onCancelarClick = { ecraAtual = NomeEcra.CONFIGURACAO }
                )
            }
            NomeEcra.ESTATISTICAS -> {
                EcraEstatisticas(
                    onVoltarClick = { ecraAtual = NomeEcra.CONFIGURACAO }
                )
            }
        }
    }
}

@Composable
fun EcraConfiguracao(
    tempoAtual: Int, somAtual: String,
    onTempoChange: (Int) -> Unit, onSomChange: (String) -> Unit,
    onIniciarClick: () -> Unit, onHistoricoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("FOCUSLOCK", color = Color.Gray, fontSize = 16.sp, letterSpacing = 2.sp)
        Text("Nova sessão", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 40.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Duração do foco", color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OpcaoBotao("${25} min", tempoAtual == 25) { onTempoChange(25) }
                    OpcaoBotao("${50} min", tempoAtual == 50) { onTempoChange(50) }
                    OpcaoBotao("${90} min", tempoAtual == 90) { onTempoChange(90) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ambiente sonoro", color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OpcaoBotao("Lo-Fi", somAtual == "Lo-Fi") { onSomChange("Lo-Fi") }
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

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onHistoricoClick) {
            Text("Ver Histórico de Foco", color = Color.DarkGray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun OpcaoBotao(texto: String, selecionado: Boolean, onClick: () -> Unit) {
    val corFundo = if (selecionado) Color(0xFF1A1A1A) else Color.Transparent
    val corTexto = if (selecionado) Color.White else Color.Black
    OutlinedButton(onClick = onClick, colors = ButtonDefaults.outlinedButtonColors(containerColor = corFundo), shape = RoundedCornerShape(20.dp)) {
        Text(texto, color = corTexto)
    }
}

// --------------------------------------------------------------------------
// ECRÃ 3: ESTATÍSTICAS
// --------------------------------------------------------------------------
@Composable
fun EcraEstatisticas(onVoltarClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("← Voltar", color = Color.Gray, modifier = Modifier.clickable { onVoltarClick() })
            Text("HISTÓRICO DE FOCO", color = Color.Gray, letterSpacing = 2.sp, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(50.dp))
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            EstatisticaCard("3h\n40", "foco hoje")
            EstatisticaCard("2", "quebras")
            EstatisticaCard("83%", "sem int.")
        }

        Text("Progresso semanal (min)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            BarraGrafico("Seg", 60, Color(0xFF5E35B1))
            BarraGrafico("Ter", 90, Color(0xFF5E35B1))
            BarraGrafico("Qua", 40, Color(0xFF5E35B1))
            BarraGrafico("Qui", 80, Color(0xFF5E35B1))
            BarraGrafico("Sex", 110, Color(0xFF5E35B1))
            BarraGrafico("Sáb", 20, Color(0xFF333333))
            BarraGrafico("Dom", 70, Color(0xFF4CAF50))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("5", color = Color(0xFFFFB300), fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                    Text("dias seguidos sem quebras", color = Color.White, fontSize = 16.sp)
                }
                Text("melhor sequência: 12 dias", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 30.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("Excelente consistência! Continua assim.", color = Color(0xFF4CAF50), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 14.sp)
    }
}

@Composable
fun EstatisticaCard(valor: String, legenda: String) {
    Card(
        modifier = Modifier.width(100.dp).height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(valor, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(legenda, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun BarraGrafico(dia: String, alturaP: Int, cor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(30.dp).height(alturaP.dp).background(cor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        Text(dia, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

// --------------------------------------------------------------------------
// ECRÃ 2: SESSÃO ATIVA
// --------------------------------------------------------------------------
@Composable
fun EcraAtivo(tempoInicialMinutos: Int, somSelecionado: String, onCancelarClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    var estadoSessao by remember { mutableStateOf(EstadoSessao.AGUARDAR) }
    var isEcraTapado by remember { mutableStateOf(false) }
    var isMovimentoBrusco by remember { mutableStateOf(false) }

    // ⚠️ ATENÇÃO: Está a 5 segundos para testes!
    val tempoTotalSegundos = 5
    var tempoRestanteSegundos by remember { mutableStateOf(tempoTotalSegundos) }

    var somFundoPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var alarmePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var sucessoPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    DisposableEffect(somSelecionado) {
        alarmePlayer = MediaPlayer.create(context, R.raw.alarme)
        alarmePlayer?.isLooping = true

        sucessoPlayer = MediaPlayer.create(context, R.raw.sucesso)

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

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (estadoSessao == EstadoSessao.CONCLUIDO) return

                if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val tapado = event.values[0] < (proximitySensor?.maximumRange ?: 5f)
                    isEcraTapado = tapado
                    if (tapado) isMovimentoBrusco = false
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
            sucessoPlayer?.release()
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(isEcraTapado, isMovimentoBrusco) {
        if (estadoSessao == EstadoSessao.CONCLUIDO) return@LaunchedEffect

        if (isEcraTapado && !isMovimentoBrusco) {
            if (estadoSessao != EstadoSessao.FOCO) {
                delay(1500)
                estadoSessao = EstadoSessao.FOCO
            }
        } else {
            if (estadoSessao == EstadoSessao.FOCO) {
                if (tempoRestanteSegundos < tempoTotalSegundos) {
                    estadoSessao = EstadoSessao.INTERROMPIDO
                } else {
                    estadoSessao = EstadoSessao.AGUARDAR
                }
            }
        }
    }

    LaunchedEffect(estadoSessao, tempoRestanteSegundos) {
        if (estadoSessao == EstadoSessao.FOCO && tempoRestanteSegundos > 0) {
            delay(1000L)
            tempoRestanteSegundos -= 1
            if (tempoRestanteSegundos == 0) {
                estadoSessao = EstadoSessao.CONCLUIDO
            }
        }
    }

    // GESTÃO DE ÁUDIO À PROVA DE BALA (Sem pauses redundantes)
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
                alarmePlayer?.start() // Apenas START! Direto e sem engasgos.
            }
            EstadoSessao.CONCLUIDO -> {
                somFundoPlayer?.pause()
                alarmePlayer?.pause()
                sucessoPlayer?.start()
            }
        }
    }

    val corFundo = when (estadoSessao) {
        EstadoSessao.FOCO -> Color(0xFF121212)
        EstadoSessao.CONCLUIDO -> Color(0xFF1B5E20)
        else -> Color(0xFF1C1C1C)
    }

    val corTexto = when (estadoSessao) {
        EstadoSessao.INTERROMPIDO -> Color(0xFFF44336)
        EstadoSessao.CONCLUIDO -> Color(0xFF4CAF50)
        else -> Color.White
    }

    val tempoFormatado = "%02d:%02d".format(tempoRestanteSegundos / 60, tempoRestanteSegundos % 60)

    Column(
        modifier = Modifier.fillMaxSize().background(corFundo).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (estadoSessao) {
                EstadoSessao.AGUARDAR -> "A AGUARDAR..."
                EstadoSessao.FOCO -> "SESSÃO ATIVA"
                EstadoSessao.INTERROMPIDO -> "SESSÃO EM PAUSA"
                EstadoSessao.CONCLUIDO -> "SESSÃO CONCLUÍDA!"
            },
            color = Color.Gray, fontSize = 14.sp, letterSpacing = 2.sp
        )

        Text(tempoFormatado, color = corTexto, fontSize = 80.sp, fontWeight = FontWeight.Bold)

        Text(
            text = when (estadoSessao) {
                EstadoSessao.AGUARDAR -> "Vire o telemóvel para baixo"
                EstadoSessao.FOCO -> "Foco profundo..."
                EstadoSessao.INTERROMPIDO -> "Interrupção detetada!"
                EstadoSessao.CONCLUIDO -> "Parabéns! Foco mantido com sucesso."
            },
            color = corTexto.copy(alpha = 0.7f), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center
        )

        Button(onClick = onCancelarClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), modifier = Modifier.padding(top = 100.dp)) {
            Text(if (estadoSessao == EstadoSessao.CONCLUIDO) "Voltar ao Menu" else "Terminar Sessão", fontSize = 16.sp, color = Color.Gray)
        }
    }
}