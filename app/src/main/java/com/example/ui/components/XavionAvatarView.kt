package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AvatarMode
import com.example.data.model.LiveConnectionState
import com.example.ui.theme.XavionAmber
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionCobalt
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionCyanSoft
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.theme.XavionViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun XavionAvatarView(
    avatarMode: AvatarMode,
    connectionState: LiveConnectionState,
    inputAmplitude: Float,
    outputAmplitude: Float,
    size: Dp = 240.dp,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeColorTarget = when (connectionState) {
        LiveConnectionState.Listening -> XavionCyan
        LiveConnectionState.Speaking -> XavionCyanSoft
        LiveConnectionState.Thinking -> XavionViolet
        LiveConnectionState.ConnectedIdle -> XavionEmerald
        LiveConnectionState.Connecting, is LiveConnectionState.Reconnecting -> XavionAmber
        is LiveConnectionState.Error -> XavionCrimson
        LiveConnectionState.Disconnected -> XavionCobalt
    }
    val accentColor by animateColorAsState(targetValue = activeColorTarget, label = "avatar_accent")
    val liveEnergy by animateFloatAsState(
        targetValue = maxOf(inputAmplitude, outputAmplitude).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 500f),
        label = "avatar_energy"
    )

    val transition = rememberInfiniteTransition(label = "avatar_anim")
    val outerAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (connectionState == LiveConnectionState.Thinking) 3200 else 12000,
                easing = LinearEasing
            )
        ),
        label = "outer_rot"
    )
    val innerAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing)
        ),
        label = "inner_rot"
    )
    val scanLineProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("xavion_avatar_stage")
            .then(
                if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = avatarMode, label = "avatar_mode_crossfade") { mode ->
            when (mode) {
                AvatarMode.MALE_VIRTUAL_AVATAR -> {
                    MaleHolographicAvatar(
                        accentColor = accentColor,
                        liveEnergy = liveEnergy,
                        outputAmplitude = outputAmplitude,
                        outerAngle = outerAngle,
                        innerAngle = innerAngle,
                        scanLineProgress = scanLineProgress,
                        breathe = breathe,
                        isThinking = connectionState == LiveConnectionState.Thinking,
                        isSpeaking = connectionState == LiveConnectionState.Speaking
                    )
                }
                AvatarMode.HOLOGRAPHIC_CORE -> {
                    HolographicEnergyCore(
                        accentColor = accentColor,
                        liveEnergy = liveEnergy,
                        outerAngle = outerAngle,
                        innerAngle = innerAngle,
                        breathe = breathe
                    )
                }
                AvatarMode.MINIMAL_VOICE -> {
                    MinimalVoiceOrb(
                        accentColor = accentColor,
                        liveEnergy = liveEnergy,
                        breathe = breathe,
                        state = connectionState
                    )
                }
            }
        }
    }
}

