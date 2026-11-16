package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.LiveConnectionState
import com.example.data.model.PendingCommandConfirmation
import com.example.ui.theme.XavionAmber
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCobalt
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionObsidian
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionPanelGlass
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.theme.XavionViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float
)

@Composable
fun HolographicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "hud_bg")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_drift"
    )

    val particles = remember {
        val rng = Random(42)
        List(28) {
            ParticleSeed(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radius = rng.nextFloat() * 2.2f + 1.1f,
                speed = rng.nextFloat() * 0.6f + 0.4f,
                phase = rng.nextFloat() * (2 * PI).toFloat()
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(XavionBlack, XavionObsidian, Color(0xFF030812))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Soft central cyan radial core ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        XavionCyan.copy(alpha = 0.08f),
                        XavionCobalt.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.maxDimension * 0.55f
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * 0.5f, size.height * 0.42f)
            )

            // Subtle holographic coordinate grid
            val step = 72.dp.toPx()
            val gridColor = XavionCyan.copy(alpha = 0.035f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }

            // Floating HUD particles
            particles.forEach { p ->
                val px = (p.xFrac * size.width + cos(drift * p.speed + p.phase) * 18f)
                    .coerceIn(0f, size.width)
                val py = (p.yFrac * size.height + sin(drift * p.speed + p.phase) * 24f)
                    .coerceIn(0f, size.height)
                drawCircle(
                    color = XavionCyan.copy(alpha = 0.24f),
                    radius = p.radius.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
        content()
    }
}

@Composable
fun HoloPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = XavionBorderSubtle,
    cornerRadius: Dp = 18.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = XavionPanelGlass,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun HoloSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(XavionCyan)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = XavionCyan
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = XavionTextMuted
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ConnectionStatusPill(
    state: LiveConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (state) {
        LiveConnectionState.Disconnected -> "STANDBY" to XavionTextMuted
        LiveConnectionState.Connecting -> "LINKING..." to XavionAmber
        LiveConnectionState.ConnectedIdle -> "LIVE READY" to XavionEmerald
        LiveConnectionState.Listening -> "LISTENING" to XavionCyan
        LiveConnectionState.Thinking -> "PROCESSING" to XavionViolet
        LiveConnectionState.Speaking -> "SPEAKING" to XavionCobalt
        is LiveConnectionState.Reconnecting -> "RECONNECT #${state.attempt}" to XavionAmber
        is LiveConnectionState.Error -> (if (state.isQuotaError) "QUOTA LIMIT" else "LINK ERROR") to XavionCrimson
    }
    val animatedColor by animateColorAsState(targetValue = color, label = "status_color")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(XavionPanelDark.copy(alpha = 0.9f))
            .border(1.dp, animatedColor.copy(alpha = 0.65f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = animatedColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AudioWaveformBar(
    inputAmplitude: Float,
    outputAmplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "wave_phase"
    )
    val energy = maxOf(inputAmplitude, outputAmplitude)
    val activeColor = if (outputAmplitude > inputAmplitude) XavionCobalt else XavionCyan

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val bars = 32
        val gap = size.width / bars
        val barWidth = (gap * 0.52f).coerceAtLeast(3f)
        val centerY = size.height / 2f

        for (i in 0 until bars) {
            val normPos = i / bars.toFloat()
            val envelope = sin(normPos * PI).toFloat()
            val wave = if (isActive) {
                (0.18f + energy * 0.82f) * (0.55f + 0.45f * sin(phase + i * 0.45f)) * envelope
            } else {
                0.08f * envelope
            }
            val barH = (wave * size.height).coerceIn(4f, size.height)
            val left = i * gap + (gap - barWidth) / 2f
            drawLine(
                color = if (isActive) activeColor.copy(alpha = 0.85f) else XavionTextMuted.copy(alpha = 0.35f),
                start = Offset(left + barWidth / 2f, centerY - barH / 2f),
                end = Offset(left + barWidth / 2f, centerY + barH / 2f),
                strokeWidth = barWidth
            )
        }
    }
}

@Composable
fun CommandConfirmationDialog(
    confirmation: PendingCommandConfirmation,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        containerColor = XavionPanelDark,
        titleContentColor = XavionTextPrimary,
        textContentColor = XavionTextSecondary,
        icon = {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Action authorization",
                tint = XavionAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "AUTHORIZE DEVICE ACTION",
                style = MaterialTheme.typography.titleLarge,
                color = XavionCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "XAVION requests permission to execute:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    color = XavionBlack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, XavionBorderBright)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = confirmation.summary,
                            style = MaterialTheme.typography.titleMedium,
                            color = XavionTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Function: ${confirmation.functionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = XavionCyan
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                modifier = Modifier.testTag("confirm_command_approve"),
                colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
            ) {
                Text("Authorize")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDeny,
                modifier = Modifier.testTag("confirm_command_deny"),
                border = BorderStroke(1.dp, XavionCrimson)
            ) {
                Text("Decline", color = XavionCrimson)
            }
        }
    )
}
