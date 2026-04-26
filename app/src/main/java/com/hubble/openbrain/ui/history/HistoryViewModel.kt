package com.hubble.openbrain.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hubble.openbrain.data.db.Thought
import com.hubble.openbrain.data.db.ThoughtStatus
import com.hubble.openbrain.data.repo.ThoughtRepository
import com.hubble.openbrain.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    repository: ThoughtRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow<ThoughtStatus?>(null)
    val filter: StateFlow<ThoughtStatus?> = _filter.asStateFlow()

    val counts: StateFlow<Map<ThoughtStatus, Int>> = repository.observeCounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val thoughts: StateFlow<List<Thought>> = _filter
        .flatMapLatest { status ->
            if (status == null) repository.observeAll() else repository.observeByStatus(status)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFilter(status: ThoughtStatus?) {
        _filter.value = status
    }

    /**
     * Enqueue a fresh sync. [ThoughtDao.pending] already returns both PENDING and FAILED rows,
     * so retry just schedules the worker — no DB state change needed.
     */
    fun retryFailed() {
        SyncScheduler.enqueue(context)
    }
}