@Composable
private fun MaleHolographicAvatar(
    accentColor: Color,
    liveEnergy: Float,
    outputAmplitude: Float,
    outerAngle: Float,
    innerAngle: Float,
    scanLineProgress: Float,
    breathe: Float,
    isThinking: Boolean,
    isSpeaking: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating HUD telemetry rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Ambient radial halo synced with speech/listening amplitude
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.24f + liveEnergy * 0.28f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Segmented rotating arc ring
            val ringRadius = radius * 0.92f
            drawArc(
                color = accentColor.copy(alpha = 0.85f),
                startAngle = outerAngle,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor.copy(alpha = 0.55f),
                startAngle = outerAngle + 180f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            val innerR = radius * 0.84f
            drawArc(
                color = XavionCobalt.copy(alpha = 0.7f),
                startAngle = innerAngle,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - innerR, center.y - innerR),
                size = Size(innerR * 2, innerR * 2),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Portrait container with holographic eye & speech modulation overlay
        Box(
            modifier = Modifier
                .fillMaxSize(0.76f)
                .graphicsLayer {
                    val scale = if (isSpeaking) 1f + liveEnergy * 0.035f else breathe
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(XavionBlack)
                .border(2.dp, accentColor.copy(alpha = 0.75f), CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_xavion_avatar),
                contentDescription = stringResource(R.string.cd_xavion_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Holographic scanline, blue/cyan tint glaze, eye shimmer & vocal modulator
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Subtle cyan holographic vignette
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.14f),
                            XavionBlack.copy(alpha = 0.55f)
                        )
                    )
                )

                // Vertical scanline pass
                val scanY = scanLineProgress * size.height
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    start = Offset(0f, scanY),
                    end = Offset(size.width, scanY),
                    strokeWidth = 2.dp.toPx()
                )

                // Subtle illuminated holographic eye markers aligned over portrait face
                val eyeY = size.height * 0.415f
                val leftEyeX = size.width * 0.425f
                val rightEyeX = size.width * 0.575f
                val eyeAlpha = if (isThinking) 0.55f else 0.25f + liveEnergy * 0.4f
                drawCircle(
                    color = XavionCyan.copy(alpha = eyeAlpha),
                    radius = 4.dp.toPx(),
                    center = Offset(leftEyeX, eyeY)
                )
                drawCircle(
                    color = XavionCyan.copy(alpha = eyeAlpha),
                    radius = 4.dp.toPx(),
                    center = Offset(rightEyeX, eyeY)
                )

                // Audio-synchronized vocal waveform arc across lower jaw / collar
                val mouthY = size.height * 0.84f
                val barCount = 13
                val totalW = size.width * 0.44f
                val startX = (size.width - totalW) / 2f
                val step = totalW / barCount
                for (i in 0 until barCount) {
                    val norm = 1f - kotlin.math.abs(i - barCount / 2f) / (barCount / 2f + 1f)
                    val amp = (0.12f + outputAmplitude * 0.95f) * norm
                    val h = (amp * 22.dp.toPx()).coerceAtLeast(2.dp.toPx())
                    val x = startX + i * step + step / 2f
                    drawLine(
                        color = accentColor.copy(alpha = if (isSpeaking) 0.95f else 0.4f),
                        start = Offset(x, mouthY - h / 2f),
                        end = Offset(x, mouthY + h / 2f),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun HolographicEnergyCore(
    accentColor: Color,
    liveEnergy: Float,
    outerAngle: Float,
    innerAngle: Float,
    breathe: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = breathe + liveEnergy * 0.06f
                scaleX = s
                scaleY = s
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f

        // Outer plasma glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.35f + liveEnergy * 0.35f),
                    XavionCobalt.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = maxR
            ),
            radius = maxR,
            center = center
        )

        // Degree ticks ring
        val tickOuter = maxR * 0.93f
        val tickInner = maxR * 0.87f
        for (deg in 0 until 360 step 10) {
            val rad = (deg + outerAngle * 0.35f) * PI / 180.0
            val lenScale = if (deg % 30 == 0) 1.0f else 0.5f
            val r1 = tickInner + (tickOuter - tickInner) * (1f - lenScale)
            drawLine(
                color = accentColor.copy(alpha = if (deg % 30 == 0) 0.85f else 0.35f),
                start = Offset(center.x + (r1 * cos(rad)).toFloat(), center.y + (r1 * sin(rad)).toFloat()),
                end = Offset(center.x + (tickOuter * cos(rad)).toFloat(), center.y + (tickOuter * sin(rad)).toFloat()),
                strokeWidth = if (deg % 30 == 0) 2.5.dp.toPx() else 1.2.dp.toPx()
            )
        }

        // Rotating segmented rings
        val r2 = maxR * 0.76f
        for (i in 0 until 3) {
            drawArc(
                color = accentColor.copy(alpha = 0.9f),
                startAngle = outerAngle + i * 120f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(center.x - r2, center.y - r2),
                size = Size(r2 * 2, r2 * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val r3 = maxR * 0.60f
        for (i in 0 until 4) {
            drawArc(
                color = XavionCobalt.copy(alpha = 0.85f),
                startAngle = innerAngle + i * 90f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = Offset(center.x - r3, center.y - r3),
                size = Size(r3 * 2, r3 * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner energy nucleus pulsing with audio amplitude
        val coreR = maxR * (0.28f + liveEnergy * 0.14f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, accentColor, XavionCobalt),
                center = center,
                radius = coreR
            ),
            radius = coreR,
            center = center
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.8f),
            radius = maxR * 0.42f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun MinimalVoiceOrb(
    accentColor: Color,
    liveEnergy: Float,
    breathe: Float,
    state: LiveConnectionState
) {
    Box(
        modifier = Modifier
            .fillMaxSize(0.82f)
            .clip(CircleShape)
            .background(XavionPanelDark.copy(alpha = 0.8f))
            .border(1.5.dp, accentColor.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f + liveEnergy * 0.35f), Color.Transparent),
                    center = center,
                    radius = r * breathe
                ),
                radius = r * breathe,
                center = center
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "XAVION",
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (state) {
                    LiveConnectionState.Listening -> "Listening..."
                    LiveConnectionState.Speaking -> "Speaking..."
                    LiveConnectionState.Thinking -> "Thinking..."
                    LiveConnectionState.ConnectedIdle -> "Connected"
                    is LiveConnectionState.Error -> "Check Link"
                    else -> "Minimal Mode"
                },
                style = MaterialTheme.typography.labelMedium,
                color = XavionTextSecondary
            )
        }
    }
}
