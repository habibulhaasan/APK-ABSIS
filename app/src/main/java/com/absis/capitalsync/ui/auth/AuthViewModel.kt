package com.absis.capitalsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.absis.capitalsync.core.notifications.CapitalSyncMessagingService

data class AuthUiState(
    val loading:     Boolean = false,
    val error:       String  = "",
    val destination: String  = "",
    val resetSent:   Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) = viewModelScope.launch {
        _uiState.value = AuthUiState(loading = true)
        try {
            val result   = auth.signInWithEmailAndPassword(email, password).await()
            val uid      = result.user?.uid ?: throw Exception("No UID")
            val userSnap = db.collection("users").document(uid).get().await()
            val role     = userSnap.getString("role") ?: ""

            // Superadmin goes to superadmin panel
            if (role == "superadmin") {
                _uiState.value = AuthUiState(destination = "superadmin")
                return@launch
            }

            // Save FCM token for push notifications
            CapitalSyncMessagingService.saveFcmToken(uid)

            // Everyone else (including admins) goes to dashboard
            _uiState.value = AuthUiState(destination = "dashboard")

        } catch (_: Exception) { // Fixed unused parameter 'e'
            _uiState.value = AuthUiState(error = "Invalid email or password.")
        }
    }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        _uiState.value = AuthUiState(loading = true)
        try {
            auth.sendPasswordResetEmail(email).await()
            _uiState.value = AuthUiState(resetSent = true)
        } catch (_: Exception) { // Fixed unused parameter 'e'
            _uiState.value = AuthUiState(error = "No account found with this email.")
        }
    }
}