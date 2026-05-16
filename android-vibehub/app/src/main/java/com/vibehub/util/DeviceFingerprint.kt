package com.vibehub.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates a stable, privacy-respecting device identifier.
 * Uses ANDROID_ID (unique per app since Android 8) salted with the package name.
 * Also fetches the public IP for session tracking.
 *
 * The device ID is stored in Supabase device_sessions table (see migration SQL).
 */
@Singleton
class DeviceFingerprint @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Returns a stable SHA-256 hash unique to this install. */
    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val salted = "$raw:${context.packageName}"
        return sha256(salted)
    }

    /** Human-readable device label, e.g. "Samsung Galaxy S24 (Android 14)". */
    fun getDeviceName(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    /** Fetches the public IP address from a lightweight public API. */
    suspend fun getPublicIp(): String = withContext(Dispatchers.IO) {
        runCatching {
            URL("https://api.ipify.org").readText().trim().take(45)
        }.getOrElse { "unknown" }
    }

    /** Build the session payload for Supabase device_sessions table. */
    suspend fun buildSessionPayload(userId: String): Map<String, String> = mapOf(
        "user_id"     to userId,
        "device_id"   to getDeviceId(),
        "device_name" to getDeviceName(),
        "platform"    to "android",
        "ip_address"  to getPublicIp(),
    )

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
