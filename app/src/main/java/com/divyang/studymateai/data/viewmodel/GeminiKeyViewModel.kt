package com.divyang.studymateai.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.gemini.GeminiClient
import com.divyang.studymateai.gemini.GenerationQuota
import com.divyang.studymateai.gemini.KeyValidationResult
import com.divyang.studymateai.shredPrefs.SharedPref
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeminiKeyUiState(
    val hasPersonalKey: Boolean = false,
    val remainingToday: Int = 0,
    val isValidating: Boolean = false,
    val errorMessage: String? = null,
    val keyJustSaved: Boolean = false
)

@HiltViewModel
class GeminiKeyViewModel @Inject constructor(
    private val sharedPref: SharedPref,
    private val geminiClient: GeminiClient,
    private val quota: GenerationQuota
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeminiKeyUiState())
    val uiState = _uiState.asStateFlow()

    var keyInput by mutableStateOf("")
        private set

    init {
        refreshStatus()
    }

    fun onKeyInputChange(value: String) {
        keyInput = value
        _uiState.update { it.copy(errorMessage = null, keyJustSaved = false) }
    }

    fun validateAndSave() {
        val key = keyInput.trim()
        // Cheap local sanity check before spending a network call: real
        // Gemini keys start with "AIza" and contain no whitespace.
        val looksValid = key.startsWith("AIza") && key.length >= 30 && key.none { it.isWhitespace() }
        when {
            key.isBlank() -> setError("Paste your Gemini API key first.")

            !looksValid -> setError(
                "That doesn't look like a Gemini API key. Check the string you pasted — " +
                    "it should start with \"AIza\" and contain no spaces or line breaks."
            )

            else -> viewModelScope.launch {
                _uiState.update { it.copy(isValidating = true, errorMessage = null) }
                when (geminiClient.validateKey(key)) {
                    KeyValidationResult.Valid -> {
                        sharedPref.setUserGeminiKey(key)
                        keyInput = ""
                        _uiState.update { it.copy(isValidating = false, keyJustSaved = true) }
                        refreshStatus()
                    }

                    KeyValidationResult.InvalidKey -> _uiState.update {
                        it.copy(
                            isValidating = false,
                            errorMessage = "Gemini rejected this key. Double-check the string you " +
                                "pasted — copy it again from Google AI Studio and make sure nothing " +
                                "is missing or added."
                        )
                    }

                    KeyValidationResult.NetworkError -> _uiState.update {
                        it.copy(
                            isValidating = false,
                            errorMessage = "Couldn't verify the key right now. Check your internet " +
                                "connection and try again."
                        )
                    }
                }
            }
        }
    }

    fun removeKey() {
        sharedPref.clearUserGeminiKey()
        _uiState.update { it.copy(keyJustSaved = false) }
        refreshStatus()
    }

    private fun refreshStatus() {
        _uiState.update {
            it.copy(
                hasPersonalKey = !sharedPref.getUserGeminiKey().isNullOrBlank(),
                remainingToday = quota.remainingToday()
            )
        }
    }

    private fun setError(message: String) =
        _uiState.update { it.copy(errorMessage = message) }
}
