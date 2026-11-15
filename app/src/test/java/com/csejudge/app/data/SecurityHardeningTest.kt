package com.csejudge.app.data

import com.csejudge.app.data.model.Options
import com.csejudge.app.data.model.Question
import com.csejudge.app.data.model.QuestionBankFile
import com.csejudge.app.data.repository.QuestionBankRepositoryImpl
import com.csejudge.app.data.repository.QuestionBankValidationException
import com.csejudge.app.data.security.QuestionBankCrypto
import com.csejudge.app.data.security.QuestionBankIntegrityChecker
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Automated Security and Anti-Tampering Test Suite for Stage 12.
 *
 * Validates:
 * - AES-256-GCM authenticated encryption and decryption.
 * - Anti-tampering and authentication tag verification (bit-flip rejection).
 * - Safe handling of corrupted/truncated payloads.
 * - Transparent in-memory loading of encrypted assets in QuestionBankRepository.
 * - Rejection of tampered encrypted assets with QuestionBankValidationException.
 * - Cryptographic SHA-256 asset checksum verification.
 * - Package identity validation.
 */
class SecurityHardeningTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = false }

    private fun createSampleQuestion(id: String = "SEC00001"): Question {
        return Question(
            questionId = id,
            subject = "Security",
            microtheme = "Asset Protection",
            question = listOf("What algorithm provides authenticated encryption?"),
            options = Options("AES-GCM", "DES", "RC4", "MD5"),
            correctAnswer = "A",
            solution = listOf("AES-GCM provides both confidentiality and authentication.")
        )
    }

    @Test
    fun testAes256GcmEncryptionAndDecryptionRoundtrip() {
        val originalPlaintext = "Sensitive Question Bank Content for CSE Judge UPSC Prelims".toByteArray(Charsets.UTF_8)

        val ciphertext = QuestionBankCrypto.encrypt(originalPlaintext)

        // Ensure ciphertext is different from plaintext
        assertFalse(originalPlaintext.contentEquals(ciphertext))
        // IV (12 bytes) + Ciphertext + GCM Tag (16 bytes)
        assertTrue(ciphertext.size >= originalPlaintext.size + 12 + 16)

        val decrypted = QuestionBankCrypto.decrypt(ciphertext)
        assertArrayEquals(originalPlaintext, decrypted)
        assertEquals(String(originalPlaintext, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun testTamperedEncryptedAssetIsRejectedByGcmTag() {
        val originalPlaintext = "Original Question Content".toByteArray(Charsets.UTF_8)
        val ciphertext = QuestionBankCrypto.encrypt(originalPlaintext)

        // Deliberately tamper with one byte in the ciphertext portion
        val tampered = ciphertext.clone()
        val tamperIndex = 15 // Within ciphertext
        tampered[tamperIndex] = (tampered[tamperIndex].toInt() xor 0xFF).toByte()

        // AES-GCM must detect the bit-flip and throw SecurityException
        val exception = assertThrows(SecurityException::class.java) {
            QuestionBankCrypto.decrypt(tampered)
        }
        assertTrue(exception.message?.contains("tampered with or corrupted") == true)
    }

    @Test
    fun testTruncatedPayloadThrowsSecurityException() {
        val tooShort = ByteArray(10) // Less than minimum IV (12) + Tag (16)
        val exception = assertThrows(SecurityException::class.java) {
            QuestionBankCrypto.decrypt(tooShort)
        }
        assertTrue(exception.message?.contains("too short") == true)
    }

    @Test
    fun testQuestionBankRepositoryLoadsEncryptedAssetInMemory() = runTest(testDispatcher) {
        val sampleQ = createSampleQuestion("ENC00001")
        val plaintextJson = json.encodeToString(QuestionBankFile.serializer(), QuestionBankFile(listOf(sampleQ)))
        val encryptedBytes = QuestionBankCrypto.encrypt(plaintextJson.toByteArray(Charsets.UTF_8))

        val repo = QuestionBankRepositoryImpl(
            openAssetStream = { ByteArrayInputStream(encryptedBytes) },
            fileNames = listOf("sample-qid.enc"),
            ioDispatcher = testDispatcher
        )

        val stats = repo.loadQuestionBank()
        assertEquals(1, stats.totalQuestions)
        val loaded = repo.getQuestionById("ENC00001")
        assertNotNull(loaded)
        assertEquals("Security", loaded?.subject)
        assertEquals("A", loaded?.correctAnswer)
    }

    @Test
    fun testTamperedEncryptedAssetFailsSafelyWithValidationException() = runTest(testDispatcher) {
        val sampleQ = createSampleQuestion("ENC00001")
        val plaintextJson = json.encodeToString(QuestionBankFile.serializer(), QuestionBankFile(listOf(sampleQ)))
        val encryptedBytes = QuestionBankCrypto.encrypt(plaintextJson.toByteArray(Charsets.UTF_8))

        // Corrupt byte 20 in the encrypted payload
        val corruptedBytes = encryptedBytes.clone()
        corruptedBytes[20] = (corruptedBytes[20].toInt() xor 0xAA).toByte()

        val repo = QuestionBankRepositoryImpl(
            openAssetStream = { ByteArrayInputStream(corruptedBytes) },
            fileNames = listOf("corrupted-qid.enc"),
            ioDispatcher = testDispatcher
        )

        val exception = assertThrows(QuestionBankValidationException::class.java) {
            runTest(testDispatcher) {
                repo.loadQuestionBank()
            }
        }
        assertTrue(exception.message?.contains("corrupted-qid.enc") == true)
    }

    @Test
    fun testSha256ChecksumVerificationDetectsModifiedData() {
        val validData = "Canonical Question Bank Asset Data".toByteArray(Charsets.UTF_8)
        val expectedSha256 = QuestionBankCrypto.computeSha256(validData)

        // Matching stream verifies true
        assertTrue(QuestionBankIntegrityChecker.verifyAssetChecksum(ByteArrayInputStream(validData), expectedSha256))

        // Modified stream verifies false
        val modifiedData = "Modified Question Bank Asset Data".toByteArray(Charsets.UTF_8)
        assertFalse(QuestionBankIntegrityChecker.verifyAssetChecksum(ByteArrayInputStream(modifiedData), expectedSha256))
    }

    @Test
    fun testPackageIdentityConstantMatchesAppId() {
        assertEquals("com.csejudge.app", QuestionBankIntegrityChecker.EXPECTED_PACKAGE_NAME)
    }
}
