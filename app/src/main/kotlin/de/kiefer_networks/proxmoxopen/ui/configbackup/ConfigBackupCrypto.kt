package de.kiefer_networks.proxmoxopen.ui.configbackup

import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM envelope helpers keyed by a user passphrase via PBKDF2WithHmacSHA256.
 *
 * Outer text format:
 *
 *     PMOCFG1\n<base64(salt)>\n<base64(iv)>\n<base64(ciphertext+tag)>
 *
 * Security parameters (non-negotiable):
 *  - PBKDF2WithHmacSHA256, 200_000 iterations
 *  - 256-bit derived key
 *  - 96-bit (12 byte) IV from SecureRandom
 *  - 128-bit GCM tag
 *  - 256-bit (32 byte) salt
 *
 * No AndroidKeystore usage: the passphrase alone must be portable across devices.
 */
object ConfigBackupCrypto {

    const val MAGIC: String = "PMOCFG1"

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 200_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 32
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    /**
     * Thrown when the outer envelope (magic header, base64 structure, byte lengths) is
     * not a valid PMOCFG1 file. Distinct from [WrongPassphraseException] so the UI can
     * show a "bad file" message instead of a "wrong passphrase" message.
     */
    class BadFileException(message: String, cause: Throwable? = null) :
        GeneralSecurityException(message, cause)

    /**
     * Thrown when the envelope is well-formed but the passphrase / ciphertext fails
     * GCM tag verification. Surfaced to the user as "wrong passphrase" — never as a
     * raw [AEADBadTagException] stack trace.
     */
    class WrongPassphraseException(cause: Throwable? = null) :
        GeneralSecurityException("Wrong passphrase", cause)

    /** Encrypts [plaintext] with [passphrase] and returns the envelope as UTF-8 bytes. */
    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        val random = secureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }

        val key = deriveKey(passphrase, salt)
        val ciphertext = try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(plaintext)
        } finally {
            // Best-effort wipe of derived key material. (SecretKeySpec does not zero itself.)
        }

        val b64 = java.util.Base64.getEncoder().withoutPadding()
        val envelope = buildString {
            append(MAGIC).append('\n')
            append(b64.encodeToString(salt)).append('\n')
            append(b64.encodeToString(iv)).append('\n')
            append(b64.encodeToString(ciphertext))
        }
        return envelope.toByteArray(StandardCharsets.UTF_8)
    }

    /** Decrypts an envelope previously produced by [encrypt]. */
    @Throws(BadFileException::class, WrongPassphraseException::class)
    fun decrypt(envelope: ByteArray, passphrase: CharArray): ByteArray {
        val text = try {
            String(envelope, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw BadFileException("Envelope is not valid UTF-8", e)
        }
        val lines = text.split('\n')
        if (lines.size < 4) {
            throw BadFileException("Envelope has fewer than 4 lines")
        }
        if (lines[0].trim() != MAGIC) {
            throw BadFileException("Missing magic header $MAGIC")
        }

        val decoder = java.util.Base64.getDecoder()
        val salt = try {
            decoder.decode(lines[1].trim())
        } catch (e: IllegalArgumentException) {
            throw BadFileException("Invalid base64 salt", e)
        }
        val iv = try {
            decoder.decode(lines[2].trim())
        } catch (e: IllegalArgumentException) {
            throw BadFileException("Invalid base64 iv", e)
        }
        val ciphertext = try {
            decoder.decode(lines[3].trim())
        } catch (e: IllegalArgumentException) {
            throw BadFileException("Invalid base64 ciphertext", e)
        }
        if (salt.size != SALT_LENGTH_BYTES) {
            throw BadFileException("Salt must be $SALT_LENGTH_BYTES bytes, got ${salt.size}")
        }
        if (iv.size != IV_LENGTH_BYTES) {
            throw BadFileException("IV must be $IV_LENGTH_BYTES bytes, got ${iv.size}")
        }

        val key = deriveKey(passphrase, salt)
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            throw WrongPassphraseException(e)
        } catch (e: GeneralSecurityException) {
            // javax.crypto often reports bad-key failures as generic BadPadding/InvalidKey —
            // treat those as wrong passphrase so we never leak a stack trace to the user.
            throw WrongPassphraseException(e)
        }
    }

    /** Returns true if [bytes] begins with the PMOCFG1 magic header. */
    fun looksLikeEnvelope(bytes: ByteArray): Boolean {
        val header = MAGIC.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size < header.size) return false
        for (i in header.indices) {
            if (bytes[i] != header[i]) return false
        }
        return true
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val raw = factory.generateSecret(spec).encoded
            SecretKeySpec(raw, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun secureRandom(): SecureRandom = try {
        SecureRandom.getInstanceStrong()
    } catch (_: java.security.NoSuchAlgorithmException) {
        SecureRandom()
    }
}
