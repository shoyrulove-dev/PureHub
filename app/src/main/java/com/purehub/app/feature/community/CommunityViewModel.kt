package com.purehub.app.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.purehub.app.data.CommunityPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityUiState(
    val proCode: String = "",
    val isUnlocked: Boolean = false,
)

class CommunityViewModel(
    private val repository: CommunityPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { preferences ->
                _uiState.update {
                    it.copy(
                        proCode = preferences.proCode,
                        isUnlocked = preferences.isUnlocked,
                    )
                }
            }
        }
    }

    fun saveCode(code: String) {
        viewModelScope.launch {
            repository.saveProCode(code)
        }
    }

    companion object {
        fun factory(
            repository: CommunityPreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CommunityViewModel(repository) as T
            }
        }
    }
}
