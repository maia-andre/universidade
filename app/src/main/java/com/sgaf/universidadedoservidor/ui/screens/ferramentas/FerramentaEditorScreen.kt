package com.sgaf.universidadedoservidor.ui.screens.ferramentas

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sgaf.universidadedoservidor.core.components.LoadingBox
import com.sgaf.universidadedoservidor.core.util.FerramentaPdfGenerator
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerramentaEditorScreen(
    viewModel: FerramentaEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.concluido) {
        if (state.concluido) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.tipo.rotulo, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.podeExcluir) {
                        IconButton(onClick = { viewModel.excluir() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingBox(contentPadding = innerPadding)
            return@Scaffold
        }

        var titulo by rememberSaveable(state.tituloInicial) { mutableStateOf(state.tituloInicial) }
        val valores = remember(state.camposIniciais, state.tipo) {
            mutableStateMapOf<String, String>().also { mapa ->
                state.tipo.campos.forEach { campo ->
                    mapa[campo.chave] = state.camposIniciais[campo.chave] ?: ""
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.tipo.campos.forEach { campo ->
                OutlinedTextField(
                    value = valores[campo.chave] ?: "",
                    onValueChange = { valores[campo.chave] = it },
                    label = { Text(campo.rotulo) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { viewModel.salvar(titulo, valores.toMap()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueSjc, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salvar", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val campos = state.tipo.campos.map { it.rotulo to (valores[it.chave] ?: "") }
                    val arquivo = FerramentaPdfGenerator.gerar(
                        context = context,
                        titulo = titulo.ifBlank { state.tipo.rotulo },
                        subtitulo = state.tipo.rotulo,
                        campos = campos
                    )
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", arquivo
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Exportar ferramenta"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BlueSjc)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar PDF", fontWeight = FontWeight.Bold, color = BlueSjc)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
