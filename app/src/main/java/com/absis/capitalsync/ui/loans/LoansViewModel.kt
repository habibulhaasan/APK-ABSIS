// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/loans/LoansViewModel.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

data class RepaymentScheduleEntry(
    val dueDate:   String,
    val amount:    Double,
    val status:    String,   // pending | paid | overdue
    val paidDate:  String,
)

data class LoanItem(
    val id:                 String,
    val purpose:            String,
    val amount:             Double,
    val outstandingBalance: Double,
    val status:             String,   // applied | approved | disbursed | repaid | rejected
    val disbursedDate:      String,
    val durationMonths:     Int,
    val repaymentSchedule:  List<RepaymentScheduleEntry>,
    val notes:              String,
)

data class LoansUiState(
    val loans:         List<LoanItem> = emptyList(),
    val loading:       Boolean        = true,
    val selectedLoan:  LoanItem?      = null,
    val totalOutstanding: Double      = 0.0,
    val activeCount:   Int            = 0,
)

@HiltViewModel
class LoansViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    init { bootstrap() }

    private fun bootstrap() = viewModelScope.launch {
        try {
            val uid     = auth.currentUser?.uid ?: return@launch
            val userSnap= db.collection("users").document(uid).get().await()
            val orgId   = userSnap.getString("activeOrgId") ?: return@launch

            listener = db.collection("organizations/$orgId/loans")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    val loans = snap.documents.mapNotNull { doc ->
                        @Suppress("UNCHECKED_CAST")
                        val scheduleRaw = doc.get("repaymentSchedule") as? List<Map<String, Any>>
                            ?: emptyList()
                        val schedule = scheduleRaw.map { s ->
                            RepaymentScheduleEntry(
                                dueDate  = s["dueDate"] as? String ?: "",
                                amount   = (s["amount"] as? Number)?.toDouble() ?: 0.0,
                                status   = s["status"] as? String ?: "pending",
                                paidDate = s["paidDate"] as? String ?: "",
                            )
                        }
                        LoanItem(
                            id                 = doc.id,
                            purpose            = doc.getString("purpose") ?: "—",
                            amount             = doc.getDouble("amount") ?: 0.0,
                            outstandingBalance = doc.getDouble("outstandingBalance") ?: 0.0,
                            status             = doc.getString("status") ?: "applied",
                            disbursedDate      = doc.getString("disbursedDate") ?: "",
                            durationMonths     = (doc.getLong("durationMonths") ?: 0L).toInt(),
                            repaymentSchedule  = schedule,
                            notes              = doc.getString("notes") ?: "",
                        )
                    }.sortedByDescending { it.disbursedDate }

                    val active = loans.filter { it.status == "disbursed" }
                    _uiState.update { it.copy(
                        loans            = loans,
                        loading          = false,
                        totalOutstanding = active.sumOf { l -> l.outstandingBalance },
                        activeCount      = active.size,
                    )}
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun selectLoan(loan: LoanItem?)  { _uiState.update { it.copy(selectedLoan = loan) } }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
