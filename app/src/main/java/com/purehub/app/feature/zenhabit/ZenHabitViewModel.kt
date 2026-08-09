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
import java.time.LocalDate
import com.purehub.app.data.local.entity.HabitEntity

enum class ZenHabitSection { TODAY, INSIGHTS, MANAGE }

data class ZenHabitUiState(
    val draftHabitName: String = "",
    val draftDescription: String = "",
    val draftColorHex: String = "#10B981",
    val draftTargetDays: Int = 7,
    val section: ZenHabitSection = ZenHabitSection.TODAY,
    val composerVisible: Boolean = false,
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
        _uiState.update { it.copy(draftHabitName = value.take(60)) }
    }

    fun updateDraftDescription(value: String) = _uiState.update { it.copy(draftDescription = value.take(120)) }

    fun updateDraftColor(value: String) = _uiState.update { it.copy(draftColorHex = value) }

    fun updateDraftTarget(value: Int) = _uiState.update { it.copy(draftTargetDays = value.coerceIn(1, 7)) }

    fun selectSection(value: ZenHabitSection) = _uiState.update { it.copy(section = value) }

    fun toggleComposer() = _uiState.update { it.copy(composerVisible = !it.composerVisible) }

    fun saveHabit() {
        val draft = _uiState.value.draftHabitName.trim()
        if (draft.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            repository.addHabit(
                name = draft,
                description = _uiState.value.draftDescription,
                colorHex = _uiState.value.draftColorHex,
                targetDaysPerWeek = _uiState.value.draftTargetDays,
            )
            _uiState.update {
                it.copy(
                    draftHabitName = "",
                    draftDescription = "",
                    composerVisible = false,
                    saving = false,
                )
            }
        }
    }

    fun toggleDay(habitId: Long, day: LocalDate, completed: Boolean) {
        viewModelScope.launch { repository.toggleDay(habitId, day, completed) }
    }

    fun setArchived(habit: HabitEntity, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(habit, archived) }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.deleteHabit(habit) }
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
