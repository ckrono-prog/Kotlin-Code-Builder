package com.vibehub.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.components.GradientText
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(modifier = Modifier.fillMaxSize().background(VibeGradientVertical)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            Surface(
                modifier = Modifier.size(80.dp),
                shape    = RoundedCornerShape(22.dp),
                color    = Color.White.copy(alpha = 0.2f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("VH", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(16.dp))
            GradientText("VibeHub", MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
            Text("Share your moments. Connect with vibes.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))

            Spacer(Modifier.height(40.dp))

            // Card
            Card(
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("Welcome Back!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Login to continue", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon  = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.signIn(email, password)
                        }),
                    )

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { viewModel.setStep(com.vibehub.ui.viewmodel.AuthStep.FORGOT_PASSWORD) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Forgot Password?", color = VibePink)
                    }

                    AnimatedVisibility(visible = uiState.error != null) {
                        Text(
                            text  = uiState.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    GradientButton(
                        text      = "Login",
                        onClick   = { viewModel.signIn(email, password) },
                        isLoading = uiState.isLoading,
                        enabled   = email.isNotBlank() && password.isNotBlank(),
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Divider(modifier = Modifier.weight(1f))
                        Text("  OR  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Divider(modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick   = onNavigateToRegister,
                        modifier  = Modifier.fillMaxWidth().height(52.dp),
                        shape     = RoundedCornerShape(14.dp),
                        border    = BorderStroke(1.5.dp, VibeGradient),
                    ) {
                        Icon(Icons.Outlined.PersonAdd, null, tint = VibePink)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Up", color = VibePink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row {
                Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Sign Up",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
