package com.purehub.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.purehub.app.data.local.entity.ExpenseEntryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseTrackerUiState(
    val draftTitle: String = "",
    val draftAmount: String = "",
    val draftCategory: String = "General",
    val draftNote: String = "",
)

class ExpenseTrackerViewModel(
    private val repository: ExpenseTrackerRepository,
) : ViewModel() {
    val expenses = repository.observeExpenses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(ExpenseTrackerUiState())
    val uiState: StateFlow<ExpenseTrackerUiState> = _uiState.asStateFlow()

    fun updateDraftTitle(value: String) = _uiState.update { it.copy(draftTitle = value) }

    fun updateDraftAmount(value: String) = _uiState.update { it.copy(draftAmount = value) }

    fun updateDraftCategory(value: String) = _uiState.update { it.copy(draftCategory = value) }

    fun updateDraftNote(value: String) = _uiState.update { it.copy(draftNote = value) }

    fun saveExpense() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.addExpense(
                title = state.draftTitle,
                amountText = state.draftAmount,
                category = state.draftCategory,
                note = state.draftNote,
            )
            _uiState.value = ExpenseTrackerUiState()
        }
    }

    fun deleteExpense(entry: ExpenseEntryEntity) {
        viewModelScope.launch {
            repository.deleteExpense(entry)
        }
    }

    companion object {
        fun factory(repository: ExpenseTrackerRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExpenseTrackerViewModel(repository) as T
                }
            }
        }
    }
}
