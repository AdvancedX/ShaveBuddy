package com.shavebuddy.demo.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

fun parseReleasePage(json: String): List<PublishedRelease> = try {
    val array = JSONArray(json)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        val assets = item.getJSONArray("assets")
        PublishedRelease(item.getString("tag_name"), item.getBoolean("draft"),
            !item.isNull("published_at") && item.getString("published_at").isNotBlank(),
            (0 until assets.length()).map { assetIndex ->
                val asset = assets.getJSONObject(assetIndex)
                ReleaseAsset(asset.getString("name"), asset.getString("browser_download_url"),
                    asset.getLong("size"), if (asset.isNull("digest")) null else asset.getString("digest"), asset.getString("state") == "uploaded")
            })
    }
} catch (e: JSONException) { throw UpdateException(UpdateProblem.METADATA, e) }

class GitHubUpdateRepository(context: Context) : UpdateRepository {
    private val app = context.applicationContext
    private val cache = File(app.cacheDir, "updates")
    private val inspector = AndroidApkInspector(app)
    private val activeConnection = AtomicReference<HttpURLConnection?>()
    private val currentVersion = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()

    override suspend fun findUpdate(): ReleaseSelection = withContext(Dispatchers.IO) {
        val releases = mutableListOf<PublishedRelease>()
        // A full page implies another page. Never report Latest from an incomplete listing.
        for (page in 1..20) {
            currentCoroutineContext().ensureActive()
            val connection = openHttps("https://api.github.com/repos/AdvancedX/ShaveBuddy/releases?per_page=100&page=$page", metadata = true)
            val items = try {
                connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (output.size() + read > 2 * 1024 * 1024) throw UpdateException(UpdateProblem.METADATA)
                        output.write(buffer, 0, read)
                    }
                    parseReleasePage(output.toString("UTF-8"))
                }
            } finally { close(connection) }
            releases += items
            if (items.size < 100) return@withContext selectUpdate(releases, currentVersion)
        }
        throw UpdateException(UpdateProblem.METADATA)
    }

    override suspend fun download(update: ReleaseSelection.Update, progress: (Long, Long?) -> Unit): File = withContext(Dispatchers.IO) {
        if (!isProjectAssetUrl(update.asset.url)) throw UpdateException(UpdateProblem.METADATA)
        if (!cache.isDirectory && !cache.mkdirs()) throw UpdateException(UpdateProblem.STORAGE)
        val partial = File(cache, "update.part")
        val apk = File(cache, "update.apk")
        var complete = false
        var connection: HttpURLConnection? = null
        try {
            val expected = update.asset.size
            if (expected != null && expected > 100L * 1024 * 1024) throw UpdateException(UpdateProblem.SIZE)
            if (expected != null && cache.usableSpace < expected + 1024 * 1024) throw UpdateException(UpdateProblem.STORAGE)
            val opened = openHttps(update.asset.url, metadata = false)
            connection = opened
            val length = opened.contentLengthLong.takeIf { it > 0 }
            if (expected != null && length != null && expected != length) throw UpdateException(UpdateProblem.LENGTH)
            val coroutine = currentCoroutineContext()
            val deadline = System.nanoTime() + 5L * 60 * 1_000_000_000
            var lastProgress = 0L
            opened.inputStream.use { input ->
                copyVerifiedApk(input, partial, expected ?: length, update.asset.digest,
                    onProgress = { received, total ->
                        val now = System.nanoTime()
                        if (now - lastProgress >= 100_000_000 || received == total) {
                            progress(received, total); lastProgress = now
                        }
                    },
                    checkActive = {
                        coroutine.ensureActive()
                        if (System.nanoTime() > deadline) throw UpdateException(UpdateProblem.NETWORK)
                    })
            }
            coroutine.ensureActive()
            inspector.verify(partial)
            if (!partial.renameTo(apk)) throw UpdateException(UpdateProblem.STORAGE)
            complete = true
            apk
        } catch (e: IOException) {
            currentCoroutineContext().ensureActive()
            if (e is UpdateException) throw e
            if (cache.usableSpace < 1024 * 1024) throw UpdateException(UpdateProblem.STORAGE, e)
            throw UpdateException(UpdateProblem.NETWORK, e)
        } finally {
            connection?.let(::close)
            partial.delete()
            if (!complete) apk.delete()
        }
    }

    override fun verifyForInstall(file: File) = inspector.verify(file)
    override fun clearCache() { cache.listFiles()?.forEach { it.delete() } }
    override fun cancelNetwork() { activeConnection.getAndSet(null)?.disconnect() }

    private suspend fun openHttps(initial: String, metadata: Boolean): HttpURLConnection {
        var url = URL(initial)
        repeat(6) {
            currentCoroutineContext().ensureActive()
            val allowed = if (metadata) setOf("api.github.com") else setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
            if (url.protocol != "https" || url.host.lowercase() !in allowed || url.userInfo != null || url.port !in listOf(-1, 443)) throw UpdateException(UpdateProblem.METADATA)
            val connection = url.openConnection() as HttpURLConnection
            activeConnection.set(connection)
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "ShaveBuddy/$currentVersion")
            connection.setRequestProperty("Accept", if (metadata) "application/vnd.github+json" else "application/octet-stream")
            if (metadata) connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            try {
                when (connection.responseCode) {
                    200 -> return connection
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw UpdateException(UpdateProblem.METADATA)
                        url = URL(url, location)
                        close(connection)
                    }
                    403, 429 -> throw UpdateException(UpdateProblem.RATE_LIMIT)
                    404 -> throw UpdateException(UpdateProblem.NO_PACKAGES)
                    else -> throw UpdateException(UpdateProblem.NETWORK)
                }
            } catch (e: Exception) { close(connection); throw e }
        }
        throw UpdateException(UpdateProblem.NETWORK)
    }

    private fun close(connection: HttpURLConnection) {
        activeConnection.compareAndSet(connection, null)
        connection.disconnect()
    }
}
