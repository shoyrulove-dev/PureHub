package com.purehub.app.feature.zenhabit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZenHabitUiState(
    val draftHabitName: String = "",
    val saving: Boolean = false,
)

class ZenHabitViewModel(
    private val repository: ZenHabitRepository,
) : ViewModel() {
    val habitSummaries = repository.observeHabitSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(ZenHabitUiState())
    val uiState: StateFlow<ZenHabitUiState> = _uiState.asStateFlow()

    fun updateDraftHabitName(value: String) {
        _uiState.update { it.copy(draftHabitName = value) }
    }

    fun saveHabit() {
        val draft = _uiState.value.draftHabitName.trim()
        if (draft.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            repository.addHabit(draft)
            _uiState.update {
                it.copy(
                    draftHabitName = "",
                    saving = false,
                )
            }
        }
    }

    fun toggleToday(habitId: Long, isCompletedToday: Boolean) {
        viewModelScope.launch {
            repository.toggleToday(habitId, isCompletedToday)
        }
    }

    companion object {
        fun factory(repository: ZenHabitRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ZenHabitViewModel(repository) as T
                }
            }
        }
    }
}
