package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Opening Launch Screen:
 * Displays the circular logo during app opening with smooth high-tech orbital pulse,
 * satisfying the requirement: "خلي لوجو التطبيق برا وأثناء الفتح يكون دائري مش مربع"
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val glowPulse = remember { Animatable(0.2f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(500, easing = LinearEasing))
        }
        launch {
            logoScale.animateTo(1.05f, tween(600, easing = FastOutSlowInEasing))
            logoScale.animateTo(1.0f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            glowPulse.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }

        // Display opening animation, then transition to main app
        delay(1400)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_spin")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .clickable { onSplashFinished() }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Deep cyber backdrop gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F1E2A),
                            Color(0xFF080D17),
                            Color(0xFF04060A)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circular Logo with High-Tech Orbital Rings
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(logoScale.value * breathingScale),
                contentAlignment = Alignment.Center
            ) {
                // High-tech orbital canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = (size.width / 2f) * 0.92f

                    // Radial glow bloom behind circular logo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.40f * glowPulse.value),
                                NeonPurple.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = r * 1.3f
                        ),
                        radius = r * 1.3f,
                        center = center
                    )

                    // Outer orbit dashed ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.45f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 4 Orbiting nodes
                    for (i in 0 until 4) {
                        val angle = Math.toRadians((i * 90.0 + orbitAngle).toDouble())
                        val nx = center.x + r * cos(angle).toFloat()
                        val ny = center.y + r * sin(angle).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) NeonCyan else Color(0xFFFBBF24),
                            radius = 3.5.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }
                }

                // Strictly Circular App Logo
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0F1A))
                        .border(2.5.dp, Brush.sweepGradient(listOf(NeonCyan, Color(0xFF38BDF8), NeonPurple, NeonCyan)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_outer_logo_circle),
                        contentDescription = "Sprix Solver Circular Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name
            Text(
                text = "Sprix Solver",
                color = Color(0xFFE2E8F0),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "منصة الحلول التعليمية الذكية",
                color = Color(0xFF34D399),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
