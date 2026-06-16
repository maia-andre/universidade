package com.sgaf.universidadedoservidor.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.ui.theme.GoldSjc
import com.sgaf.universidadedoservidor.ui.theme.TextGray

/** Indicador de carregamento padrão do app (identidade SJC). */
@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GoldSjc)
    }
}

/** Estado vazio/erro reutilizável, com ícone, título e mensagem opcional. */
@Composable
fun EmptyMessage(
    icon: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    descricao: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            descricao?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
