package com.vibehub.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.vibehub.ui.components.GradientButton
import com.vibehub.ui.components.VibeAvatar
import com.vibehub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(onBack: () -> Unit, onSave: () -> Unit) {
    var displayName  by remember { mutableStateOf("Armenam") }
    var username     by remember { mutableStateOf("armenam244") }
    var bio          by remember { mutableStateOf("") }
    var website      by remember { mutableStateOf("") }
    var location     by remember { mutableStateOf("") }
    var pronouns     by remember { mutableStateOf("") }
    var gender       by remember { mutableStateOf("") }
    var birthday     by remember { mutableStateOf("") }
    var isPrivate    by remember { mutableStateOf(false) }
    var showNSFW     by remember { mutableStateOf(false) }
    var avatarUri    by remember { mutableStateOf<String?>(null) }
    var coverUri     by remember { mutableStateOf<String?>(null) }
    var showCategory by remember { mutableStateOf(false) }
    var category     by remember { mutableStateOf("") }
    var isSaving     by remember { mutableStateOf(false) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri?.toString()
    }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        coverUri = uri?.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = { isSaving = true; onSave() }) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VibePink)
                        } else {
                            Text("Save", color = VibePink, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // Cover photo + avatar
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    // Cover
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp).background(VibeGradient).clickable {
                            coverLauncher.launch("image/*")
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (coverUri != null) {
                            AsyncImage(model = coverUri, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Icon(Icons.Outlined.CameraAlt, null, tint = Color.White.copy(0.75f), modifier = Modifier.size(32.dp))
                    }
                    // Avatar
                    Box(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp),
                    ) {
                        VibeAvatar(imageUrl = avatarUri ?: "", size = 88.dp)
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).size(28.dp)
                                .clip(CircleShape).background(VibePink)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { avatarLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // FEATURE 1: Name & Username
            item { SettingsSectionHeader("Identity") }
            item {
                SettingsGroup {
                    ProfileTextField(Icons.Outlined.Person, "Display Name", displayName) { displayName = it }
                    ProfileTextField(Icons.Outlined.AlternateEmail, "Username", username) { username = it }
                }
            }

            // FEATURE 2: Bio
            item { SettingsSectionHeader("Bio") }
            item {
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Notes, null, tint = VibePink, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Bio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bio, onValueChange = { if (it.length <= 200) bio = it },
                            placeholder = { Text("Tell your story (${200 - bio.length} left)") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            minLines = 3, maxLines = 5,
                        )
                    }
                }
            }

            // FEATURE 3: Links & Contact
            item { SettingsSectionHeader("Links & Contact") }
            item {
                SettingsGroup {
                    ProfileTextField(Icons.Outlined.Link, "Website", website, "https://") { website = it }
                    ProfileTextField(Icons.Outlined.LocationOn, "Location", location, "City, Country") { location = it }
                }
            }

            // FEATURE 4: Pronouns
            item { SettingsSectionHeader("Personal") }
            item {
                SettingsGroup {
                    ProfileTextField(Icons.Outlined.RecordVoiceOver, "Pronouns", pronouns, "e.g. they/them") { pronouns = it }
                    ProfileTextField(Icons.Outlined.Wc, "Gender", gender, "Optional") { gender = it }
                    ProfileTextField(Icons.Outlined.Cake, "Birthday", birthday, "MM/DD/YYYY") { birthday = it }
                }
            }

            // FEATURE 5: Creator category
            item { SettingsSectionHeader("Creator") }
            item {
                SettingsGroup {
                    SettingsToggleRow(Icons.Outlined.Stars, "Show Creator Category", showCategory) { showCategory = it }
                    if (showCategory) {
                        ProfileTextField(Icons.Outlined.Category, "Category", category, "e.g. Musician, Gamer") { category = it }
                    }
                    SettingsRow(Icons.Outlined.BarChart, "Creator Dashboard", onClick = {})
                    SettingsRow(Icons.Outlined.MonetizationOn, "Monetization", onClick = {})
                }
            }

            // FEATURE 6: Privacy toggles
            item { SettingsSectionHeader("Privacy") }
            item {
                SettingsGroup {
                    SettingsToggleRow(Icons.Outlined.Lock, "Private Account", isPrivate) { isPrivate = it }
                    SettingsToggleRow(Icons.Outlined.VisibilityOff, "Restrict NSFW Content", showNSFW) { showNSFW = it }
                }
            }

            // FEATURE 7: Connected accounts
            item { SettingsSectionHeader("Connected Accounts") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Share, "Twitter / X", trailing = "Connect", onClick = {})
                    SettingsRow(Icons.Outlined.Facebook, "Facebook", trailing = "Connect", onClick = {})
                    SettingsRow(Icons.Outlined.Link, "TikTok", trailing = "Connect", onClick = {})
                }
            }

            // FEATURE 8: Profile URL
            item { SettingsSectionHeader("Share Profile") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.QrCode, "My QR Code", onClick = {})
                    SettingsRow(Icons.Outlined.Share, "Share Profile Link", onClick = {})
                }
            }

            // FEATURE 9: Badges
            item { SettingsSectionHeader("Badges & Achievements") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.EmojiEvents, "My Badges", trailing = "4", onClick = {})
                    SettingsRow(Icons.Outlined.Verified, "Get Verified", onClick = {})
                }
            }

            // FEATURE 10: Danger zone
            item { SettingsSectionHeader("Account Actions") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Outlined.Pause, "Deactivate Account", onClick = {})
                    ListItem(
                        headlineContent = { Text("Delete Account", color = VibeError) },
                        leadingContent  = { Icon(Icons.Outlined.PersonRemove, null, tint = VibeError) },
                        modifier = Modifier.clickable {},
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit,
) {
    ListItem(
        headlineContent = {
            OutlinedTextField(
                value = value, onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibePink,
                    focusedLabelColor  = VibePink,
                ),
            )
        },
        leadingContent = { Icon(icon, null, tint = VibePink) },
    )
}
