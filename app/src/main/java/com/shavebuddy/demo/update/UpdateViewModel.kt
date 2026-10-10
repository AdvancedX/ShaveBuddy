package com.shavebuddy.demo.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

interface UpdateRepository {
    suspend fun findUpdate(): ReleaseSelection
    suspend fun download(update: ReleaseSelection.Update, progress: (Long, Long?) -> Unit): File
    fun verifyForInstall(file: File)
    fun clearCache()
    fun cancelNetwork() {}
}
enum class InstallStage { AUTOMATIC, PERMISSION, OPENED }
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Latest : UpdateState
    data object Cancelling : UpdateState
    data object Cancelled : UpdateState
    data class Downloading(val update: ReleaseSelection.Update, val received: Long, val total: Long?) : UpdateState
    data class Ready(val update: ReleaseSelection.Update, val file: File, val stage: InstallStage = InstallStage.AUTOMATIC) : UpdateState
    data class Failure(val problem: UpdateProblem) : UpdateState
}
class UpdateViewModel(private val repository: UpdateRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : ViewModel() {
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = mutableState.asStateFlow()
    private var operation: Job? = null

    init { repository.clearCache() }

    fun check() {
        if (operation?.isCompleted == false) return
        operation = viewModelScope.launch(dispatcher) {
            mutableState.value = UpdateState.Checking
            try {
                repository.clearCache()
                when (val selection = repository.findUpdate()) {
                    ReleaseSelection.Latest -> mutableState.value = UpdateState.Latest
                    ReleaseSelection.NoPackages -> mutableState.value = UpdateState.Failure(UpdateProblem.NO_PACKAGES)
                    is ReleaseSelection.Update -> {
                        mutableState.value = UpdateState.Downloading(selection, 0, selection.asset.size)
                        val file = repository.download(selection) { received, total ->
                            mutableState.value = UpdateState.Downloading(selection, received, total)
                        }
                        mutableState.value = UpdateState.Ready(selection, file)
                    }
                }
            } catch (_: CancellationException) {
                // IO may finish a file just before prompt cancellation discards its result.
                repository.clearCache()
                mutableState.value = UpdateState.Cancelled
            } catch (e: UpdateException) {
                mutableState.value = UpdateState.Failure(e.problem)
            } catch (_: java.io.IOException) {
                mutableState.value = UpdateState.Failure(UpdateProblem.NETWORK)
            }
        }
    }

    fun cancel() {
        if (operation?.isCompleted == false) {
            mutableState.value = UpdateState.Cancelling
            operation?.cancel()
            repository.cancelNetwork()
        }
    }

    fun waitForPermission() {
        (mutableState.value as? UpdateState.Ready)?.let { mutableState.value = it.copy(stage = InstallStage.PERMISSION) }
    }

    fun prepareInstallation(): File? {
        val ready = mutableState.value as? UpdateState.Ready ?: return null
        return try {
            repository.verifyForInstall(ready.file)
            mutableState.value = ready.copy(stage = InstallStage.OPENED)
            ready.file
        } catch (e: UpdateException) {
            repository.clearCache()
            mutableState.value = UpdateState.Failure(e.problem)
            null
        }
    }

    fun installationFailed() { mutableState.value = UpdateState.Failure(UpdateProblem.INSTALL) }

    override fun onCleared() {
        repository.cancelNetwork()
        repository.clearCache()
    }
}
