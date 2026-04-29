// ui/auth/AuthViewModel.kt
package com.absis.capitalsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val loading: Boolean = false,
    val error: String = "",
    val destination: String = "",   // "dashboard" | "admin" | "superadmin"
    val resetSent: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) = viewModelScope.launch {
        _uiState.value = AuthUiState(loading = true)
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid ?: throw Exception("No UID")
            val userSnap = db.collection("users").document(uid).get().await()
            val role     = userSnap.getString("role") ?: ""

            if (role == "superadmin") {
                _uiState.value = AuthUiState(destination = "superadmin"); return@launch
            }
            val activeOrgId = userSnap.getString("activeOrgId") ?: ""
            if (activeOrgId.isNotEmpty()) {
                val memberSnap = db.collection("organizations")
                    .document(activeOrgId).collection("members").document(uid).get().await()
                val memRole    = memberSnap.getString("role") ?: ""
                val approved   = memberSnap.getBoolean("approved") ?: false
                if (memRole == "admin" && approved) {
                    _uiState.value = AuthUiState(destination = "admin"); return@launch
                }
            }
            _uiState.value = AuthUiState(destination = "dashboard")
        } catch (e: Exception) {
            _uiState.value = AuthUiState(error = "Invalid email or password.")
        }
    }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        _uiState.value = AuthUiState(loading = true)
        try {
            auth.sendPasswordResetEmail(email).await()
            _uiState.value = AuthUiState(resetSent = true)
        } catch (e: Exception) {
            _uiState.value = AuthUiState(error = "No account found with this email.")
        }
    }
}