package de.kiefer_networks.proxmoxopen.data.api.tls

import java.security.MessageDigest
import java.security.cert.CertificateException
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

    init {
        require(expectedFingerprintSha256 == null || expectedFingerprintSha256.matches(Regex("^[0-9a-fA-F]{64}$"))) {
            "expected pin must be 64 hex characters"
        }
    }

    @Volatile
    var observedFingerprint: String? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        throw CertificateException("client authentication not supported")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        require(chain.isNotEmpty()) { "empty server cert chain" }
        val leaf = chain[0]
        val actual = sha256Hex(leaf.encoded)
        val expected = expectedFingerprintSha256
        if (expected != null && !constantTimeHexEquals(actual, expected)) {
            // Do not record observed fingerprint for rejected certs.
            throw FingerprintMismatchException(expected, actual)
        }
        observedFingerprint = actual
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

private fun constantTimeHexEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    return try {
        MessageDigest.isEqual(hexDecode(a), hexDecode(b))
    } catch (_: IllegalArgumentException) {
        false
    }
}

private fun hexDecode(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "odd-length hex string" }
    val out = ByteArray(hex.length / 2)
    for (i in out.indices) {
        val hi = Character.digit(hex[i * 2], 16)
        val lo = Character.digit(hex[i * 2 + 1], 16)
        if (hi < 0 || lo < 0) throw IllegalArgumentException("non-hex char")
        out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
}
