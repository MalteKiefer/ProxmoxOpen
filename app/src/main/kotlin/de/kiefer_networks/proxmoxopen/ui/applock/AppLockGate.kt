package de.kiefer_networks.proxmoxopen.ui.applock

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import de.kiefer_networks.proxmoxopen.R

@Composable
fun AppLockGate(
    enabled: Boolean,
    onUnlocked: @Composable () -> Unit,
) {
    if (!enabled) {
        onUnlocked()
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var unlocked by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> { unlocked = false }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (!unlocked) {
                        showBiometricPrompt(context as? FragmentActivity) { success -> unlocked = success }
                    }
                }
                else -> { /* no-op */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (unlocked) {
        onUnlocked()
        return
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
            Text(
                stringResource(R.string.app_lock_prompt),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = {
                showBiometricPrompt(context as? FragmentActivity) { success ->
                    unlocked = success
                }
            }) {
                Text(stringResource(R.string.app_lock_retry))
            }
        }
    }
}

private fun showBiometricPrompt(activity: FragmentActivity?, callback: (Boolean) -> Unit) {
    if (activity == null) { callback(false); return }
    val biometricManager = BiometricManager.from(activity)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                         BiometricManager.Authenticators.DEVICE_CREDENTIAL
    when (biometricManager.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> { /* fall through */ }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
            // TODO(i18n): move hardcoded string to R.string.app_lock_unavailable
            Toast.makeText(
                activity,
                "App lock is unavailable: enable a screen lock or biometric in system settings.",
                Toast.LENGTH_LONG,
            ).show()
            callback(false)
            return
        }
        else -> { callback(false); return }
    }
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { callback(true) }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
            }
            callback(false)
        }
        override fun onAuthenticationFailed() { /* user can retry */ }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("ProxMoxOpen")
        .setSubtitle(activity.getString(R.string.app_lock_prompt))
        .setAllowedAuthenticators(authenticators)
        .build()
    prompt.authenticate(info)
}
