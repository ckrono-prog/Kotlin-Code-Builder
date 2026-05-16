package com.vibehub.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.AuthViewModel
import com.vibehub.ui.viewmodel.AuthStep
import com.vibehub.util.InputSanitizer
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: (email: String) -> Unit,   // → OTP screen, receives the email
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var confirmPwd  by remember { mutableStateOf("") }
    var showPwd     by remember { mutableStateOf(false) }
    var agreeTerms  by remember { mutableStateOf(false) }
    val focus       = LocalFocusManager.current
    val scope       = rememberCoroutineScope()

    // Navigate after successful sign-up — pass email so OTP screen can display + resend it
    LaunchedEffect(uiState.step) {
        if (uiState.step == AuthStep.OTP_VERIFY) onSignUpSuccess(email.trim())
    }

    val emailError   = email.isNotEmpty() && !email.contains("@")
    val pwdMismatch  = confirmPwd.isNotEmpty() && confirmPwd != password
    val pwdWeak      = password.isNotEmpty() && password.length < 8
    val canSignUp    = email.isNotEmpty() && password.length >= 8 && !pwdMismatch && agreeTerms && !uiState.isLoading

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            Box(
                modifier = Modifier.size(80.dp).background(VibeGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("V", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(20.dp))
            Text("Create Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Join VibeHub today — it's free", style = MaterialTheme.typography.bodyMedium, color = VibeTextSecondary)
            Spacer(Modifier.height(32.dp))

            // Email
            OutlinedTextField(
                value         = email,
                onValueChange = { email = InputSanitizer.sanitizeText(it.trim(), 254) },
                label         = { Text("Email address") },
                leadingIcon   = { Icon(Icons.Outlined.Email, null) },
                isError       = emailError,
                supportingText = { if (emailError) Text("Enter a valid email address", color = VibeError) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
            )

            Spacer(Modifier.height(14.dp))

            // Password
            OutlinedTextField(
                value         = password,
                onValueChange = { password = it },
                label         = { Text("Password") },
                leadingIcon   = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon  = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                    }
                },
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                isError       = pwdWeak,
                supportingText = { if (pwdWeak) Text("Minimum 8 characters", color = VibeError) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
            )

            // Password strength bar
            if (password.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                PasswordStrengthBar(password)
            }

            Spacer(Modifier.height(14.dp))

            // Confirm password
            OutlinedTextField(
                value         = confirmPwd,
                onValueChange = { confirmPwd = it },
                label         = { Text("Confirm password") },
                leadingIcon   = { Icon(Icons.Outlined.LockOpen, null) },
                visualTransformation = PasswordVisualTransformation(),
                isError       = pwdMismatch,
                supportingText = { if (pwdMismatch) Text("Passwords don't match", color = VibeError) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
            )

            Spacer(Modifier.height(16.dp))

            // Terms checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = agreeTerms, onCheckedChange = { agreeTerms = it }, colors = CheckboxDefaults.colors(checkedColor = VibePink))
                Text(
                    text = "I agree to the Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Error
            AnimatedVisibility(visible = uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors   = CardDefaults.cardColors(containerColor = VibeError.copy(0.1f)),
                    shape    = RoundedCornerShape(12.dp),
                ) {
                    Text(uiState.error ?: "", color = VibeError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            // Sign up button
            GradientButton(
                text      = "Create Account",
                isLoading = uiState.isLoading,
                enabled   = canSignUp,
                onClick   = { viewModel.signUp(email.trim(), password) },
            )

            Spacer(Modifier.height(20.dp))

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f)); Text(" or ", style = MaterialTheme.typography.labelMedium, color = VibeTextSecondary); HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // Already have account
            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? ", color = VibeTextSecondary)
                Text("Sign In", color = VibePink, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    val strength = when {
        password.length >= 12 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 10 && (password.any { it.isDigit() } || password.any { it.isUpperCase() })   -> 2
        password.length >= 8 -> 1
        else                 -> 0
    }
    val (label, color) = when (strength) {
        3    -> "Strong" to VibeSuccess
        2    -> "Medium" to VibeGold
        else -> "Weak"   to VibeError
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { idx ->
            LinearProgressIndicator(
                progress  = { if (idx < strength) 1f else 0f },
                modifier  = Modifier.weight(1f).height(4.dp),
                color     = if (idx < strength) color else MaterialTheme.colorScheme.outline.copy(0.2f),
                trackColor = MaterialTheme.colorScheme.outline.copy(0.1f),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}
