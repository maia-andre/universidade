package com.sgaf.universidadedoservidor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sgaf.universidadedoservidor.ui.theme.GoldSjc
import com.sgaf.universidadedoservidor.ui.theme.DarkBackground
import com.sgaf.universidadedoservidor.ui.theme.LightBackground

@Composable
fun SjcGearSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    animate: Boolean = true,
    backgroundColor: Color = if (isSystemInDarkTheme()) DarkBackground else LightBackground
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gearRotation")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    // Redução de movimento (acessibilidade v4): engrenagem estática.
    val rotationAngle = if (animate) animatedAngle else 0f

    Canvas(modifier = modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val centerX = width / 2
        val centerY = height / 2
        val scale = width / 100f // Base scale relative to 100x100 viewBox

        // 1. OUTER ROTATING GEAR
        rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
            // Gear teeth (4 rounded rectangles)
            val rectWidth = 18f * scale
            val rectHeight = 92f * scale
            val rx = 2f * scale
            
            for (angle in listOf(0f, 45f, 90f, 135f)) {
                rotate(degrees = angle, pivot = Offset(centerX, centerY)) {
                    drawRoundRect(
                        color = GoldSjc,
                        topLeft = Offset(centerX - rectWidth / 2, centerY - rectHeight / 2),
                        size = Size(rectWidth, rectHeight),
                        cornerRadius = CornerRadius(rx, rx)
                    )
                }
            }
            
            // Outer ring
            drawCircle(
                color = GoldSjc,
                radius = 37f * scale,
                center = Offset(centerX, centerY)
            )
        }

        // 2. INNER HOLE CUTOUT (To match local background)
        drawCircle(
            color = backgroundColor,
            radius = 25f * scale,
            center = Offset(centerX, centerY)
        )

        // 3. INNER STATIC EMBLEM
        // Blue circle background
        drawCircle(
            color = Color(0xFF003882),
            radius = 23f * scale,
            center = Offset(centerX, centerY)
        )

        // Wavy gold line (Paraíba do Sul river)
        val riverPath = Path().apply {
            val startX = centerX + (27f - 50f) * scale
            val startY = centerY + (50f - 50f) * scale
            val q1x = centerX + (38f - 50f) * scale
            val q1y = centerY + (42f - 50f) * scale
            val midX = centerX + (50f - 50f) * scale
            val midY = centerY + (50f - 50f) * scale
            
            moveTo(startX, startY)
            quadraticTo(q1x, q1y, midX, midY)
            
            val q2x = centerX + (62f - 50f) * scale
            val q2y = centerY + (58f - 50f) * scale
            val endX = centerX + (73f - 50f) * scale
            val endY = centerY + (50f - 50f) * scale
            quadraticTo(q2x, q2y, endX, endY)
        }
        drawPath(
            path = riverPath,
            color = GoldSjc,
            style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
        )

        // 3 Gold Stars
        val starPath = Path().apply {
            addStar(centerX, centerY - 14f * scale, 1.1f * scale)
            addStar(centerX - 12f * scale, centerY + 12f * scale, 1.1f * scale)
            addStar(centerX + 12f * scale, centerY + 12f * scale, 1.1f * scale)
        }
        drawPath(path = starPath, color = GoldSjc)
    }
}

// Helper to generate a 5-point star path
private fun Path.addStar(cx: Float, cy: Float, scale: Float) {
    val points = listOf(
        0f to -4f,
        1.1f to -1.2f,
        4f to -1.2f,
        1.8f to 0.5f,
        2.5f to 3.5f,
        0f to 1.8f,
        -2.5f to 3.5f,
        -1.8f to 0.5f,
        -4f to -1.2f,
        -1.1f to -1.2f
    )
    moveTo(cx + points[0].first * scale, cy + points[0].second * scale)
    for (i in 1 until points.size) {
        lineTo(cx + points[i].first * scale, cy + points[i].second * scale)
    }
    close()
}

@Composable
fun GraduationCapIcon(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    capColor: Color = Color(0xFF1E1E1E), // Chapéu quase preto
    tasselColor: Color = GoldSjc         // Cordinha dourada
) {
    Canvas(modifier = modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        
        // Draw the top diamond of the cap
        val diamondPath = Path().apply {
            moveTo(w / 2f, h * 0.2f)       // Top corner
            lineTo(w * 0.9f, h * 0.4f)     // Right corner
            lineTo(w / 2f, h * 0.6f)       // Bottom corner
            lineTo(w * 0.1f, h * 0.4f)     // Left corner
            close()
        }
        drawPath(path = diamondPath, color = capColor)
        
        // Draw the cap base (the head part)
        val basePath = Path().apply {
            moveTo(w * 0.3f, h * 0.51f)
            quadraticTo(w / 2f, h * 0.58f, w * 0.7f, h * 0.51f)
            lineTo(w * 0.7f, h * 0.68f)
            quadraticTo(w / 2f, h * 0.78f, w * 0.3f, h * 0.68f)
            close()
        }
        drawPath(path = basePath, color = capColor)
        
        // Draw the tassel hanging on the right
        val tasselPath = Path().apply {
            moveTo(w / 2f, h * 0.4f)       // Center top
            lineTo(w * 0.82f, h * 0.45f)   // Over to side
            lineTo(w * 0.85f, h * 0.7f)    // Hanging down
        }
        drawPath(
            path = tasselPath,
            color = tasselColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Tassel fringe/brush
        drawCircle(
            color = tasselColor,
            radius = 3.dp.toPx(),
            center = Offset(w * 0.85f, h * 0.74f)
        )
    }
}

@Composable
fun UniversidadeLogo(
    modifier: Modifier = Modifier,
    gearSize: Dp = 38.dp,
    scale: Float = 1f,
    animate: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    backgroundColor: Color = if (isSystemInDarkTheme()) DarkBackground else LightBackground
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Capelo Logo
        GraduationCapIcon(
            modifier = Modifier.padding(bottom = (8 * scale).dp),
            size = (54 * scale).dp,
            capColor = Color(0xFF1A1A1A),
            tasselColor = GoldSjc
        )
        
        // Brand Subtitle
        Text(
            text = "UNIVERSIDADE DO",
            color = GoldSjc,
            fontSize = (11 * scale).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            fontFamily = FontFamily.SansSerif
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Brand Title: SERVID[O]R
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SERVID",
                color = textColor,
                fontSize = (28 * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.SansSerif
            )
            
            SjcGearSpinner(
                modifier = Modifier
                    .size(gearSize)
                    .padding(horizontal = 2.dp),
                size = gearSize,
                animate = animate,
                backgroundColor = backgroundColor
            )
            
            Text(
                text = "R",
                color = textColor,
                fontSize = (28 * scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
