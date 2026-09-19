package com.minimalphone.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minimalphone.core.ui.theme.DarkSurface
import com.minimalphone.core.ui.theme.MutedText
import com.minimalphone.core.ui.theme.PureBlack
import com.minimalphone.core.ui.theme.SuccessGreen

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.currentStep) {
        viewModel.refreshState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Progress Dots
            StepIndicator(currentStep = uiState.currentStep)

            // Center Content: Step Specific
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.currentStep) {
                    OnboardingStep.VISION -> VisionStepContent()
                    OnboardingStep.DEFAULT_LAUNCHER -> DefaultLauncherStepContent(
                        isDefault = uiState.isDefaultLauncher,
                        onOpenSettings = { viewModel.openDefaultLauncherSettings(context) }
                    )
                    OnboardingStep.PERMISSIONS -> PermissionsStepContent(
                        isAccessibility = uiState.isAccessibilityEnabled,
                        isUsage = uiState.isUsageGranted,
                        onOpenAccessibility = { viewModel.openAccessibilitySettings() },
                        onOpenUsage = { viewModel.openUsageSettings() }
                    )
                    OnboardingStep.SCAN_AND_READY -> ScanReadyStepContent(
                        uiState = uiState
                    )
                }
            }

            // Bottom Actions: Navigation Controls
            StepControls(
                currentStep = uiState.currentStep,
                onBack = { viewModel.previousStep() },
                onNext = { viewModel.nextStep() },
                onFinish = { viewModel.completeOnboarding(onFinishOnboarding) }
            )
        }
    }
}

@Composable
private fun StepIndicator(currentStep: OnboardingStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingStep.entries.forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isCompleted = step.ordinal < currentStep.ordinal

            Box(
                modifier = Modifier
                    .size(if (isActive) 24.dp else 8.dp, 8.dp)
                    .background(
                        color = when {
                            isActive -> Color.White
                            isCompleted -> Color(0xFF52525B)
                            else -> Color(0xFF27272A)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            if (index < OnboardingStep.entries.size - 1) {
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun VisionStepContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = com.minimalphone.core.ui.R.drawable.numbphone_logo),
            contentDescription = "NumbPhone Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NUMBPHONE",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 4.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Make intentional actions easy\nand impulsive actions deliberate.",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFEDEDED),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your phone is a tool. NumbPhone removes dopamine traps, replaces chaotic home screens, and protects your focus sessions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun DefaultLauncherStepContent(
    isDefault: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DEFAULT LAUNCHER",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set NumbPhone as your default home app to replace your conventional home screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(12.dp))
                .border(1.dp, if (isDefault) Color(0xFF27272A) else Color(0xFF52525B), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isDefault) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDefault) SuccessGreen else Color(0xFFFF9F0A),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (isDefault) "Default Launcher Active" else "Not Set as Default",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!isDefault) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SET DEFAULT",
                            color = PureBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsStepContent(
    isAccessibility: Boolean,
    isUsage: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenUsage: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FOCUS PROTECTION",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Grant permissions to protect focus sessions against notification and recent app bypasses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Permission Card 1: Accessibility
        PermissionCard(
            title = "Bypass Backstop (Accessibility)",
            description = "Intercepts attempts to open distracting apps from notifications during focus.",
            isGranted = isAccessibility,
            actionText = "ENABLE",
            onAction = onOpenAccessibility
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Permission Card 2: Usage Access
        PermissionCard(
            title = "Usage Access (Screen Time)",
            description = "Calculates screen time and category breakdown without cloud tracking.",
            isGranted = isUsage,
            actionText = "GRANT",
            onAction = onOpenUsage
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, if (isGranted) Color(0xFF27272A) else Color(0xFF52525B), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )

                Box(
                    modifier = Modifier
                        .background(if (isGranted) Color(0xFF1C1C1E) else Color.White, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isGranted, onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else actionText,
                        color = if (isGranted) MutedText else PureBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = description,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ScanReadyStepContent(uiState: OnboardingUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "YOU ARE READY",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isScanning) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Scanning & Classifying Apps...", color = MutedText)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "APP CLASSIFICATION SUMMARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedText,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Essential (Always Available)", color = Color.White)
                        Text(text = "${uiState.essentialAppsCount}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Managed (Controlled during Focus)", color = Color.White)
                        Text(text = "${uiState.managedAppsCount}", color = Color(0xFFFF9F0A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepControls(
    currentStep: OnboardingStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep != OnboardingStep.VISION) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3F3F46))),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "BACK")
            }
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }

        if (currentStep == OnboardingStep.SCAN_AND_READY) {
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PureBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "ENTER NUMBPHONE", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PureBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "CONTINUE", fontWeight = FontWeight.Bold)
            }
        }
    }
}
