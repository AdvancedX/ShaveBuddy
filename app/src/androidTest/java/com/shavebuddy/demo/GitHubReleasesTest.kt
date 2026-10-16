package com.shavebuddy.demo

import com.shavebuddy.demo.update.*
import org.junit.Assert.*
import org.junit.Test

class GitHubReleasesTest {
    private val json = """[{"tag_name":"v0.2.0-demo","draft":false,"prerelease":true,"published_at":"2026-10-10T00:00:00Z","assets":[{"name":"ShaveBuddy.apk","state":"uploaded","size":3,"digest":"sha256:abc","browser_download_url":"https://github.com/AdvancedX/ShaveBuddy/releases/download/v0.2.0-demo/ShaveBuddy.apk"}]}]"""
    @Test fun publishedPrereleaseAndDigestAreParsed() {
        val parsed = parseReleasePage(json)
        val selected = selectUpdate(parsed, "0.1.3-demo") as ReleaseSelection.Update
        assertEquals("sha256:abc", selected.asset.digest)
    }
    @Test fun nullPublishedTimeIsNotAnUpdate() {
        assertEquals(ReleaseSelection.NoPackages, selectUpdate(parseReleasePage(json.replace("\"2026-10-10T00:00:00Z\"", "null")), "0.1.3-demo"))
    }
    @Test fun malformedMetadataFailsInsteadOfReportingLatest() {
        for (value in listOf("not json", "{}", "[{\"tag_name\":\"v0.2.0\"}]")) {
            try { parseReleasePage(value); fail("invalid metadata") }
            catch (e: UpdateException) { assertEquals(UpdateProblem.METADATA, e.problem) }
        }
    }
}
