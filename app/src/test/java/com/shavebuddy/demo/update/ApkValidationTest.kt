package com.shavebuddy.demo.update

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkValidationTest {
    @get:Rule val temp = TemporaryFolder()
    private val abc = "abc".toByteArray()
    private val digest = "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private val installed = ApkIdentity("com.shavebuddy.demo", 4, setOf("original-cert"))
    private val candidate = ApkIdentity("com.shavebuddy.demo", 5, setOf("original-cert"))

    @Test fun completeFileIsVerifiedAndProgressIncludesSize() {
        val file = File(temp.root, "update.apk")
        var progress = 0L
        copyVerifiedApk(ByteArrayInputStream(abc), file, 3, digest, onProgress = { received, total ->
            progress = received; assertEquals(3L, total)
        })
        assertArrayEquals(abc, file.readBytes()); assertEquals(3L, progress)
    }
    @Test fun unknownLengthStillVerifiesAndReportsIndeterminateProgress() {
        val file = File(temp.root, "update.apk")
        copyVerifiedApk(ByteArrayInputStream(abc), file, null, null, onProgress = { _, total -> assertNull(total) })
        assertEquals(3L, file.length())
    }
    @Test fun truncatedFileIsDeleted() = rejects(UpdateProblem.LENGTH) { f -> copyVerifiedApk(ByteArrayInputStream(abc), f, 4, null) }
    @Test fun extraBytesAreRejectedAndDeleted() = rejects(UpdateProblem.LENGTH) { f -> copyVerifiedApk(ByteArrayInputStream(abc), f, 2, null) }
    @Test fun wrongDigestIsDeleted() = rejects(UpdateProblem.DIGEST) { f -> copyVerifiedApk(ByteArrayInputStream(abc), f, 3, "sha256:" + "0".repeat(64)) }
    @Test fun malformedDigestIsRejected() = rejects(UpdateProblem.DIGEST) { f -> copyVerifiedApk(ByteArrayInputStream(abc), f, 3, "sha256:broken") }
    @Test fun sizeLimitIsEnforcedWithAndWithoutContentLength() {
        rejects(UpdateProblem.SIZE) { f -> copyVerifiedApk(ByteArrayInputStream(abc), f, 3, null, maxBytes = 2) }
        rejects(UpdateProblem.SIZE) { f -> copyVerifiedApk(ByteArrayInputStream(abc), f, null, null, maxBytes = 2) }
    }
    @Test fun emptyFileIsRejected() = rejects(UpdateProblem.LENGTH) { f -> copyVerifiedApk(ByteArrayInputStream(byteArrayOf()), f, null, null) }
    @Test fun cancellationDeletesPartialFile() {
        val file = File(temp.root, "update.apk")
        try {
            copyVerifiedApk(ByteArrayInputStream(abc), file, 3, null, checkActive = { throw CancellationException() })
            fail("must cancel")
        } catch (_: CancellationException) { assertFalse(file.exists()) }
    }
    @Test fun interruptedStreamDeletesPartialFile() {
        val file = File(temp.root, "update.apk")
        val input = object : InputStream() { override fun read(): Int = throw IOException("interrupted") }
        try { copyVerifiedApk(input, file, null, null); fail("must fail") }
        catch (_: IOException) { assertFalse(file.exists()) }
    }
    @Test fun sameSignatureHigherCodeIsAccepted() { validateApkIdentity(installed, candidate) }
    @Test fun wrongPackageIsRejected() { identityRejects(UpdateProblem.PACKAGE, candidate.copy(packageName = "other.app")) }
    @Test fun sameAndLowerCodesAreRejected() {
        identityRejects(UpdateProblem.VERSION, candidate.copy(versionCode = 4))
        identityRejects(UpdateProblem.VERSION, candidate.copy(versionCode = 3))
    }
    @Test fun differentMissingAndExtraSignersAreRejected() {
        identityRejects(UpdateProblem.SIGNATURE, candidate.copy(signers = setOf("other-cert")))
        identityRejects(UpdateProblem.SIGNATURE, candidate.copy(signers = emptySet()))
        identityRejects(UpdateProblem.SIGNATURE, candidate.copy(signers = setOf("original-cert", "extra")))
    }
    private fun identityRejects(problem: UpdateProblem, apk: ApkIdentity) {
        try { validateApkIdentity(installed, apk); fail("must reject") }
        catch (e: UpdateException) { assertEquals(problem, e.problem) }
    }
    private fun rejects(problem: UpdateProblem, action: (File) -> Unit) {
        val file = File(temp.root, "update.apk")
        try { action(file); fail("must reject") }
        catch (e: UpdateException) { assertEquals(problem, e.problem); assertFalse(file.exists()) }
    }
}
