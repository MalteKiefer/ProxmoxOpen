package de.kiefer_networks.proxmoxopen.data.secrets

interface SecretStore {
    /**
     * Stores a secret under [key].
     *
     * @throws SecretStoreLockedException if the underlying Keystore key is bound to user
     *   authentication and the user has not authenticated within the timeout window. The
     *   caller must prompt for biometric / device-credential auth and retry.
     */
    suspend fun put(key: String, value: String)

    /**
     * Returns the secret stored under [key], or null if absent.
     *
     * @throws SecretStoreLockedException if the underlying Keystore key is bound to user
     *   authentication and the user has not authenticated within the timeout window. The
     *   caller must prompt for biometric / device-credential auth and retry.
     */
    suspend fun get(key: String): String?

    suspend fun remove(key: String)
}

/**
 * Thrown by [SecretStore.put] / [SecretStore.get] when the AndroidKeyStore key backing
 * the store requires fresh user authentication (e.g. biometric or device credential)
 * before it can be used. Callers should prompt the user with BiometricPrompt and retry.
 */
class SecretStoreLockedException(cause: Throwable? = null) :
    RuntimeException("Secrets locked; user authentication required", cause)
