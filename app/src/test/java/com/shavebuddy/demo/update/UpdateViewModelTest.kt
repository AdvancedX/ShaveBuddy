package com.shavebuddy.demo.update

import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.Assert.*
import org.junit.Test

class UpdateViewModelTest {
    @get:Rule val temp = TemporaryFolder()
    private class QueuedDispatcher : CoroutineDispatcher() {
        val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.addLast(block) }
        fun runNext() { tasks.removeFirst().run() }
        fun drain() { while (tasks.isNotEmpty()) runNext() }
    }
    @Test fun cancelAfterCompletedFileBeforeResultDeliveryDeletesCachedApk() {
        val main = QueuedDispatcher()
        val io = QueuedDispatcher()
        val file = File(temp.root, "update.apk")
        val repository = object : UpdateRepository {
            override suspend fun findUpdate(): ReleaseSelection = update
            override suspend fun download(update: ReleaseSelection.Update, progress: (Long, Long?) -> Unit): File = withContext(io) {
                file.writeText("completed download")
                file
            }
            override fun verifyForInstall(file: File) {}
            override fun clearCache() { file.delete() }
        }
        val model = UpdateViewModel(repository, main)
        model.check()
        main.runNext() // Check completes; download is queued on IO.
        io.runNext() // File is complete; delivery is queued on Main.
        assertTrue(file.exists())
        model.cancel()
        main.drain()
        assertEquals(UpdateState.Cancelled, model.state.value)
        assertFalse("cancelled result must not leave an APK", file.exists())
    }

    private val update = ReleaseSelection.Update(PublishedRelease("v0.2.0-demo", false, true, emptyList()),
        ReleaseAsset("ShaveBuddy.apk", "https://github.com/AdvancedX/ShaveBuddy/releases/download/v0.2.0-demo/ShaveBuddy.apk", 3, null, true))
    private class Repository : UpdateRepository {
        var result: ReleaseSelection = ReleaseSelection.Latest
        var failure: UpdateProblem? = null
        var gate: CompletableDeferred<Unit>? = null
        var queries = 0
        var downloads = 0
        override suspend fun findUpdate(): ReleaseSelection { queries++; gate?.await(); failure?.let { throw UpdateException(it) }; return result }
        override suspend fun download(update: ReleaseSelection.Update, progress: (Long, Long?) -> Unit): File {
            downloads++; progress(1, 3); return File("verified.apk")
        }
        override fun verifyForInstall(file: File) {}
        override fun clearCache() {}
    }
    @Test fun idleDoesNotQueryAndExplicitCheckCanReportLatest() {
        val repo = Repository(); val model = UpdateViewModel(repo, Dispatchers.Unconfined)
        assertEquals(0, repo.queries); assertEquals(UpdateState.Idle, model.state.value)
        model.check(); assertEquals(UpdateState.Latest, model.state.value); assertEquals(0, repo.downloads)
    }
    @Test fun updateDownloadsAutomaticallyThenWaitsForInstallation() {
        val repo = Repository().also { it.result = update }; val model = UpdateViewModel(repo, Dispatchers.Unconfined)
        model.check(); assertEquals(1, repo.downloads)
        val ready = model.state.value as UpdateState.Ready
        assertEquals("v0.2.0-demo", ready.update.release.tag); assertEquals(InstallStage.AUTOMATIC, ready.stage)
        model.waitForPermission(); assertEquals(InstallStage.PERMISSION, (model.state.value as UpdateState.Ready).stage)
        assertEquals(File("verified.apk"), model.prepareInstallation())
        assertEquals(InstallStage.OPENED, (model.state.value as UpdateState.Ready).stage)
    }
    @Test fun duplicateCheckDoesNotCreateConcurrentQueriesAndCancelAllowsRetry() {
        val gate = CompletableDeferred<Unit>(); val repo = Repository().also { it.gate = gate }
        val model = UpdateViewModel(repo, Dispatchers.Unconfined)
        model.check(); assertEquals(UpdateState.Checking, model.state.value)
        model.check(); assertEquals(1, repo.queries)
        model.cancel(); assertEquals(UpdateState.Cancelled, model.state.value)
        repo.gate = null; model.check(); assertEquals(UpdateState.Latest, model.state.value)
    }
    @Test fun noPackageAndNetworkFailureAreNeverReportedAsLatestAndCanRetry() {
        val repo = Repository().also { it.result = ReleaseSelection.NoPackages }
        val model = UpdateViewModel(repo, Dispatchers.Unconfined)
        model.check(); assertEquals(UpdateState.Failure(UpdateProblem.NO_PACKAGES), model.state.value)
        repo.failure = UpdateProblem.NETWORK; model.check(); assertEquals(UpdateState.Failure(UpdateProblem.NETWORK), model.state.value)
        repo.failure = null; repo.result = ReleaseSelection.Latest; model.check(); assertEquals(UpdateState.Latest, model.state.value)
    }
    @Test fun installerFailureOffersRetryAsFailure() {
        val repo = Repository().also { it.result = update }; val model = UpdateViewModel(repo, Dispatchers.Unconfined)
        model.check(); model.installationFailed(); assertEquals(UpdateState.Failure(UpdateProblem.INSTALL), model.state.value)
    }
}
