package com.vibehub.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
        .map { runCatching { supabase.auth.currentSessionOrNull() }.getOrNull() }

    val currentUserId: String?
        get() = runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()

    val currentUserEmail: String?
        get() = runCatching { supabase.auth.currentUserOrNull()?.email }.getOrNull()

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

    /** Verify email OTP (sent automatically by Supabase on signup) */
    suspend fun verifyEmailOtp(email: String, token: String): Result<Unit> =
        runCatching {
            supabase.auth.verifyEmailOtp(
                email = email,
                token = token,
                type  = io.github.jan.supabase.auth.providers.builtin.Email.Type.EMAIL,
            )
        }

    /** Resend email OTP */
    suspend fun resendOtp(email: String): Result<Unit> =
        runCatching {
            supabase.auth.resendEmail(
                type  = io.github.jan.supabase.auth.providers.builtin.Email.Type.SIGNUP,
                email = email,
            )
        }

    /** Send password reset email */
    suspend fun resetPassword(email: String): Result<Unit> =
        runCatching {
            supabase.auth.resetPasswordForEmail(email)
        }

    /** Update password */
    suspend fun updatePassword(newPassword: String): Result<Unit> =
        runCatching {
            supabase.auth.updateUser { password = newPassword }
        }

    /** Sign out — call from SettingsViewModel or AuthViewModel */
    suspend fun signOut(): Result<Unit> =
        runCatching { supabase.auth.signOut() }

    /** Alias used by SettingsViewModel */
    suspend fun logout(): Result<Unit> = signOut()

    fun isLoggedIn(): Boolean = currentUserId != null

    fun isEmailConfirmed(): Boolean =
        runCatching { supabase.auth.currentUserOrNull()?.emailConfirmedAt != null }.getOrElse { false }

    fun needsProfileSetup(): Boolean {
        // After sign-up, the user exists in auth.users but not yet in public.profiles.
        // The ProfileSetupScreen will check this via UserRepository.getCurrentUser().
        return isLoggedIn()
    }
}
