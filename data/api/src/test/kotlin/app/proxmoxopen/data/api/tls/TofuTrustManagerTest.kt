package app.proxmoxopen.data.api.tls

import io.mockk.every
import io.mockk.mockk
import java.security.cert.X509Certificate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TofuTrustManagerTest {

    private fun fakeCert(bytes: ByteArray): X509Certificate {
        val cert = mockk<X509Certificate>()
        every { cert.encoded } returns bytes
        return cert
    }

    private val certABytes = "cert-a-bytes".toByteArray()
    private val certBBytes = "cert-b-bytes".toByteArray()
    private val fpA = TofuTrustManager.sha256Hex(certABytes)
    private val fpB = TofuTrustManager.sha256Hex(certBBytes)

    @Test
    fun `probe mode accepts any cert and records fingerprint`() {
        val tm = TofuTrustManager(expectedFingerprintSha256 = null)
        tm.checkServerTrusted(arrayOf(fakeCert(certABytes)), "RSA")
        assertNotNull(tm.observedFingerprint)
        assertEquals(fpA, tm.observedFingerprint)
    }

    @Test
    fun `matching fingerprint is accepted`() {
        val tm = TofuTrustManager(expectedFingerprintSha256 = fpA)
        tm.checkServerTrusted(arrayOf(fakeCert(certABytes)), "RSA")
    }

    @Test
    fun `mismatching fingerprint is rejected`() {
        val tm = TofuTrustManager(expectedFingerprintSha256 = fpA)
        val ex = assertThrows(FingerprintMismatchException::class.java) {
            tm.checkServerTrusted(arrayOf(fakeCert(certBBytes)), "RSA")
        }
        assertEquals(fpA, ex.expected)
        assertEquals(fpB, ex.actual)
    }

    @Test
    fun `accepted issuers is empty`() {
        val tm = TofuTrustManager(null)
        assertEquals(0, tm.getAcceptedIssuers().size)
    }
}
