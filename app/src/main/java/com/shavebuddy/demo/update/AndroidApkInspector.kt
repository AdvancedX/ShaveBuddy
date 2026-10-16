package com.shavebuddy.demo.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

class AndroidApkInspector(private val context: Context) {
    @Suppress("DEPRECATION")
    private val flags: Int get() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    fun verify(file: File) {
        if (!file.isFile || file.length() == 0L) throw UpdateException(UpdateProblem.LENGTH)
        val manager = context.packageManager
        val installed = manager.getPackageInfo(context.packageName, flags)
        val candidate = manager.getPackageArchiveInfo(file.absolutePath, flags) ?: throw UpdateException(UpdateProblem.PACKAGE)
        validateApkIdentity(identity(installed), identity(candidate))
    }

    @Suppress("DEPRECATION")
    private fun identity(info: PackageInfo): ApkIdentity {
        // Compare current signers exactly. Rotated or unrelated signing identities are not accepted by this demo.
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val signers = signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return ApkIdentity(info.packageName, code, signers)
    }
}
