package com.vibehub.ui.screens.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.ProfileSetupViewModel
import com.vibehub.util.InputSanitizer

private val COUNTRY_CODES = listOf(
    "+1 🇺🇸 United States", "+44 🇬🇧 United Kingdom", "+91 🇮🇳 India",
    "+49 🇩🇪 Germany", "+33 🇫🇷 France", "+86 🇨🇳 China",
    "+81 🇯🇵 Japan", "+55 🇧🇷 Brazil", "+7 🇷🇺 Russia",
    "+27 🇿🇦 South Africa", "+234 🇳🇬 Nigeria", "+254 🇰🇪 Kenya",
    "+61 🇦🇺 Australia", "+64 🇳🇿 New Zealand", "+82 🇰🇷 South Korea",
    "+971 🇦🇪 UAE", "+966 🇸🇦 Saudi Arabia", "+20 🇪🇬 Egypt",
    "+52 🇲🇽 Mexico", "+34 🇪🇸 Spain", "+39 🇮🇹 Italy",
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsState()
    var step     by remember { mutableIntStateOf(0) }
    val context  = LocalContext.current

    var avatarUri       by remember { mutableStateOf<android.net.Uri?>(null) }
    var displayName     by remember { mutableStateOf("") }
    var username        by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var countryCode     by remember { mutableStateOf("+1") }
    var locationText    by remember { mutableStateOf("") }
    var expandCountry   by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> avatarUri = uri }
    val locationPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(uiState.suggestions) {
        if (username.isBlank() && uiState.suggestions.isNotEmpty()) {
            username = uiState.suggestions.first()
        }
    }
    LaunchedEffect(uiState.detectedLocation) {
        if (locationText.isBlank() && uiState.detectedLocation.isNotEmpty()) {
            locationText = uiState.detectedLocation
        }
    }
    LaunchedEffect(uiState.profileSaved) {
        if (uiState.profileSaved) onComplete()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color    = VibePink,
                trackColor = MaterialTheme.colorScheme.outline.copy(0.15f),
            )

            Spacer(Modifier.height(24.dp))
            Text(
                listOf("Set up your profile", "Contact & location", "Review")[step.coerceIn(0, 2)],
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(24.dp))

            AnimatedContent(targetState = step, label = "step", transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            }) { s ->
                when (s) {
                    0 -> StepOneProfile(
                        avatarUri   = avatarUri,
                        displayName = displayName,
                        username    = username,
                        suggestions = uiState.suggestions,
                        isChecking  = uiState.isCheckingUsername,
                        usernameAvailable = uiState.usernameAvailable,
                        onPickPhoto = { photoPicker.launch("image/*") },
                        onNameChange = { displayName = InputSanitizer.sanitizeDisplayName(it) },
                        onUsernameChange = {
                            username = InputSanitizer.sanitizeUsername(it)
                            viewModel.checkUsername(username)
                        },
                        onSelectSuggestion = { username = it },
                        onNext = { step = 1 },
                    )
                    1 -> StepTwoContact(
                        phone        = phone,
                        countryCode  = countryCode,
                        locationText = locationText,
                        expandCountry = expandCountry,
                        onPhoneChange = { phone = it.filter { c -> c.isDigit() }.take(15) },
                        onCountryCodeChange = { countryCode = it.substringBefore(" "); expandCountry = false },
                        onToggleCountry = { expandCountry = !expandCountry },
                        onLocationDetect = {
                            if (locationPerm.status.isGranted) viewModel.detectLocation(context)
                            else locationPerm.launchPermissionRequest()
                        },
                        onLocationChange = { locationText = InputSanitizer.sanitizeText(it, 100) },
                        onNext = { step = 2 },
                        onBack = { step = 0 },
                    )
                    else -> StepThreeReview(
                        avatarUri   = avatarUri,
                        displayName = displayName,
                        username    = username,
                        phone       = "$countryCode $phone",
                        location    = locationText,
                        isLoading   = uiState.isLoading,
                        error       = uiState.error,
                        onBack      = { step = 1 },
                        onSave      = {
                            viewModel.saveProfile(
                                avatarUri   = avatarUri,
                                displayName = displayName,
                                username    = username,
                                phone       = "$countryCode${phone.removePrefix("0")}",
                                location    = locationText,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepOneProfile(
    avatarUri: android.net.Uri?,
    displayName: String,
    username: String,
    suggestions: List<String>,
    isChecking: Boolean,
    usernameAvailable: Boolean?,
    onPickPhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Avatar picker
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUri != null) {
                    AsyncImage(model = avatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(32.dp), tint = VibeTextSecondary)
                }
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(VibePink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = displayName, onValueChange = onNameChange,
            label = { Text("Full name") }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
        )

        OutlinedTextField(
            value = username, onValueChange = onUsernameChange,
            label = { Text("Username") },
            leadingIcon = { Text("@", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
            trailingIcon = {
                when {
                    isChecking -> CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = VibePink)
                    usernameAvailable == true  -> Icon(Icons.Filled.CheckCircle, null, tint = VibeSuccess)
                    usernameAvailable == false -> Icon(Icons.Filled.Cancel, null, tint = VibeError)
                }
            },
            isError = usernameAvailable == false,
            supportingText = {
                when (usernameAvailable) {
                    true  -> Text("@$username is available!", color = VibeSuccess)
                    false -> Text("Username taken. Try another.", color = VibeError)
                    null  -> {}
                }
            },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
        )

        // Suggestions chips
        if (suggestions.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Suggestions:", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.foundation.lazy.items(suggestions) { s ->
                        FilterChip(selected = username == s, onClick = { onSelectSuggestion(s) }, label = { Text("@$s") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VibePink, selectedLabelColor = Color.White))
                    }
                }
            }
        }

        GradientButton(
            text    = "Continue →",
            enabled = displayName.isNotBlank() && username.length >= 3 && usernameAvailable != false,
            onClick = onNext,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepTwoContact(
    phone: String, countryCode: String, locationText: String, expandCountry: Boolean,
    onPhoneChange: (String) -> Unit, onCountryCodeChange: (String) -> Unit,
    onToggleCountry: () -> Unit, onLocationDetect: () -> Unit, onLocationChange: (String) -> Unit,
    onNext: () -> Unit, onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Phone with country code
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onToggleCountry, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(56.dp)) {
                Text(countryCode, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp))
            }
            OutlinedTextField(
                value = phone, onValueChange = onPhoneChange,
                label = { Text("Phone number") }, leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), singleLine = true,
            )
        }

        if (expandCountry) {
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                COUNTRY_CODES.forEach { cc ->
                    ListItem(
                        headlineContent = { Text(cc, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.clickable { onCountryCodeChange(cc) },
                    )
                }
            }
        }

        // Location
        OutlinedTextField(
            value = locationText, onValueChange = onLocationChange,
            label = { Text("Your location") }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
            trailingIcon = {
                IconButton(onClick = onLocationDetect) { Icon(Icons.Outlined.MyLocation, null, tint = VibePink) }
            },
            placeholder = { Text("City, Country or search…") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        )

        Text("📍 Tap the location icon to auto-detect", style = MaterialTheme.typography.labelSmall, color = VibeTextSecondary)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(2f), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = VibePink)) {
                Text("Continue →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepThreeReview(
    avatarUri: android.net.Uri?, displayName: String, username: String, phone: String, location: String,
    isLoading: Boolean, error: String?, onBack: () -> Unit, onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (avatarUri != null) AsyncImage(model = avatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Outlined.Person, null, modifier = Modifier.size(36.dp), tint = VibeTextSecondary)
        }
        Text(displayName.ifBlank { "—" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("@$username", color = VibePink, fontWeight = FontWeight.SemiBold)
        if (phone.isNotBlank()) ReviewRow(Icons.Outlined.Phone, phone)
        if (location.isNotBlank()) ReviewRow(Icons.Outlined.LocationOn, location)

        if (error != null) Text(error, color = VibeError, style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f), shape = RoundedCornerShape(28.dp)) { Text("Edit") }
            GradientButton(text = "Save Profile", isLoading = isLoading, enabled = !isLoading, onClick = onSave)
        }
    }
}

@Composable
private fun ReviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = VibePink, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
