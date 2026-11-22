package com.minimalphone.core.ui.components

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.PauseStyle
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MindfulPauseScreen(
    packageName: String,
    appLabel: String,
    config: PauseConfig,
    delaySeconds: Int,
    isReintervention: Boolean = false,
    onLeave: () -> Unit,
    onContinue: (reflectionReason: String?) -> Unit
) {
    BackHandler {
        onLeave()
    }

    val context = LocalContext.current
    val animScale = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    } catch (_: Exception) {
        1f
    }
    val reduceMotion = animScale == 0f

    var remainingSeconds by remember { mutableIntStateOf(delaySeconds) }
    var selectedReason by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0D0D)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = if (isReintervention) "Mindful Check-in" else "Mindful Pause",
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedText,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isReintervention) {
                        "You've been in $appLabel for ${config.sessionMinutes} minutes."
                    } else {
                        appLabel
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Body depending on PauseStyle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (config.style) {
                    PauseStyle.BREATHE -> {
                        BreatheCircleContent(
                            reduceMotion = reduceMotion,
                            remainingSeconds = remainingSeconds
                        )
                    }
                    PauseStyle.INTENTION -> {
                        IntentionContent(
                            appLabel = appLabel,
                            selectedReason = selectedReason,
                            onReasonSelected = { selectedReason = it },
                            remainingSeconds = remainingSeconds
                        )
                    }
                    PauseStyle.DELAY_ONLY -> {
                        DelayOnlyContent(
                            remainingSeconds = remainingSeconds
                        )
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Countdown accessibility live region
                if (remainingSeconds > 0) {
                    Text(
                        text = "Available in ${remainingSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                } else {
                    Text(
                        text = "Take your time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF32D74B),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                // Leave Button (Instant, always available)
                Button(
                    onClick = onLeave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Text(
                        text = "Leave",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continue Button (Unlocks when countdown reaches 0)
                OutlinedButton(
                    onClick = { onContinue(selectedReason) },
                    enabled = remainingSeconds == 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF555555)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (remainingSeconds == 0) Color(0xFF444444) else Color(0xFF222222)
                        )
                    )
                ) {
                    Text(
                        text = if (remainingSeconds > 0) {
                            "Continue (${remainingSeconds}s)"
                        } else {
                            "Continue to $appLabel"
                        },
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BreatheCircleContent(
    reduceMotion: Boolean,
    remainingSeconds: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(if (reduceMotion) 1f else scale)
                .alpha(if (reduceMotion) 0.7f else alpha)
                .border(2.dp, Color(0xFF888888), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (remainingSeconds > 0) "$remainingSeconds" else "✓",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 42.sp
                ),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Take a breath.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = MutedText
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntentionContent(
    appLabel: String,
    selectedReason: String?,
    onReasonSelected: (String) -> Unit,
    remainingSeconds: Int
) {
    val intentions = listOf(
        "Important",
        "Just checking",
        "Bored",
        "Someone sent me something",
        "Other"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "What is your intention?",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            intentions.forEach { intention ->
                val isSelected = selectedReason == intention
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(
                            color = if (isSelected) Color.White else DarkSurface,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.White else Color(0xFF333333),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onReasonSelected(intention) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = intention,
                        color = if (isSelected) Color.Black else Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DelayOnlyContent(
    remainingSeconds: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (remainingSeconds > 0) "$remainingSeconds" else "Ready",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.ExtraLight,
                fontSize = 72.sp
            ),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pausing for reflection",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText
        )
    }
}
