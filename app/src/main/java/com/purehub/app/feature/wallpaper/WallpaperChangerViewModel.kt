package com.purehub.app.feature.wallpaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WallpaperChangerUiState(
    val selectedUris: List<String> = emptyList(),
    val rotationHours: Int = 24,
    val rotationEnabled: Boolean = false,
    val note: String = "Pick local images, then let WorkManager rotate them on-device.",
)

class WallpaperChangerViewModel(
    private val repository: WallpaperRotationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        WallpaperChangerUiState(
            selectedUris = repository.loadSelectedUris().map { it.toString() },
            rotationHours = repository.loadRotationHours(),
            rotationEnabled = repository.isRotationEnabled(),
        ),
    )
    val uiState: StateFlow<WallpaperChangerUiState> = _uiState.asStateFlow()

    fun setSelectedUris(uriStrings: List<String>) {
        repository.saveSelectedUris(uriStrings.map(android.net.Uri::parse))
        _uiState.update { it.copy(selectedUris = uriStrings, note = "Wallpaper set updated locally.") }
    }

    fun updateRotationHours(hours: Int) {
        _uiState.update { it.copy(rotationHours = hours.coerceIn(12, 72)) }
    }

    fun applyNow() {
        repository.applyNextWallpaperNow()
        _uiState.update { it.copy(note = "Applied next local wallpaper.") }
    }

    fun scheduleRotation() {
        repository.scheduleRotation(_uiState.value.rotationHours)
        _uiState.update { it.copy(rotationEnabled = true, note = "Wallpaper rotation scheduled locally.") }
    }

    fun cancelRotation() {
        repository.cancelRotation()
        _uiState.update { it.copy(rotationEnabled = false, note = "Wallpaper rotation stopped.") }
    }

    companion object {
        fun factory(repository: WallpaperRotationRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WallpaperChangerViewModel(repository) as T
                }
            }
        }
    }
}
