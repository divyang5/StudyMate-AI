package com.divyang.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.UserRepository
import com.divyang.studymateai.shredPrefs.SharedPref
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TermsUiState(
    val isAccepting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TermsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sharedPref: SharedPref
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermsUiState())
    val uiState: StateFlow<TermsUiState> = _uiState

    /**
     * Records acceptance of the current terms version on the account's
     * Firestore document, then caches it locally. Navigation only happens
     * once the server write succeeds — acceptance must be auditable.
     */
    fun accept(onAccepted: () -> Unit) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.update { it.copy(error = "Session expired. Please sign in again.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAccepting = true, error = null) }
            try {
                userRepository.recordTermsAcceptance(uid)
                sharedPref.setTermsAccepted()
                _uiState.update { it.copy(isAccepting = false) }
                onAccepted()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAccepting = false,
                        error = "Couldn't save your acceptance. Check your connection and try again."
                    )
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
