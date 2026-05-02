/*
 * KeystoreSecretStore — AES-256-GCM secret store backed by AndroidKeyStore.
 *
 * SECURITY NOTE (F-004): the wrapping key is bound to user authentication (biometric
 * STRONG or device credential) with a 300s validity window. The Keystore alias has
 * been bumped from v1 -> v2 to introduce this binding. v1 blobs (if any) are NOT
 * migrated; they remain under the old alias until the OS prunes them. This is a
 * one-time reset: on first launch after the upgrade containing this change, users
 * will need to re-add their servers (token secrets / passwords are not transferred).
 * The trade-off is acceptable because the v1 alias was unbound and the secret-set is
 * small (server token secrets only — passwords are no longer persisted, see F-006).
 */
package de.kiefer_networks.proxmoxopen.data.secrets

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

/**
 * AES-256-GCM secret store backed by AndroidKeyStore (hardware-backed when available).
 * Ciphertext + IV are persisted in a DataStore preferences file keyed by the logical key.
 */
class KeystoreSecretStore(context: Context) : SecretStore {

    private val appContext = context.applicationContext
    private val dataStore: DataStore<Preferences> = appContext.secretsDataStore

    override suspend fun put(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(key.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            val ct = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val encoded = VERSION_PREFIX +
                Base64.encodeToString(iv, Base64.NO_WRAP) +
                "." + Base64.encodeToString(ct, Base64.NO_WRAP)
            dataStore.edit { it[stringPreferencesKey(key)] = encoded }
        } catch (e: UserNotAuthenticatedException) {
            throw SecretStoreLockedException(e)
        }
    }

    override suspend fun get(key: String): String? {
        val encoded = dataStore.data.first()[stringPreferencesKey(key)] ?: return null
        val isV2 = encoded.startsWith(VERSION_PREFIX)
        val payload = if (isV2) encoded.removePrefix(VERSION_PREFIX) else encoded
        val parts = payload.split(".", limit = 2)
        if (parts.size != 2) return null
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ct = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            if (isV2) {
                cipher.updateAAD(key.toByteArray(Charsets.UTF_8))
            }
            val plaintext = String(cipher.doFinal(ct), Charsets.UTF_8)
            if (!isV2) {
                // Silently migrate legacy v1 blob to v2 with AAD binding.
                try {
                    put(key, plaintext)
                } catch (_: Exception) {
                    // Migration failure is non-fatal; v1 blob remains usable.
                }
            }
            plaintext
        } catch (e: UserNotAuthenticatedException) {
            throw SecretStoreLockedException(e)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = getKey()
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                /* timeoutSeconds = */ AUTH_VALIDITY_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            .setInvalidatedByBiometricEnrollment(true)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (appContext.packageManager.hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE,
                )
            ) {
                builder.setIsStrongBoxBacked(true)
            }
        }
        return try {
            generator.init(builder.build())
            generator.generateKey()
        } catch (_: java.security.ProviderException) {
            val fallback = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    /* timeoutSeconds = */ AUTH_VALIDITY_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
                .setInvalidatedByBiometricEnrollment(true)
                .build()
            generator.init(fallback)
            generator.generateKey()
        }
    }

    private fun getKey(): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry ?: return null
        return entry.secretKey
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "proxmoxopen_secret_store_v2"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val VERSION_PREFIX = "v2."
        private const val AUTH_VALIDITY_SECONDS = 300
    }
}

private val Context.secretsDataStore: DataStore<Preferences> by preferencesDataStore(name = "secrets")
