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
        // Only reject obvious garbage locally (whitespace inside, far too
        // short) — Google issues keys with several prefixes (AIza, AQ., …),
        // so the real judge is the live validation call below.
        val obviouslyWrong = key.length < 20 || key.any { it.isWhitespace() }
        when {
            key.isBlank() -> setError("Paste your Gemini API key first.")

            obviouslyWrong -> setError(
                "That doesn't look like an API key. Check the string you pasted — keys " +
                    "are usually around 39 characters (often starting with \"AIza\" or " +
                    "\"AQ.\") with no spaces or line breaks."
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
