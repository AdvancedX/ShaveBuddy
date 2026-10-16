package com.shavebuddy.demo

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.shavebuddy.demo.update.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class AndroidUpdateIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun packageInspectorRejectsActualInstalledApkWithSameVersion() {
        try { AndroidApkInspector(context).verify(File(context.applicationInfo.sourceDir)); fail("cannot downgrade or reinstall") }
        catch (e: UpdateException) { assertEquals(UpdateProblem.VERSION, e.problem) }
    }
    @Test fun invalidArchiveNeverPassesPackageInspection() {
        val file = File(context.cacheDir, "invalid-${System.nanoTime()}.apk")
        try {
            file.writeText("not an apk")
            try { AndroidApkInspector(context).verify(file); fail("invalid archive") }
            catch (e: UpdateException) { assertEquals(UpdateProblem.PACKAGE, e.problem) }
        } finally { file.delete() }
    }
    @Test fun fileProviderSharesUpdateCacheButRejectsOtherPrivateFiles() {
        val directory = File(context.cacheDir, "updates").also { it.mkdirs() }
        val file = File(directory, "test-${System.nanoTime()}.apk")
        val outside = File(context.filesDir, "private-${System.nanoTime()}.apk")
        try {
            file.writeBytes(byteArrayOf(0x50, 0x4b, 0x03, 0x04))
            val uri = FileProvider.getUriForFile(context, context.packageName + ".updates", file)
            assertEquals("content", uri.scheme)
            context.contentResolver.openInputStream(uri)!!.use { assertArrayEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04), it.readBytes()) }
            outside.writeText("private")
            try { FileProvider.getUriForFile(context, context.packageName + ".updates", outside); fail("outside cache must not be shared") }
            catch (_: IllegalArgumentException) { /* Narrow FileProvider root enforced. */ }
        } finally { file.delete(); outside.delete() }
    }
}
