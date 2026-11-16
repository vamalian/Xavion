package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricStatus
import com.example.ui.components.HoloPanel
import com.example.ui.components.HolographicBackground
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionTextSecondary

@Composable
fun LockGateScreen(
    biometricEnabled: Boolean,
    errorText: String?,
    onUnlockPin: (String) -> Unit,
    onUnlockBiometric: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val bioAvailable = remember { BiometricAuthHelper.checkStatus(context) == BiometricStatus.AVAILABLE }
    var pin by remember { mutableStateOf("") }
    var bioStatusMsg by remember { mutableStateOf<String?>(null) }

    val triggerBiometric = {
        if (activity != null && bioAvailable) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                onSuccess = onUnlockBiometric,
                onError = { bioStatusMsg = it }
            )
        }
    }

    LaunchedEffect(biometricEnabled, bioAvailable) {
        if (biometricEnabled && bioAvailable) {
            triggerBiometric()
        }
    }

    HolographicBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HoloPanel(
                modifier = Modifier.widthIn(max = 440.dp),
                borderColor = XavionBorderBright
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = XavionCyan,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("XAVION SECURITY GATE", style = MaterialTheme.typography.headlineMedium, color = XavionCyan)
                    Text(
                        "Verify owner identity to access XAVION AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XavionTextSecondary
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                        label = { Text("Owner PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_pin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XavionCyan,
                            unfocusedBorderColor = XavionBorderSubtle
                        )
                    )

                    val msg = errorText ?: bioStatusMsg
                    if (!msg.isNullOrBlank()) {
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = XavionCrimson)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onUnlockPin(pin) },
                            enabled = pin.length >= 4,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("lock_unlock_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                        ) { Text("Unlock") }

                        if (biometricEnabled && bioAvailable) {
                            OutlinedButton(
                                onClick = triggerBiometric,
                                modifier = Modifier.height(48.dp),
                                border = BorderStroke(1.dp, XavionBorderBright)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Biometric unlock", tint = XavionCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Biometric", color = XavionCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}
