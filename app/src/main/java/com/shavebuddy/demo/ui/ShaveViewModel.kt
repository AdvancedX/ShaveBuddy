package com.shavebuddy.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shavebuddy.demo.data.ShaveRepository
import com.shavebuddy.demo.domain.ShaveSnapshot
import com.shavebuddy.demo.domain.ShaveRatings
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShaveUiState(
    val snapshot: ShaveSnapshot? = null,
    val today: LocalDate = LocalDate.now(),
    val busy: Boolean = false,
    val error: Boolean = false,
    val ratingEventId: String? = null,
)

class ShaveViewModel(private val repository: ShaveRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ShaveUiState())
    val state = mutableState.asStateFlow()
    private var observer: Job? = null

    init { reload() }

    fun reload() {
        observer?.cancel()
        mutableState.update { it.copy(error = false) }
        observer = viewModelScope.launch {
            try {
                repository.snapshots.collect { snapshot ->
                    mutableState.update { it.copy(snapshot = snapshot, today = LocalDate.now()) }
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { mutableState.update { it.copy(error = true) } }
        }
    }

    fun refreshDate() { mutableState.update { it.copy(today = LocalDate.now()) } }
    fun dismissError() { mutableState.update { it.copy(error = false) } }

    private fun mutate(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try {
                action()
                val snapshot = repository.read()
                mutableState.update { it.copy(snapshot = snapshot, today = LocalDate.now()) }
                onSuccess()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { mutableState.update { it.copy(error = true) } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun setup(name: String, installedOn: LocalDate, uses: Int?, days: Int?, interval: Int) =
        mutate { repository.setup(name, installedOn, uses, days, interval) }
    fun record(date: LocalDate) = recordEvent(date, false)
    fun backfill(date: LocalDate, adjustFirstInstallation: Boolean) =
        recordEvent(date, adjustFirstInstallation)

    private fun recordEvent(date: LocalDate, adjustFirstInstallation: Boolean) {
        var id: String? = null
        mutate(onSuccess = { mutableState.update { it.copy(ratingEventId = id) } }) {
            id = repository.addEvent(date, adjustFirstInstallation)
        }
    }

    fun skipRatings() {
        if (!mutableState.value.busy) mutableState.update { it.copy(ratingEventId = null, error = false) }
    }

    fun saveRatings(ratings: ShaveRatings) {
        val id = mutableState.value.ratingEventId ?: return
        mutate(onSuccess = { mutableState.update { it.copy(ratingEventId = null) } }) { repository.rateEvent(id, ratings) }
    }
    fun delete(id: String) = mutate { repository.deleteEvent(id) }
    fun replace() = mutate { repository.replaceBlade() }
    fun updateSettings(name: String, uses: Int?, days: Int?, interval: Int, onSuccess: () -> Unit) =
        mutate(onSuccess) { repository.updateSettings(name, uses, days, interval) }
}
