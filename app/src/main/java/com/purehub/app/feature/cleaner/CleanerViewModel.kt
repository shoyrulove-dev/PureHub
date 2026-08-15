package com.purehub.app.feature.cleaner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CleanerUiState(
    val isScanning: Boolean = false,
    val statusMessage: String = "Idle",
    val largeFiles: List<CleanerFileItem> = emptyList(),
    val duplicateGroups: List<DuplicateImageGroup> = emptyList(),
    val selectedFileIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
) {
    val totalReclaimableBytes: Long
        get() = (largeFiles + duplicateGroups.flatMap { it.files.drop(1) })
            .distinctBy { it.id }
            .sumOf { it.sizeBytes }

    val selectedBytes: Long
        get() = selectedFiles.sumOf { it.sizeBytes }

    val exactDuplicateBytes: Long
        get() = duplicateGroups
            .flatMap { it.files.drop(1) }
            .distinctBy { it.id }
            .sumOf { it.sizeBytes }

    val selectedFiles: List<CleanerFileItem>
        get() {
            val duplicateFiles = duplicateGroups.flatMap { it.files.drop(1) }
            return (largeFiles + duplicateFiles)
                .distinctBy { it.id }
                .filter { it.id in selectedFileIds }
        }
}

class CleanerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = CleanerRepository(application.contentResolver)

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    fun startScan() {
        if (_uiState.value.isScanning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    statusMessage = "Preparing offline scan",
                    errorMessage = null,
                    selectedFileIds = emptySet(),
                    largeFiles = emptyList(),
                    duplicateGroups = emptyList(),
                )
            }

            runCatching {
                repository.scan { progress ->
                    _uiState.update { state ->
                        state.copy(statusMessage = progress)
                    }
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = "Scan complete",
                        largeFiles = result.largeFiles,
                        duplicateGroups = result.duplicateGroups,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = "Scan failed",
                        errorMessage = throwable.message,
                    )
                }
            }
        }
    }

    fun toggleSelection(file: CleanerFileItem) {
        _uiState.update { state ->
            val updated = state.selectedFileIds.toMutableSet()
            if (!updated.add(file.id)) {
                updated.remove(file.id)
            }
            state.copy(selectedFileIds = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFileIds = emptySet()) }
    }

    fun selectExactDuplicates() {
        _uiState.update { state ->
            state.copy(
                selectedFileIds = state.duplicateGroups
                    .flatMap { it.files.drop(1) }
                    .mapTo(mutableSetOf()) { it.id },
            )
        }
    }

    fun selectAllLargeFiles() {
        _uiState.update { state ->
            state.copy(selectedFileIds = state.largeFiles.mapTo(mutableSetOf()) { it.id })
        }
    }

    fun deleteSelectedFiles() {
        val selectedFiles = _uiState.value.selectedFiles
        if (selectedFiles.isEmpty()) return

        viewModelScope.launch {
            repository.deleteFiles(selectedFiles)
            clearSelection()
            startScan()
        }
    }
}
