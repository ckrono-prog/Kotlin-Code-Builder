package com.vibehub.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.AuthViewModel
import com.vibehub.ui.viewmodel.AuthStep
import kotlinx.coroutines.*

/**
 * 6-digit OTP screen.
 * Supabase sends a verification code to the user's email on sign-up.
 */
@Composable
fun OtpVerifyScreen(
    email: String,
    onVerified: () -> Unit,       // → ProfileSetupScreen
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsState()
    var otpDigits by remember { mutableStateOf(List(6) { "" }) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val scope = rememberCoroutineScope()
    var resendCooldown by remember { mutableIntStateOf(60) }
    var canResend      by remember { mutableStateOf(false) }

    // Countdown timer for resend
    LaunchedEffect(Unit) {
        while (resendCooldown > 0) {
            delay(1000L)
            resendCooldown--
        }
        canResend = true
    }

    // Navigate on success
    LaunchedEffect(uiState.step) {
        if (uiState.step == AuthStep.PROFILE_SETUP || (uiState.isEmailConfirmed && uiState.profileComplete)) {
            onVerified()
        }
    }

    val fullOtp   = otpDigits.joinToString("")
    val isComplete = fullOtp.length == 6 && fullOtp.all { it.isDigit() }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))

        Box(
            modifier = Modifier.size(72.dp).background(VibePink.copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MarkEmailRead, null, tint = VibePink, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text("Check your email", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("We sent a 6-digit code to", style = MaterialTheme.typography.bodyMedium, color = VibeTextSecondary)
        Text(email, fontWeight = FontWeight.Bold, color = VibePink, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(32.dp))

        // OTP digit boxes
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            otpDigits.forEachIndexed { idx, digit ->
                OtpDigitBox(
                    value          = digit,
                    focusRequester = focusRequesters[idx],
                    onValueChange  = { newVal ->
                        if (newVal.length <= 1 && (newVal.isEmpty() || newVal.all { it.isDigit() })) {
                            otpDigits = otpDigits.toMutableList().also { it[idx] = newVal }
                            if (newVal.isNotEmpty() && idx < 5) focusRequesters[idx + 1].requestFocus()
                            else if (newVal.isEmpty() && idx > 0) focusRequesters[idx - 1].requestFocus()
                        }
                    },
                    onBackspace = {
                        if (digit.isEmpty() && idx > 0) {
                            otpDigits = otpDigits.toMutableList().also { it[idx - 1] = "" }
                            focusRequesters[idx - 1].requestFocus()
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Error
        AnimatedVisibility(visible = uiState.error != null) {
            Text(uiState.error ?: "", color = VibeError, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        // Verify button
        GradientButton(
            text      = "Verify Email",
            isLoading = uiState.isLoading,
            enabled   = isComplete && !uiState.isLoading,
            onClick   = { viewModel.verifyOtp(email, fullOtp) },
        )

        Spacer(Modifier.height(16.dp))

        // Resend
        if (canResend) {
            TextButton(onClick = {
                viewModel.resendOtp(email)
                resendCooldown = 60
                canResend = false
                scope.launch { while (resendCooldown > 0) { delay(1000L); resendCooldown-- }; canResend = true }
            }) {
                Text("Resend code", color = VibePink, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Resend in ${resendCooldown}s", style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary)
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack) {
            Text("Wrong email? Go back", color = VibeTextSecondary)
        }
    }
}

@Composable
private fun OtpDigitBox(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val focused = remember { mutableStateOf(false) }
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier
            .size(52.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused.value = it.isFocused },
        textStyle     = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = VibePink,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f),
        ),
    )
}
