package com.vibehub.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibehub.ui.components.GradientText
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.theme.*
import com.vibehub.ui.viewmodel.SettingsViewModel
import com.vibehub.util.BiometricHelper
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState  by viewModel.uiState.collectAsState()
    val context  = LocalContext.current
    val activity = context as? Activity

    var biometricEnabled    by remember { mutableStateOf(false) }
    var showLogoutDialog    by remember { mutableStateOf(false) }
    var showDeleteDialog    by remember { mutableStateOf(false) }

    val biometricHelper = remember { BiometricHelper() }
    val biometricAvail  = remember { biometricHelper.getAvailability(context) }

    // Observe logout state
    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLogout()
    }

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Log out?") },
            text    = { Text("You'll need to sign in again to access your account.") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(); showLogoutDialog = false }) {
                    Text("Log Out", color = VibeError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // ── Profile card (live from Supabase) ────────────────────────────
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(16.dp),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).clickable(onClick = onBack),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VibeAvatar(imageUrl = uiState.currentUser?.avatarUrl ?: "", size = 60.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.currentUser?.displayName ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${uiState.currentUser?.username ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Text("View and edit profile →", style = MaterialTheme.typography.labelSmall, color = VibePink)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }

            // ── Account ──────────────────────────────────────────────────────
            item { SettingsSectionHeader("Account") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Person, "Personal Information", onClick = {})
                    SettingsRow(Icons.Outlined.Lock, "Password & Security", onClick = onNavigateToSecurity)
                    SettingsRow(Icons.Outlined.Email, "Email & Phone", onClick = {})
                    SettingsRow(Icons.Outlined.Shield, "Privacy", onClick = onNavigateToPrivacy)
                    SettingsRow(Icons.Outlined.Block, "Blocked Accounts", onClick = onNavigateToBlocked)
                }
            }

            // ── Security ─────────────────────────────────────────────────────
            item { SettingsSectionHeader("Security") }
            item {
                SettingsGroup {
                    if (biometricAvail == BiometricHelper.Availability.AVAILABLE) {
                        SettingsToggleRow(
                            icon    = Icons.Outlined.Fingerprint,
                            label   = "Biometric App Lock",
                            checked = biometricEnabled,
                            onToggle = { enabled ->
                                if (enabled && activity != null) {
                                    biometricHelper.authenticate(
                                        activity  = activity as androidx.fragment.app.FragmentActivity,
                                        title     = "Enable Biometric Lock",
                                        subtitle  = "Authenticate to turn on fingerprint lock",
                                        onSuccess = { biometricEnabled = true },
                                    )
                                } else {
                                    biometricEnabled = false
                                }
                            },
                        )
                    } else {
                        ListItem(
                            headlineContent   = { Text("Biometric Lock", color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) },
                            supportingContent = { Text("Not available on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.3f)) },
                            leadingContent    = { Icon(Icons.Outlined.Fingerprint, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f)) },
                        )
                    }
                    SettingsRow(Icons.Outlined.DevicesOther, "Active Sessions", onClick = {})
                }
            }

            // ── Preferences ──────────────────────────────────────────────────
            item { SettingsSectionHeader("Preferences") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Notifications, "Notifications", onClick = onNavigateToNotifications)
                    SettingsRow(Icons.Outlined.Palette, "Appearance & Theme", onClick = onNavigateToAppearance)
                    SettingsRow(Icons.Outlined.Language, "Language", trailing = "English", onClick = {})
                    SettingsRow(Icons.Outlined.Download, "Storage & Downloads", onClick = onNavigateToDownloads)
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            item { SettingsSectionHeader("Content & Display") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.AutoFixHigh, "AI Personalization", onClick = {})
                    SettingsRow(Icons.Outlined.AccessibilityNew, "Accessibility", onClick = {})
                    SettingsRow(Icons.Outlined.DataSaverOn, "Data Usage", onClick = {})
                }
            }

            // ── Support ───────────────────────────────────────────────────────
            item { SettingsSectionHeader("Support") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Help, "Help Center", onClick = onNavigateToHelp)
                    SettingsRow(Icons.Outlined.BugReport, "Report a Problem", onClick = {})
                    SettingsRow(Icons.Outlined.Info, "About VibeHub", trailing = "1.0.0", onClick = {})
                    SettingsRow(Icons.Outlined.Description, "Terms & Privacy Policy", onClick = {})
                }
            }

            // ── Logout / Delete ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SettingsGroup {
                    ListItem(
                        headlineContent = { Text("Log Out", color = VibeError, fontWeight = FontWeight.SemiBold) },
                        leadingContent  = { Icon(Icons.Outlined.Logout, null, tint = VibeError) },
                        modifier        = Modifier.clickable { showLogoutDialog = true },
                    )
                    ListItem(
                        headlineContent = { Text("Delete Account", color = VibeError.copy(0.7f)) },
                        leadingContent  = { Icon(Icons.Outlined.PersonRemove, null, tint = VibeError.copy(0.7f)) },
                        modifier        = Modifier.clickable { showDeleteDialog = true },
                    )
                }
                if (uiState.error != null) {
                    Text(uiState.error ?: "", color = VibeError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ─── Shared Setting widgets ───────────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text         = title.uppercase(),
        style        = MaterialTheme.typography.labelSmall,
        color        = MaterialTheme.colorScheme.onSurface.copy(0.45f),
        fontWeight   = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier     = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailing: String? = null,
    showBadge: Boolean = false,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent  = { Text(label) },
        leadingContent   = { Icon(icon, null, tint = VibePink) },
        trailingContent  = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                if (showBadge) Badge { Text("New") }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(18.dp))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent  = { Text(label) },
        leadingContent   = { Icon(icon, null, tint = VibePink) },
        trailingContent  = {
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VibePink),
            )
        },
    )
}
