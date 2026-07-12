package com.purehub.app.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class PasswordVaultUiState(
    val entries: List<VaultEntry> = emptyList(),
    val draftTitle: String = "",
    val draftUsername: String = "",
    val draftPassword: String = "",
    val note: String = "EncryptedSharedPreferences keeps this vault local to the device.",
)

class PasswordVaultViewModel(
    private val repository: PasswordVaultRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PasswordVaultUiState(entries = repository.loadEntries()),
    )
    val uiState: StateFlow<PasswordVaultUiState> = _uiState.asStateFlow()

    fun updateDraftTitle(value: String) {
        _uiState.update { it.copy(draftTitle = value) }
    }

    fun updateDraftUsername(value: String) {
        _uiState.update { it.copy(draftUsername = value) }
    }

    fun updateDraftPassword(value: String) {
        _uiState.update { it.copy(draftPassword = value) }
    }

    fun saveDraft() {
        val state = _uiState.value
        if (state.draftTitle.isBlank() || state.draftPassword.isBlank()) return

        val updated = listOf(
            VaultEntry(
                id = UUID.randomUUID().toString(),
                title = state.draftTitle.trim(),
                username = state.draftUsername.trim(),
                password = state.draftPassword,
            ),
        ) + state.entries

        repository.saveEntries(updated)
        _uiState.update {
            it.copy(
                entries = updated,
                draftTitle = "",
                draftUsername = "",
                draftPassword = "",
                note = "Entry saved in encrypted local storage.",
            )
        }
    }

    fun deleteEntry(entryId: String) {
        val updated = _uiState.value.entries.filterNot { it.id == entryId }
        repository.saveEntries(updated)
        _uiState.update {
            it.copy(
                entries = updated,
                note = "Entry removed from encrypted local storage.",
            )
        }
    }

    companion object {
        fun factory(repository: PasswordVaultRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PasswordVaultViewModel(repository) as T
                }
            }
        }
    }
}
