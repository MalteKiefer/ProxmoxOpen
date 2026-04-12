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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    var unlocked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (unlocked) {
        onUnlocked()
        return
    }

    LaunchedEffect(Unit) {
        showBiometricPrompt(context as? FragmentActivity) { success ->
            unlocked = success
        }
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
    if (activity == null) { callback(true); return }
    val biometricManager = BiometricManager.from(activity)
    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
        callback(true)
        return
    }
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { callback(true) }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
            }
        }
        override fun onAuthenticationFailed() { /* user can retry */ }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("ProxMoxOpen")
        .setSubtitle(activity.getString(R.string.app_lock_prompt))
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(info)
}
