package com.absis.capitalsync.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ExpenseItem(
    val id:       String,
    val date:     String,
    val title:    String,
    val category: String,
    val amount:   Double,
    val notes:    String,
)

@HiltViewModel
class ExpensesViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _expenses = MutableStateFlow<List<ExpenseItem>>(emptyList())
    private val _loading  = MutableStateFlow(true)

    val expenses = _expenses.asStateFlow()
    val loading  = _loading.asStateFlow()

    private var listener: ListenerRegistration? = null

    init { bootstrap() }

    fun refresh() {
        _loading.value = true
        listener?.remove()
        bootstrap()
    }

    private fun bootstrap() = viewModelScope.launch {
        try {
            val uid      = auth.currentUser?.uid ?: return@launch
            val userSnap = db.collection("users").document(uid).get().await()
            val orgId    = userSnap.getString("activeOrgId") ?: return@launch

            listener = db.collection("organizations/$orgId/expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    _expenses.value = snap.documents.map { doc ->
                        ExpenseItem(
                            id       = doc.id,
                            date     = doc.getString("date")     ?: "—",
                            title    = doc.getString("title")    ?: "—",
                            category = doc.getString("category") ?: "—",
                            amount   = doc.getDouble("amount")   ?: 0.0,
                            notes    = doc.getString("notes")    ?: "—",
                        )
                    }
                    _loading.value = false
                }
        } catch (e: Exception) {
            _loading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}