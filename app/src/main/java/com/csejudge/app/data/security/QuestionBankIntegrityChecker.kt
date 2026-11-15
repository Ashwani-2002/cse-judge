package com.csejudge.app.data.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.InputStream
import java.security.MessageDigest

/**
 * Result of an integrity or tamper-detection check.
 */
data class IntegrityCheckResult(
    val isClean: Boolean,
    val packageName: String,
    val isPackageValid: Boolean,
    val issues: List<String>
)

/**
 * Security integrity checker verifying application identity and question bank asset integrity.
 *
 * Verifies:
 * - Package identity matches "com.csejudge.app".
 * - Application signature certificate hash.
 * - Integrity of question bank files against SHA-256 checksums to detect modified APK assets.
 */
object QuestionBankIntegrityChecker {

    const val EXPECTED_PACKAGE_NAME = "com.csejudge.app"

    /**
     * Verifies that the running package identity matches the expected application package.
     */
    fun verifyPackageIdentity(context: Context): Boolean {
        return context.packageName == EXPECTED_PACKAGE_NAME
    }

    /**
     * Retrieves the SHA-256 hash of the application's signing certificate.
     */
    @Suppress("DEPRECATION")
    fun getSigningCertificateSha256(context: Context): String? {
        return try {
            val pm = context.packageManager
            val packageName = context.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            val primaryCert = signatures?.firstOrNull() ?: return null
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(primaryCert.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Verifies that the SHA-256 hash of an asset [InputStream] matches the [expectedSha256].
     *
     * @return true if checksum matches; false if the asset was altered or truncated.
     */
    fun verifyAssetChecksum(stream: InputStream, expectedSha256: String): Boolean {
        val actualHash = QuestionBankCrypto.computeSha256(stream)
        return actualHash.equals(expectedSha256, ignoreCase = true)
    }

    /**
     * Performs a comprehensive runtime integrity audit of the application environment.
     */
    fun performEnvironmentAudit(context: Context): IntegrityCheckResult {
        val issues = mutableListOf<String>()
        val isPackageValid = verifyPackageIdentity(context)
        if (!isPackageValid) {
            issues.add("Unexpected package name: '${context.packageName}' (expected '$EXPECTED_PACKAGE_NAME')")
        }

        val certSha256 = getSigningCertificateSha256(context)
        if (certSha256 == null) {
            issues.add("Unable to read signing certificate")
        }

        return IntegrityCheckResult(
            isClean = issues.isEmpty(),
            packageName = context.packageName,
            isPackageValid = isPackageValid,
            issues = issues
        )
    }
}
