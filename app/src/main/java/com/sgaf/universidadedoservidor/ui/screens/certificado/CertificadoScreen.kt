package com.sgaf.universidadedoservidor.ui.screens.certificado

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.sgaf.universidadedoservidor.core.components.EmptyMessage
import com.sgaf.universidadedoservidor.core.components.LoadingBox
import com.sgaf.universidadedoservidor.core.util.CertificadoPdfGenerator
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.GoldSjc
import com.sgaf.universidadedoservidor.ui.theme.SuccessGreen
import com.sgaf.universidadedoservidor.ui.theme.TextGray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificadoScreen(
    viewModel: CertificadoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val desempenho by viewModel.desempenho.collectAsState()
    val cargaHoraria by viewModel.cargaHoraria.collectAsState()
    val context = LocalContext.current
    var nome by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Certificado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        val d = desempenho
        when {
            d == null -> LoadingBox(contentPadding = innerPadding)

            !d.aprovado -> {
                EmptyMessage(
                    icon = Icons.Default.Lock,
                    titulo = "Certificado ainda indisponível",
                    descricao = "Conclua 100% das aulas e seja aprovado na prova final para emitir o certificado.\n\n" +
                        "Aulas: ${d.aulasConcluidas}/${d.totalAulas} • Prova final: ${(d.percentualAcerto * 100).toInt()}%",
                    contentPadding = innerPadding
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = GoldSjc,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "Parabéns! Você concluiu o curso",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = d.cursoTitulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BlueSjc
                    )
                    Text(
                        text = "Prova final: ${(d.percentualAcerto * 100).toInt()}%  •  ${d.aulasConcluidas}/${d.totalAulas} aulas",
                        fontSize = 13.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Seu nome completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val arquivo = CertificadoPdfGenerator.gerar(
                                context = context,
                                nomeAluno = nome.trim(),
                                cursoTitulo = d.cursoTitulo,
                                cargaHoraria = cargaHoraria,
                                dataTexto = LocalDate.now().format(DATA_FORMATTER)
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                arquivo
                            )
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Compartilhar certificado"))
                        },
                        enabled = nome.trim().length >= 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueSjc,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerar e compartilhar certificado", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "O certificado é gerado em PDF no seu aparelho, com o nome informado.",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

private val DATA_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
