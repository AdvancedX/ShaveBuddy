package com.shavebuddy.demo.update

import org.junit.Assert.*
import org.junit.Test

class ReleaseSelectionTest {
    private fun release(tag: String, assets: List<ReleaseAsset> = listOf(asset()), draft: Boolean = false, published: Boolean = true) =
        PublishedRelease(tag, draft, published, assets)
    private fun asset(name: String = "ShaveBuddy.apk", url: String = "https://github.com/AdvancedX/ShaveBuddy/releases/download/v0.2.0-demo/$name", size: Long = 20) =
        ReleaseAsset(name, url, size, null, true)

    @Test fun numericHighestDemoReleaseWinsRegardlessOfListOrder() {
        val result = selectUpdate(listOf(release("v0.9.9-demo"), release("v0.10.0-demo"), release("v0.2.0")), "0.1.3-demo")
        assertEquals("v0.10.0-demo", (result as ReleaseSelection.Update).release.tag)
    }
    @Test fun sameAndOlderVersionsDoNotDownload() {
        assertEquals(ReleaseSelection.Latest, selectUpdate(listOf(release("v0.1.3"), release("v0.1.2-demo")), "0.1.3-demo"))
    }
    @Test fun draftsUnpublishedAndMalformedTagsDoNotTriggerUpdates() {
        val invalid = listOf(release("v9.0.0", draft = true), release("v8.0.0", published = false),
            release("v7.0.0-beta"), release("0.6.0-demo"), release("0.1.2-release"), release("v999999999999999.0.0"), release("v1.0.0\n"))
        assertEquals(ReleaseSelection.NoPackages, selectUpdate(invalid, "0.1.3-demo"))
    }
    @Test fun missingWrongInsecureOrForeignApksCannotTriggerDownload() {
        val invalid = listOf(release("v1.0.0", emptyList()), release("v2.0.0", listOf(asset("Other.apk"))),
            release("v3.0.0", listOf(asset(url = "http://github.com/AdvancedX/ShaveBuddy/releases/download/v3/a.apk"))),
            release("v4.0.0", listOf(asset(url = "https://example.com/ShaveBuddy.apk"))),
            release("v5.0.0", listOf(asset(size = 0))), release("v6.0.0", listOf(asset().copy(uploaded = false))))
        assertEquals(ReleaseSelection.NoPackages, selectUpdate(invalid, "0.1.3-demo"))
    }
    @Test fun missingApkOnNewerReleaseDoesNotHideUsableRelease() {
        val result = selectUpdate(listOf(release("v9.0.0", emptyList()), release("v0.2.0-demo")), "0.1.3-demo")
        assertEquals("v0.2.0-demo", (result as ReleaseSelection.Update).release.tag)
    }
    @Test fun ambiguousApksAreRejectedUnlessCanonicalApkExists() {
        val variants = listOf(asset("ShaveBuddy-arm.apk"), asset("ShaveBuddy-x86.apk"))
        assertEquals(ReleaseSelection.NoPackages, selectUpdate(listOf(release("v0.2.0", variants)), "0.1.3-demo"))
        val result = selectUpdate(listOf(release("v0.2.0", variants + asset())), "0.1.3-demo") as ReleaseSelection.Update
        assertEquals("ShaveBuddy.apk", result.asset.name)
    }
}
