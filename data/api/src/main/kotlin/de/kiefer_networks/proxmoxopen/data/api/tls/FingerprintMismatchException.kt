package de.kiefer_networks.proxmoxopen.data.api.tls

import java.security.cert.CertificateException

class FingerprintMismatchException(
    val expected: String,
    val actual: String,
) : CertificateException("Server fingerprint changed. expected=$expected actual=$actual")
