package com.example.ui.components

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-Screen Cinematic Character Background Animation:
 * Conforms directly to the user prompt:
 * "تكون الخلفيه كامله ويكون قليله الوضوح أو مش فارقه المهم تكون هي الخلفيه وعليها النصوص واللوجاهات ودهانات التسجيل والخ"
 *
 * 1. Starts off-screen to the right, walks confidently towards center with realistic walking gait.
 * 2. Black tech cloak moves with physics/cloth flow, catching blue neon light reflections.
 * 3. Reaches center, halts in heroic stance, smoothly rotates body and head towards viewer.
 * 4. Lifts head slightly, locks gaze onto viewer, electric cyan eyes burst with power surge & radial shockwave.
 * 5. Volumetric blue ambient lighting & headphones LEDs glow, settling into a majestic idle state.
 */
@Composable
fun CinematicBackgroundMascot(
    modifier: Modifier = Modifier,
    isFocusedOnInput: Boolean = false,
    eyeGlowIntensity: Float = 1.0f
) {
    // Phase 1: Entrance walking from right
    val walkEntranceX = remember { Animatable(380f) } // Starts off-screen right
    val walkStepBob = remember { Animatable(0f) }
    val cloakClothSway = remember { Animatable(16f) }

    // Phase 2 & 3: Turn body and head towards camera
    val bodyRotationY = remember { Animatable(-55f) } // Turned to profile initially
    val headLagTilt = remember { Animatable(4.0f) }
    val headLift = remember { Animatable(7f) } // Head down initially, lifts up to lock eyes

    // Phase 4: Shockwave burst & eye ignition surge
    val eyeIgnitionBurst = remember { Animatable(0f) }
    val shockwaveScale = remember { Animatable(0.1f) }
    val shockwaveAlpha = remember { Animatable(0f) }
    val lightRayAlpha = remember { Animatable(0f) }

    // Sequence Choreographer
    LaunchedEffect(Unit) {
        // Step 1: Walk in from the right (0 -> 1600ms)
        launch {
            walkEntranceX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
            )
        }

        // Realistic walking gait steps & cloak momentum
        launch {
            for (i in 0 until 4) {
                walkStepBob.animateTo(-9f, tween(200, easing = FastOutSlowInEasing))
                walkStepBob.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            }
            walkStepBob.animateTo(0f, tween(150))
        }

        launch {
            // Cloak sway during walk, settling down smoothly upon stopping
            cloakClothSway.animateTo(-12f, tween(400))
            cloakClothSway.animateTo(14f, tween(400))
            cloakClothSway.animateTo(-8f, tween(400))
            cloakClothSway.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }

        delay(1650) // Now at center in heroic stance

        // Step 3: Turn body towards viewer smoothly
        launch {
            bodyRotationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }

        // Head turns naturally slightly trailing the body
        launch {
            delay(150)
            headLagTilt.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
        }

        delay(1150) // Facing viewer directly

        // Step 4: Lift head slightly and lock gaze into camera
        launch {
            headLift.animateTo(
                targetValue = -3.5f, // Confident chin lift
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        delay(400) // Eyes lock!

        // Step 5: Ignition! Electric cyan eyes burst with radial shockwave
        launch {
            shockwaveAlpha.snapTo(0.95f)
            lightRayAlpha.snapTo(0.95f)
            launch {
                shockwaveScale.animateTo(2.2f, tween(850, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                shockwaveAlpha.animateTo(0f, tween(700, easing = LinearEasing))
                lightRayAlpha.animateTo(0f, tween(750, easing = LinearEasing))
            }
        }

        // Eye energy surge: flash to maximum then settle into intense burning glow
        eyeIgnitionBurst.animateTo(1.9f, tween(350, easing = FastOutSlowInEasing))
        eyeIgnitionBurst.animateTo(0.45f, tween(600, easing = FastOutSlowInEasing))
    }

    // Phase 6: Continuous Majestic Idle Physics
    val infiniteTransition = rememberInfiniteTransition(label = "bg_heroic_loop")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroic_breath"
    )

    val heroicFloat by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroic_float"
    )

    val auraGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotate"
    )

    val eyePlasmaPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eye_plasma"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Deep futuristic cyber dark base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF060910),
                            Color(0xFF090E17),
                            Color(0xFF04060C)
                        )
                    )
                )
        )

        // Full-Screen High-Tech Cyber Circuit Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.38f) // Upper-center focus for hero
            val baseRadius = size.width * 0.44f

            // Shockwave expanding ring (ignites across the screen upon eye contact)
            if (shockwaveAlpha.value > 0.01f) {
                drawCircle(
                    color = NeonCyan.copy(alpha = shockwaveAlpha.value),
                    radius = baseRadius * shockwaveScale.value,
                    center = center,
                    style = Stroke(width = (4f * (1f - shockwaveScale.value * 0.35f)).coerceAtLeast(1.5f).dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = shockwaveAlpha.value * 0.45f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * shockwaveScale.value * 1.3f
                    ),
                    radius = baseRadius * shockwaveScale.value * 1.3f,
                    center = center
                )
            }

            // Radial Light Flare Rays (during ignition burst)
            if (lightRayAlpha.value > 0.01f) {
                val rayCount = 14
                for (i in 0 until rayCount) {
                    val angle = Math.toRadians((i * (360.0 / rayCount)).toDouble())
                    val rayEnd = Offset(
                        center.x + (size.width * 0.85f) * cos(angle).toFloat(),
                        center.y + (size.width * 0.85f) * sin(angle).toFloat()
                    )
                    drawLine(
                        color = NeonCyan.copy(alpha = lightRayAlpha.value * 0.40f),
                        start = center,
                        end = rayEnd,
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Continuous Volumetric Ambient Aura Bloom
            val totalGlow = (auraGlowPulse * eyeGlowIntensity + eyeIgnitionBurst.value).coerceAtMost(2.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = (0.35f * totalGlow).coerceAtMost(0.65f)),
                        NeonPurple.copy(alpha = 0.20f * auraGlowPulse),
                        Color(0xFF0284C7).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // Outer Dashed High-Tech Circuit Ring
            drawCircle(
                color = NeonCyan.copy(alpha = 0.25f),
                radius = baseRadius * 1.05f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Orbiting cyber energy nodes
            val segments = 10
            for (i in 0 until segments) {
                val angle = Math.toRadians((i * (360.0 / segments) + ringRotation).toDouble())
                val nodeX = center.x + (baseRadius * 1.05f) * cos(angle).toFloat()
                val nodeY = center.y + (baseRadius * 1.05f) * sin(angle).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) NeonCyan.copy(alpha = 0.80f) else NeonPurple.copy(alpha = 0.65f),
                    radius = if (i % 2 == 0) 3.5.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }

            // Full-screen electronic grid and horizontal circuit traces
            val lineY1 = center.y - 120.dp.toPx()
            val lineY2 = center.y + 110.dp.toPx()
            drawLine(
                color = NeonCyan.copy(alpha = 0.30f),
                start = Offset(0f, lineY1),
                end = Offset(size.width * 0.32f, lineY1),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.30f),
                start = Offset(size.width * 0.68f, lineY1),
                end = Offset(size.width, lineY1),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.20f),
                start = Offset(0f, lineY2),
                end = Offset(size.width * 0.25f, lineY2),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.20f),
                start = Offset(size.width * 0.75f, lineY2),
                end = Offset(size.width, lineY2),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Animated Character Figure in Background (Heroic Stance & Walking Entry)
        val combinedY = walkStepBob.value + heroicFloat
        val currentScale = if (isFocusedOnInput) breathScale * 1.04f else breathScale

        Box(
            modifier = Modifier
                .size(310.dp)
                .offset(
                    x = walkEntranceX.value.dp,
                    y = (-65).dp + combinedY.dp // Positioned in upper half as magnificent background
                )
                .scale(currentScale)
                .rotate(cloakClothSway.value * 0.28f + headLagTilt.value)
                .graphicsLayer {
                    rotationY = bodyRotationY.value
                    rotationX = headLift.value
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center
        ) {
            // Character Hero Asset
            Image(
                painter = painterResource(id = R.drawable.ic_sprix_cinematic_hero),
                contentDescription = "Sprix Solver Mascot Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
            )

            // Radial vignette blending edges into the dark cyber background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x66080B12),
                                Color(0xDD080B12)
                            )
                        )
                    )
            )

            // Electric Cyan Glowing Eyes Overlay & Headphone Lighting
            val eyeGlowFactor = ((0.52f * eyePlasmaPulse + eyeIgnitionBurst.value * 0.65f) * eyeGlowIntensity).coerceAtMost(1.3f)

            // Face and eyes glowing flare
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .offset(y = (-44).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                NeonCyan.copy(alpha = eyeGlowFactor.coerceAtMost(0.95f)),
                                NeonCyan.copy(alpha = (eyeGlowFactor * 0.45f).coerceAtMost(0.6f)),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Headphone cyan neon accents
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = (-44).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = (0.30f * eyeGlowFactor).coerceAtMost(0.55f)),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Dark Semi-Transparent Vignette Overlay:
        // Allows the background character animation to shine through while keeping texts & login inputs crisp and clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55060910), // Light tint at top to see the character clearly
                            Color(0x99080D18), // Medium tint
                            Color(0xEE04060C)  // Darker tint at bottom for pristine input field readability
                        )
                    )
                )
        )
    }
}

