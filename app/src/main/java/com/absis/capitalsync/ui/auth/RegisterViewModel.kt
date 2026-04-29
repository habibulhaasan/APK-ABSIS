// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/auth/RegisterViewModel.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class RegisterUiState(
    val loading:  Boolean = false,
    val success:  Boolean = false,
    val error:    String  = "",
)

@HiltViewModel
class RegisterViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun register(
        nameEnglish: String,
        email:       String,
        password:    String,
        phone:       String,
        orgCode:     String,    // organisation invite/join code
    ) = viewModelScope.launch {
        // Basic validation
        val err = when {
            nameEnglish.isBlank() -> "Full name is required."
            email.isBlank()       -> "Email is required."
            password.length < 8   -> "Password must be at least 8 characters."
            orgCode.isBlank()     -> "Organisation code is required."
            else -> ""
        }
        if (err.isNotEmpty()) { _uiState.value = RegisterUiState(error = err); return@launch }

        _uiState.value = RegisterUiState(loading = true)
        try {
            // 1. Find org by join code
            val orgSnap = db.collection("organizations")
                .whereEqualTo("joinCode", orgCode.trim())
                .limit(1).get().await()

            if (orgSnap.isEmpty) {
                _uiState.value = RegisterUiState(error = "Invalid organisation code.")
                return@launch
            }
            val orgDoc  = orgSnap.documents.first()
            val orgId   = orgDoc.id
            val orgName = orgDoc.getString("name") ?: ""

            // 2. Create Firebase Auth user
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid    = result.user?.uid ?: throw Exception("No UID returned")

            val now = FieldValue.serverTimestamp()

            // 3. Write users/{uid}
            db.collection("users").document(uid).set(
                mapOf(
                    "uid"          to uid,
                    "nameEnglish"  to nameEnglish.trim(),
                    "displayName"  to nameEnglish.trim(),
                    "email"        to email.trim(),
                    "phone"        to phone.trim(),
                    "activeOrgId"  to orgId,
                    "role"         to "member",
                    "approved"     to false,
                    "createdAt"    to now,
                )
            ).await()

            // 4. Write organizations/{orgId}/members/{uid}
            db.collection("organizations/$orgId/members").document(uid).set(
                mapOf(
                    "uid"          to uid,
                    "nameEnglish"  to nameEnglish.trim(),
                    "email"        to email.trim(),
                    "phone"        to phone.trim(),
                    "role"         to "member",
                    "approved"     to false,
                    "createdAt"    to now,
                )
            ).await()

            _uiState.value = RegisterUiState(success = true)

        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("email-already-in-use") == true ->
                    "An account with this email already exists."
                e.message?.contains("invalid-email") == true ->
                    "Please enter a valid email address."
                else -> e.message ?: "Registration failed. Please try again."
            }
            _uiState.value = RegisterUiState(error = msg)
        }
    }
}
