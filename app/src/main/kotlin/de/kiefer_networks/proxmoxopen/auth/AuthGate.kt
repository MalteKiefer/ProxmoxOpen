package de.kiefer_networks.proxmoxopen.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Single source of truth for "make the user authenticate right now".
 *
 * The AndroidKeyStore key backing [de.kiefer_networks.proxmoxopen.data.secrets.SecretStore]
 * is bound to user authentication with a 300s validity window. When that window expires,
 * `get` / `put` throw [de.kiefer_networks.proxmoxopen.data.secrets.SecretStoreLockedException].
 * ViewModels recover by catching that exception, calling [ensureFreshAuth] to re-prompt the
 * user, and then retrying the secret-store call once.
 *
 * Activity binding is required because [BiometricPrompt] needs a [FragmentActivity]; the
 * weak (atomic) reference is set in [MainActivity.onResume] and cleared in `onPause`, so
 * any prompt issued while no Activity is foregrounded fails fast (returns `false`).
 */
@Singleton
class AuthGate @Inject constructor() {
    private val activityRef = AtomicReference<FragmentActivity?>(null)

    fun bindActivity(activity: FragmentActivity) {
        activityRef.set(activity)
    }

    fun unbindActivity(activity: FragmentActivity) {
        activityRef.compareAndSet(activity, null)
    }

    /**
     * Suspends until the user has freshly authenticated via biometric / device credential.
     * Returns true on success, false if the user cancelled, no activity is bound, or the
     * device cannot authenticate.
     *
     * Rate-limited: callers should wrap the SECOND attempt; the first failure already
     * means the cached auth window has elapsed.
     */
    suspend fun ensureFreshAuth(title: String, subtitle: String? = null): Boolean {
        val activity = activityRef.get() ?: return false
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            return false
        }
        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        // user can retry
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { subtitle?.let { setSubtitle(it) } }
                .setAllowedAuthenticators(authenticators)
                .build()
            try {
                prompt.authenticate(info)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(false)
            }
            cont.invokeOnCancellation {
                // cannot cancel an in-flight prompt cleanly
            }
        }
    }
}