/**
 * Bounded component variant of the animated mascot for embedded panels.
 */
@Composable
fun AnimatedMascot(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    isFocusedOnInput: Boolean = false,
    eyeGlowIntensity: Float = 1.0f
) {
    val walkEntranceX = remember { Animatable(320f) }
    val walkStepBob = remember { Animatable(0f) }
    val cloakClothSway = remember { Animatable(14f) }

    val bodyRotationY = remember { Animatable(-52f) }
    val headLagTilt = remember { Animatable(3.5f) }
    val headLift = remember { Animatable(6f) }

    val eyeIgnitionBurst = remember { Animatable(0f) }
    val shockwaveScale = remember { Animatable(0.1f) }
    val shockwaveAlpha = remember { Animatable(0f) }
    val lightRayAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            walkEntranceX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
            )
        }

        launch {
            for (i in 0 until 4) {
                walkStepBob.animateTo(-8f, tween(200, easing = FastOutSlowInEasing))
                walkStepBob.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            }
            walkStepBob.animateTo(0f, tween(150))
        }

        launch {
            cloakClothSway.animateTo(-10f, tween(400))
            cloakClothSway.animateTo(12f, tween(400))
            cloakClothSway.animateTo(-8f, tween(400))
            cloakClothSway.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }

        delay(1650)

        launch {
            bodyRotationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        }

        launch {
            delay(150)
            headLagTilt.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
            )
        }

        delay(1150)

        launch {
            headLift.animateTo(
                targetValue = -3f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        delay(400)

        launch {
            shockwaveAlpha.snapTo(0.9f)
            lightRayAlpha.snapTo(0.95f)
            launch {
                shockwaveScale.animateTo(1.6f, tween(750, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                shockwaveAlpha.animateTo(0f, tween(600, easing = LinearEasing))
                lightRayAlpha.animateTo(0f, tween(700, easing = LinearEasing))
            }
        }

        eyeIgnitionBurst.animateTo(1.8f, tween(350, easing = FastOutSlowInEasing))
        eyeIgnitionBurst.animateTo(0.4f, tween(600, easing = FastOutSlowInEasing))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "idle_heroic_loop")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroic_breath"
    )

    val heroicFloat by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroic_float"
    )

    val auraGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotate"
    )

    val eyePlasmaPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eye_plasma"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.width / 2f) * 0.94f

            if (shockwaveAlpha.value > 0.01f) {
                drawCircle(
                    color = NeonCyan.copy(alpha = shockwaveAlpha.value),
                    radius = baseRadius * shockwaveScale.value,
                    center = center,
                    style = Stroke(width = (4f * (1f - shockwaveScale.value * 0.4f)).coerceAtLeast(1.5f).dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = shockwaveAlpha.value * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * shockwaveScale.value * 1.2f
                    ),
                    radius = baseRadius * shockwaveScale.value * 1.2f,
                    center = center
                )
            }

            if (lightRayAlpha.value > 0.01f) {
                val rayCount = 12
                for (i in 0 until rayCount) {
                    val angle = Math.toRadians((i * (360.0 / rayCount)).toDouble())
                    val rayEnd = Offset(
                        center.x + (baseRadius * 1.3f) * cos(angle).toFloat(),
                        center.y + (baseRadius * 1.3f) * sin(angle).toFloat()
                    )
                    drawLine(
                        color = NeonCyan.copy(alpha = lightRayAlpha.value * 0.45f),
                        start = center,
                        end = rayEnd,
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            val totalGlow = (auraGlowPulse * eyeGlowIntensity + eyeIgnitionBurst.value).coerceAtMost(2.6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = (0.38f * totalGlow).coerceAtMost(0.75f)),
                        NeonPurple.copy(alpha = 0.22f * auraGlowPulse),
                        Color(0xFF0284C7).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.4f
                ),
                radius = baseRadius * 1.4f,
                center = center
            )

            drawCircle(
                color = NeonCyan.copy(alpha = 0.28f),
                radius = baseRadius * 0.90f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            val segments = 8
            for (i in 0 until segments) {
                val angle = Math.toRadians((i * (360.0 / segments) + ringRotation).toDouble())
                val nodeX = center.x + (baseRadius * 0.90f) * cos(angle).toFloat()
                val nodeY = center.y + (baseRadius * 0.90f) * sin(angle).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) NeonCyan.copy(alpha = 0.85f) else NeonPurple.copy(alpha = 0.70f),
                    radius = if (i % 2 == 0) 3.5.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }

            val lineY1 = center.y - 34.dp.toPx()
            val lineY2 = center.y + 44.dp.toPx()
            drawLine(
                color = NeonCyan.copy(alpha = 0.35f),
                start = Offset(0f, lineY1),
                end = Offset(this.size.width * 0.30f, lineY1),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.35f),
                start = Offset(this.size.width * 0.70f, lineY1),
                end = Offset(this.size.width, lineY1),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.22f),
                start = Offset(0f, lineY2),
                end = Offset(this.size.width * 0.22f, lineY2),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = NeonCyan.copy(alpha = 0.22f),
                start = Offset(this.size.width * 0.78f, lineY2),
                end = Offset(this.size.width, lineY2),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        val combinedY = walkStepBob.value + heroicFloat
        val currentScale = if (isFocusedOnInput) breathScale * 1.05f else breathScale

        Box(
            modifier = Modifier
                .size(size * 0.90f)
                .offset(
                    x = walkEntranceX.value.dp,
                    y = combinedY.dp
                )
                .scale(currentScale)
                .rotate(cloakClothSway.value * 0.3f + headLagTilt.value)
                .graphicsLayer {
                    rotationY = bodyRotationY.value
                    rotationX = headLift.value
                    cameraDistance = 16f * density
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sprix_cinematic_hero),
                contentDescription = "Sprix Solver Cinematic Character",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x66080B12),
                                Color(0xDD080B12)
                            )
                        )
                    )
            )

            val eyeGlowFactor = ((0.50f * eyePlasmaPulse + eyeIgnitionBurst.value * 0.6f) * eyeGlowIntensity).coerceAtMost(1.2f)

            Box(
                modifier = Modifier
                    .size(size * 0.36f)
                    .offset(y = (-32).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                NeonCyan.copy(alpha = eyeGlowFactor.coerceAtMost(0.95f)),
                                NeonCyan.copy(alpha = (eyeGlowFactor * 0.45f).coerceAtMost(0.6f)),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(size * 0.48f)
                    .offset(y = (-32).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = (0.28f * eyeGlowFactor).coerceAtMost(0.5f)),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
