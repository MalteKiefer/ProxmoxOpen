package de.kiefer_networks.proxmoxopen.data.api.tls

import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Trust-On-First-Use X509TrustManager. Accepts exactly one server cert whose SHA-256
 * fingerprint matches [expectedFingerprintSha256] (hex, lowercase, no separators).
 * Any other cert is rejected with [FingerprintMismatchException]. If [expectedFingerprintSha256]
 * is null (probe mode), accepts any cert and stores the observed fingerprint in [observedFingerprint].
 */
class TofuTrustManager(
    private val expectedFingerprintSha256: String?,
) : X509TrustManager {

    @Volatile
    var observedFingerprint: String? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        // client auth not used
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        require(chain.isNotEmpty()) { "empty server cert chain" }
        val leaf = chain[0]
        val actual = sha256Hex(leaf.encoded)
        observedFingerprint = actual
        val expected = expectedFingerprintSha256 ?: return
        if (!actual.equals(expected, ignoreCase = true)) {
            throw FingerprintMismatchException(expected, actual)
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
