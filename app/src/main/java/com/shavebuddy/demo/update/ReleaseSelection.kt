package com.shavebuddy.demo.update

import java.net.URI

data class ReleaseAsset(val name: String, val url: String, val size: Long?, val digest: String?, val uploaded: Boolean)
data class PublishedRelease(val tag: String, val draft: Boolean, val published: Boolean, val assets: List<ReleaseAsset>)
sealed interface ReleaseSelection {
    data class Update(val release: PublishedRelease, val asset: ReleaseAsset) : ReleaseSelection
    data object Latest : ReleaseSelection
    data object NoPackages : ReleaseSelection
}

private data class NumericVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<NumericVersion> {
    override fun compareTo(other: NumericVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    companion object {
        fun parse(tag: String): NumericVersion? {
            val parts = Regex("v([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-demo)?").matchEntire(tag)?.groupValues ?: return null
            return NumericVersion(parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null, parts[3].toIntOrNull() ?: return null)
        }
    }
}

fun isProjectAssetUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && uri.host.equals("github.com", ignoreCase = true) && uri.userInfo == null &&
        (uri.port == -1 || uri.port == 443) && uri.path.startsWith("/AdvancedX/ShaveBuddy/releases/download/")
}.getOrDefault(false)

fun selectUpdate(releases: List<PublishedRelease>, currentVersion: String): ReleaseSelection {
    val current = NumericVersion.parse("v" + currentVersion.removePrefix("v")) ?: throw UpdateException(UpdateProblem.VERSION)
    val candidates = releases.mapNotNull { release ->
        if (release.draft || !release.published) return@mapNotNull null
        val version = NumericVersion.parse(release.tag) ?: return@mapNotNull null
        val assets = release.assets.filter { asset ->
            asset.uploaded && Regex("ShaveBuddy(?:[-_.].*)?\\.apk", RegexOption.IGNORE_CASE).matches(asset.name) &&
                isProjectAssetUrl(asset.url) && (asset.size == null || asset.size > 0)
        }
        val asset = assets.singleOrNull { it.name.equals("ShaveBuddy.apk", ignoreCase = true) } ?: assets.singleOrNull() ?: return@mapNotNull null
        Triple(version, release, asset)
    }
    val newest = candidates.maxByOrNull { it.first } ?: return ReleaseSelection.NoPackages
    return if (newest.first > current) ReleaseSelection.Update(newest.second, newest.third) else ReleaseSelection.Latest
}
