
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/auth/ResetPasswordViewModel.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _phase   = MutableStateFlow(ResetPhase.VERIFYING)
    private val _email   = MutableStateFlow("")
    private val _error   = MutableStateFlow("")
    private val _loading = MutableStateFlow(false)

    val phase   = _phase.asStateFlow()
    val email   = _email.asStateFlow()
    val error   = _error.asStateFlow()
    val loading = _loading.asStateFlow()

    // Verify the oobCode from the Firebase deep link
    fun verifyCode(oobCode: String) = viewModelScope.launch {
        if (oobCode.isEmpty()) { _phase.value = ResetPhase.INVALID; return@launch }
        try {
            val email      = auth.verifyPasswordResetCode(oobCode).await()
            _email.value   = email
            _phase.value   = ResetPhase.FORM
        } catch (e: Exception) {
            _phase.value   = ResetPhase.INVALID
        }
    }

    fun resetPassword(oobCode: String, password: String, confirm: String) = viewModelScope.launch {
        // Validate
        val validationError = when {
            password.length < 8           -> "Password must be at least 8 characters."
            getStrength(password).score < 2 -> "Please choose a stronger password."
            password != confirm           -> "Passwords do not match."
            else                          -> ""
        }
        if (validationError.isNotEmpty()) { _error.value = validationError; return@launch }

        _error.value   = ""
        _loading.value = true
        try {
            auth.confirmPasswordReset(oobCode, password).await()
            _phase.value = ResetPhase.SUCCESS
        } catch (e: Exception) {
            _error.value = when {
                e.message?.contains("expired") == true ->
                    "This reset link has expired. Please request a new one."
                else -> "Something went wrong. Please try again."
            }
        }
        _loading.value = false
    }
}
