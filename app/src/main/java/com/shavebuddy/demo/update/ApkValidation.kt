package com.shavebuddy.demo.update

import java.io.File
import java.io.InputStream
import java.io.IOException
import java.security.MessageDigest

enum class UpdateProblem { NETWORK, RATE_LIMIT, METADATA, NO_PACKAGES, SIZE, LENGTH, DIGEST, STORAGE, PACKAGE, VERSION, SIGNATURE, INSTALL }
class UpdateException(val problem: UpdateProblem, cause: Throwable? = null) : IOException(problem.name, cause)
data class ApkIdentity(val packageName: String, val versionCode: Long, val signers: Set<String>)

fun validateApkIdentity(installed: ApkIdentity, candidate: ApkIdentity) {
    if (installed.packageName != "com.shavebuddy.demo" || candidate.packageName != installed.packageName) throw UpdateException(UpdateProblem.PACKAGE)
    if (candidate.versionCode <= installed.versionCode) throw UpdateException(UpdateProblem.VERSION)
    if (installed.signers.isEmpty() || candidate.signers != installed.signers) throw UpdateException(UpdateProblem.SIGNATURE)
}

/** The caller owns the input; failed/cancelled copies never leave an installable file. */
fun copyVerifiedApk(input: InputStream, destination: File, expectedSize: Long?, expectedDigest: String?, maxBytes: Long = 100L * 1024 * 1024,
    onProgress: (Long, Long?) -> Unit = { _, _ -> }, checkActive: () -> Unit = {}) {
    var complete = false
    try {
        if (expectedSize != null && (expectedSize <= 0 || expectedSize > maxBytes)) throw UpdateException(UpdateProblem.SIZE)
        val expectedHash = expectedDigest?.let {
            Regex("sha256:([0-9a-fA-F]{64})").matchEntire(it)?.groupValues?.get(1)?.lowercase() ?: throw UpdateException(UpdateProblem.DIGEST)
        }
        val hash = MessageDigest.getInstance("SHA-256")
        var received = 0L
        checkActive()
        destination.outputStream().use { output ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                checkActive()
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                received += read
                if (received > maxBytes) throw UpdateException(UpdateProblem.SIZE)
                if (expectedSize != null && received > expectedSize) throw UpdateException(UpdateProblem.LENGTH)
                output.write(buffer, 0, read)
                hash.update(buffer, 0, read)
                onProgress(received, expectedSize)
            }
        }
        checkActive()
        if (received == 0L || (expectedSize != null && received != expectedSize)) throw UpdateException(UpdateProblem.LENGTH)
        val actualHash = hash.digest().joinToString("") { "%02x".format(it) }
        if (expectedHash != null && actualHash != expectedHash) throw UpdateException(UpdateProblem.DIGEST)
        complete = true
    } finally { if (!complete) destination.delete() }
}
