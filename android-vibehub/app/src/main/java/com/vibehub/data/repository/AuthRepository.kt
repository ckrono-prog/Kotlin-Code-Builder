package com.vibehub.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    val currentSession: Flow<UserSession?> = supabase.auth.sessionStatus
        .map { status ->
            // Return the session if authenticated, null otherwise
            runCatching { supabase.auth.currentSessionOrNull() }.getOrNull()
        }

    val currentUserId: String?
        get() = runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()

    /** Sign up with email + password */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
            }
        }

    /** Sign in with email + password */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
        }

    /** Send OTP to phone for verification */
    suspend fun sendOtp(phone: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(OTP) {
                this.phone = phone
            }
        }

    /** Verify OTP code */
    suspend fun verifyOtp(phone: String, token: String): Result<Unit> =
        runCatching {
            supabase.auth.verifyPhoneOtp(
                phone = phone,
                token = token,
                type  = io.github.jan.supabase.auth.providers.builtin.Phone.Type.SMS,
            )
        }

    /** Sign in with Google OAuth */
    suspend fun signInWithGoogle(): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Google)
        }

    /** Send password reset email */
    suspend fun resetPassword(email: String): Result<Unit> =
        runCatching {
            supabase.auth.resetPasswordForEmail(email)
        }

    /** Update password */
    suspend fun updatePassword(newPassword: String): Result<Unit> =
        runCatching {
            supabase.auth.updateUser {
                password = newPassword
            }
        }

    /** Sign out */
    suspend fun signOut(): Result<Unit> =
        runCatching {
            supabase.auth.signOut()
        }

    fun isLoggedIn(): Boolean = currentUserId != null
}
