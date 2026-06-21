package com.sgaf.universidadedoservidor.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.core.data.preferences.ThemeMode
import com.sgaf.universidadedoservidor.ui.theme.BlueSjc
import com.sgaf.universidadedoservidor.ui.theme.TextGray

private data class ThemeOption(
    val mode: ThemeMode,
    val titulo: String,
    val descricao: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAcessibilidade: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val trocaSenha by viewModel.trocaSenha.collectAsState()
    val context = LocalContext.current
    var showTrocarSenha by remember { mutableStateOf(false) }

    // Sucesso na troca de senha: fecha o diálogo e confirma ao aluno.
    LaunchedEffect(trocaSenha.sucesso) {
        if (trocaSenha.sucesso) {
            showTrocarSenha = false
            Toast.makeText(context, "Senha alterada com sucesso.", Toast.LENGTH_LONG).show()
            viewModel.limparTrocaSenha()
        }
    }

    val options = listOf(
        ThemeOption(ThemeMode.SYSTEM, "Padrão do sistema", "Acompanha o tema do aparelho", Icons.Default.SettingsBrightness),
        ThemeOption(ThemeMode.LIGHT, "Claro", "Sempre no tema claro", Icons.Default.LightMode),
        ThemeOption(ThemeMode.DARK, "Escuro", "Sempre no tema escuro", Icons.Default.DarkMode)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Brightness6,
                    contentDescription = null,
                    tint = BlueSjc
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aparência",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Escolha como o aplicativo deve ser exibido.",
                fontSize = 13.sp,
                color = TextGray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            options.forEach { option ->
                ThemeOptionRow(
                    option = option,
                    selected = option.mode == themeMode,
                    onClick = { viewModel.setThemeMode(option.mode) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAcessibilidade),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Accessibility,
                        contentDescription = null,
                        tint = BlueSjc
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acessibilidade",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tamanho de fonte, alto contraste e movimento",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextGray
                    )
                }
            }

            // Seção Conta: trocar a própria senha (v7, Item 2).
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = BlueSjc
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.limparTrocaSenha()
                        showTrocarSenha = true
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = BlueSjc
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trocar senha",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Altere a senha de acesso ao aplicativo",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da conta")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTrocarSenha) {
        TrocarSenhaDialog(
            state = trocaSenha,
            onConfirm = { atual, nova, confirmacao -> viewModel.trocarSenha(atual, nova, confirmacao) },
            onDismiss = {
                showTrocarSenha = false
                viewModel.limparTrocaSenha()
            }
        )
    }
}

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, BlueSjc) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (selected) BlueSjc else TextGray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.titulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.descricao,
                    fontSize = 13.sp,
                    color = TextGray
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BlueSjc)
            )
        }
    }
}

/** Diálogo de troca de senha: senha atual + nova + confirmação (v7, Item 2). */
@Composable
private fun TrocarSenhaDialog(
    state: TrocaSenhaUiState,
    onConfirm: (atual: String, nova: String, confirmacao: String) -> Unit,
    onDismiss: () -> Unit
) {
    var atual by remember { mutableStateOf("") }
    var nova by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.carregando) onDismiss() },
        title = { Text("Trocar senha", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = atual,
                    onValueChange = { atual = it },
                    label = { Text("Senha atual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nova,
                    onValueChange = { nova = it },
                    label = { Text("Nova senha (mín. 6)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmacao,
                    onValueChange = { confirmacao = it },
                    label = { Text("Confirmar nova senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.erro != null) {
                    Text(
                        text = state.erro,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(atual, nova, confirmacao) },
                enabled = !state.carregando
            ) {
                if (state.carregando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Salvar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.carregando) {
                Text("Cancelar")
            }
        }
    )
}
