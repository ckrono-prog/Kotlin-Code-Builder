package com.vibehub.util

import android.webkit.MimeTypeMap
import java.io.File

/**
 * Sanitizes all user-submitted text and validates uploaded media files.
 * Call these helpers before any write operation that touches Supabase or Room.
 */
object InputSanitizer {

    // ── Text ──────────────────────────────────────────────────────────────────

    private val CONTROL_CHARS = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")
    private val SQL_INJECTION  = Regex("(?i)(--|;|/\\*|\\*/|xp_|\\bselect\\b|\\bdrop\\b|\\binsert\\b|\\bdelete\\b|\\bupdate\\b|\\bexec\\b)")
    private val XSS_PATTERNS   = Regex("(?i)(<script|javascript:|on\\w+=|<iframe|<object|<embed)")

    fun sanitizeText(input: String, maxLength: Int = 2200): String {
        return input
            .replace(CONTROL_CHARS, "")
            .replace(XSS_PATTERNS, "")
            .trim()
            .take(maxLength)
    }

    fun sanitizeUsername(input: String): String =
        input.lowercase().replace(Regex("[^a-z0-9._]"), "").take(30)

    fun sanitizeBio(input: String): String = sanitizeText(input, 200)

    fun sanitizeCaption(input: String): String = sanitizeText(input, 2200)

    fun sanitizeComment(input: String): String = sanitizeText(input, 1000)

    fun sanitizeDisplayName(input: String): String = sanitizeText(input, 50)

    fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed.take(512) else ""
    }

    fun sanitizeSearchQuery(query: String): String =
        query.replace(SQL_INJECTION, "").replace(Regex("[<>\"']"), "").trim().take(100)

    // ── File / Upload ─────────────────────────────────────────────────────────

    private val ALLOWED_IMAGE_TYPES = setOf("jpg", "jpeg", "png", "gif", "webp", "heic")
    private val ALLOWED_VIDEO_TYPES = setOf("mp4", "mov", "3gp", "avi", "mkv", "webm")
    private val ALLOWED_AUDIO_TYPES = setOf("aac", "mp3", "m4a", "ogg", "opus", "wav")

    const val MAX_IMAGE_BYTES = 10 * 1024 * 1024L   // 10 MB
    const val MAX_VIDEO_BYTES = 200 * 1024 * 1024L  // 200 MB
    const val MAX_AUDIO_BYTES = 20 * 1024 * 1024L   // 20 MB

    data class ValidationResult(val isValid: Boolean, val reason: String = "")

    fun validateImageFile(file: File): ValidationResult {
        if (!file.exists()) return ValidationResult(false, "File not found")
        val ext = file.extension.lowercase()
        if (ext !in ALLOWED_IMAGE_TYPES) return ValidationResult(false, "Unsupported image type: $ext")
        if (file.length() > MAX_IMAGE_BYTES) return ValidationResult(false, "Image exceeds 10 MB limit")
        return ValidationResult(true)
    }

    fun validateVideoFile(file: File): ValidationResult {
        if (!file.exists()) return ValidationResult(false, "File not found")
        val ext = file.extension.lowercase()
        if (ext !in ALLOWED_VIDEO_TYPES) return ValidationResult(false, "Unsupported video type: $ext")
        if (file.length() > MAX_VIDEO_BYTES) return ValidationResult(false, "Video exceeds 200 MB limit")
        return ValidationResult(true)
    }

    fun validateAudioFile(file: File): ValidationResult {
        if (!file.exists()) return ValidationResult(false, "File not found")
        val ext = file.extension.lowercase()
        if (ext !in ALLOWED_AUDIO_TYPES) return ValidationResult(false, "Unsupported audio type: $ext")
        if (file.length() > MAX_AUDIO_BYTES) return ValidationResult(false, "Audio exceeds 20 MB limit")
        return ValidationResult(true)
    }

    fun getMimeType(file: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "application/octet-stream"

    fun sanitizePath(path: String): String =
        path.replace(Regex("[^a-zA-Z0-9._/\\-]"), "_").take(512)
}
