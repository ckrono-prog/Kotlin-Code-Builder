package com.vibehub.util

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class ContactMatch(
    val name: String,
    val phoneHash: String,
    val normalizedPhone: String,
)

/**
 * Reads device contacts and produces SHA-256 hashed E.164 phone numbers.
 * We never send raw phone numbers to the server — only hashes.
 *
 * The hashes are sent to the `contact-suggestions` Edge Function, which
 * matches them against public.contact_hashes and returns VibeHub profiles.
 */
@Singleton
class ContactsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns up to [limit] hashed phone numbers from device contacts.
     * Requires READ_CONTACTS permission before calling.
     */
    suspend fun getHashedContacts(limit: Int = 500): List<ContactMatch> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<ContactMatch>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            )
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
            ) ?: return@withContext emptyList()

            cursor.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext() && results.size < limit) {
                    val name  = c.getString(nameIdx) ?: continue
                    val phone = c.getString(phoneIdx) ?: continue
                    val normalized = normalizePhone(phone) ?: continue
                    results += ContactMatch(
                        name            = name,
                        phoneHash       = sha256(normalized),
                        normalizedPhone = normalized,
                    )
                }
            }
            results.distinctBy { it.phoneHash }
        }

    /** Strips non-digit characters and ensures E.164-like format. */
    private fun normalizePhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() || it == '+' }
        return if (digits.length >= 7) digits else null
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
