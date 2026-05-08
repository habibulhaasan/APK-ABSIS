package com.absis.capitalsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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

            // Save FCM token on login
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                db.collection("users").document(uid).update("fcmToken", token)
            }

            // Superadmin goes to superadmin panel
            if (role == "superadmin") {
                _uiState.value = AuthUiState(destination = "superadmin")
                return@launch
            }

            // Everyone else (including org admins) goes to dashboard.
            // Admin banner on DashboardScreen handles the jump to admin panel.
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