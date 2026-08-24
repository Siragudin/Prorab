package com.example.prorab.presentation.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    // Вызывается, когда Google вернул ответ (успех или ошибка)
    fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        ) }
    }

    // Сбросить состояние (например, после показа ошибки)
    fun resetState() {
        _state.update { SignInState() }
    }
}

// Хранит текущее состояние экрана входа
data class SignInState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)