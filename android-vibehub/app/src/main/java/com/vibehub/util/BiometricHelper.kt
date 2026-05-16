package com.vibehub.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages fingerprint / face unlock for the VibeHub app lock feature.
 *
 * Usage:
 *   val helper = BiometricHelper(context)
 *   if (helper.isAvailable()) {
 *       helper.authenticate(activity, onSuccess = { /* unlock */ }, onFailed = { /* wrong finger */ })
 *   }
 */
@Singleton
class BiometricHelper @Inject constructor() {

    enum class Availability {
        AVAILABLE,              // Ready to use
        NO_HARDWARE,            // Device has no biometric sensor
        NONE_ENROLLED,          // Sensor exists but no fingerprints enrolled
        NOT_AVAILABLE,          // Temporarily unavailable
    }

    fun getAvailability(context: Context): Availability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS          -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else                                        -> Availability.NOT_AVAILABLE
        }
    }

    fun isAvailable(context: Context): Boolean =
        getAvailability(context) == Availability.AVAILABLE

    /**
     * Show the biometric prompt.
     * @param activity     Must be a FragmentActivity (all Compose activities are).
     * @param title        Dialog title, e.g. "Unlock VibeHub".
     * @param subtitle     Sub-text shown beneath the title.
     * @param onSuccess    Called when authentication succeeds.
     * @param onFailed     Called on failed attempt (wrong finger/face).
     * @param onError      Called on a system error or user cancel.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String    = "Unlock VibeHub",
        subtitle: String = "Use your fingerprint or face to continue",
        onSuccess: () -> Unit,
        onFailed: () -> Unit  = {},
        onError: (String) -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationFailed() {
                onFailed()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
