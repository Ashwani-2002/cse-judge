package com.csejudge.app.data.security

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic utility providing AES-256-GCM authenticated encryption,
 * decryption, and SHA-256 integrity verification for question bank assets.
 *
 * Algorithm: AES/GCM/NoPadding (Authenticated Encryption with Associated Data).
 * Key length: 256 bits (32 bytes).
 * IV length: 96 bits (12 bytes, NIST recommended for GCM).
 * Tag length: 128 bits (16 bytes, provides cryptographic integrity and authenticity).
 *
 * Security properties:
 * - Confidentiality: Protects question bank assets from casual extraction via APK unzip.
 * - Authenticity / Anti-Tampering: GCM authentication tag verification ensures any
 *   modification, bit-flip, or truncation in an encrypted asset is immediately detected
 *   and rejected during decryption.
 * - In-Memory Decryption: Decrypted data is held strictly in RAM and never written to disk
 *   or external storage.
 *
 * Known Architectural Limitation:
 * In a purely offline Android application, the cryptographic key or derivation logic must
 * reside within the APK. A determined reverse-engineer using dynamic instrumentation (e.g. Frida)
 * or memory dumping can theoretically recover the key or the decrypted in-memory Question objects.
 * This implementation provides practical resistance against casual extraction and tampering,
 * but cannot replace a server-controlled DRM delivery architecture.
 */
object QuestionBankCrypto {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ALGORITHM = "AES"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    // Internal application seed used for deterministic key derivation
    private val INTERNAL_SALT = byteArrayOf(
        0x43, 0x53, 0x45, 0x4A, 0x75, 0x64, 0x67, 0x65, // "CSEJudge"
        0x50, 0x72, 0x65, 0x6C, 0x69, 0x6D, 0x73, 0x21  // "Prelims!"
    )

    /**
     * Derives a 256-bit AES key using SHA-256 over internal application seed material.
     */
    fun deriveKey(appContextTag: String = "com.csejudge.app.questionbank.v1"): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(INTERNAL_SALT)
        digest.update(appContextTag.toByteArray(Charsets.UTF_8))
        val keyBytes = digest.digest()
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypts plaintext data using AES-256-GCM.
     * The generated 12-byte IV is prepended to the returned ciphertext.
     *
     * Format: [12-byte IV] + [Ciphertext + 16-byte GCM Tag]
     */
    fun encrypt(plaintext: ByteArray, key: SecretKey = deriveKey()): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val ciphertext = cipher.doFinal(plaintext)
        val output = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, output, 0, iv.size)
        System.arraycopy(ciphertext, 0, output, iv.size, ciphertext.size)
        return output
    }

    /**
     * Decrypts ciphertext previously encrypted with [encrypt].
     * Extracts the 12-byte IV from the prefix and verifies the 16-byte GCM authentication tag.
     * Throws [SecurityException] if ciphertext has been tampered with or corrupted.
     */
    fun decrypt(encryptedBytes: ByteArray, key: SecretKey = deriveKey()): ByteArray {
        if (encryptedBytes.size < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw SecurityException("Corrupted encrypted asset: Payload too short")
        }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH_BYTES)

        val ciphertextOffset = GCM_IV_LENGTH_BYTES
        val ciphertextLength = encryptedBytes.size - GCM_IV_LENGTH_BYTES

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return try {
            cipher.doFinal(encryptedBytes, ciphertextOffset, ciphertextLength)
        } catch (e: Exception) {
            throw SecurityException("Decryption authentication failed: Asset has been tampered with or corrupted", e)
        }
    }

    /**
     * Decrypts an encrypted [InputStream] in memory and returns a [ByteArrayInputStream]
     * of the plaintext bytes. Never writes plaintext to disk.
     */
    fun decryptStream(encryptedStream: InputStream, key: SecretKey = deriveKey()): InputStream {
        val encryptedBytes = encryptedStream.use { it.readBytes() }
        val decryptedBytes = decrypt(encryptedBytes, key)
        return ByteArrayInputStream(decryptedBytes)
    }

    /**
     * Computes the cryptographic SHA-256 hash of the given [bytes].
     */
    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes the cryptographic SHA-256 hash of an [InputStream].
     */
    fun computeSha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        stream.use { input ->
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
