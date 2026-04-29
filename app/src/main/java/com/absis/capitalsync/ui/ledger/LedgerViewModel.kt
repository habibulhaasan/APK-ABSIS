// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/ledger/LedgerViewModel.kt
// Capital ledger — shows member's full payment
// history with running capital balance
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LedgerEntry(
    val id:          String,
    val date:        String,
    val type:        String,    // "monthly" | "entry_fee" | "reregistration_fee" | "general" | etc.
    val method:      String,
    val amount:      Double,
    val gatewayFee:  Double,
    val netAmount:   Double,    // amount - gatewayFee (when feeInAccounting=false)
    val status:      String,    // verified | pending | rejected
    val paidMonths:  List<String>,
    val penaltyPaid: Double,
    val isContribution: Boolean,
    val runningTotal:   Double, // computed client-side
)

data class LedgerUiState(
    val entries:       List<LedgerEntry> = emptyList(),
    val totalCapital:  Double            = 0.0,
    val totalProfit:   Double            = 0.0,
    val memberName:    String            = "",
    val memberId:      String            = "",
    val orgName:       String            = "",
    val loading:       Boolean           = true,
    val feeInAcct:     Boolean           = false,
)

@HiltViewModel
class LedgerViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    init { bootstrap() }

    private fun bootstrap() = viewModelScope.launch {
        try {
            val uid      = auth.currentUser?.uid ?: return@launch
            val userSnap = db.collection("users").document(uid).get().await()
            val orgId    = userSnap.getString("activeOrgId") ?: return@launch
            val name     = userSnap.getString("nameEnglish")
                        ?: userSnap.getString("displayName") ?: "Member"
            val idNo     = userSnap.getString("idNo") ?: "—"

            val orgSnap  = db.collection("organizations").document(orgId).get().await()
            val orgName  = orgSnap.getString("name") ?: ""
            val feeInAcct= orgSnap.getBoolean("settings.gatewayFeeInAccounting") ?: false

            // Profit distributions — to calculate total profit
            val distSnap = db.collection("organizations/$orgId/profitDistributions")
                .whereEqualTo("status", "distributed")
                .get().await()

            val totalProfit = distSnap.documents.sumOf { doc ->
                @Suppress("UNCHECKED_CAST")
                val shares = doc.get("memberShares") as? List<Map<String, Any>> ?: emptyList()
                shares.find { it["userId"] == uid }
                    ?.let { (it["shareAmount"] as? Number)?.toDouble() } ?: 0.0
            }

            // Real-time listener on member's investments
            listener = db.collection("organizations/$orgId/investments")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    val rawEntries = snap.documents.map { doc ->
                        val amount      = doc.getDouble("amount") ?: 0.0
                        val gatewayFee  = doc.getDouble("gatewayFee") ?: 0.0
                        val isContr     = doc.getBoolean("isContribution") ?: true
                        val netAmount   = if (feeInAcct) amount else amount - gatewayFee
                        val createdAt   = doc.getTimestamp("createdAt")
                        val dateStr     = createdAt?.toDate()?.let {
                            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(it)
                        } ?: "—"

                        @Suppress("UNCHECKED_CAST")
                        val paidMonths = (doc.get("paidMonths") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList()

                        LedgerEntry(
                            id             = doc.id,
                            date           = dateStr,
                            type           = doc.getString("paymentType") ?: "monthly",
                            method         = doc.getString("method") ?: "—",
                            amount         = amount,
                            gatewayFee     = gatewayFee,
                            netAmount      = netAmount,
                            status         = doc.getString("status") ?: "pending",
                            paidMonths     = paidMonths,
                            penaltyPaid    = doc.getDouble("penaltyPaid") ?: 0.0,
                            isContribution = isContr,
                            runningTotal   = 0.0, // computed below
                        )
                    }

                    // Compute running capital total (oldest → newest, only verified contributions)
                    val sorted = rawEntries.reversed()
                    var running = 0.0
                    val withRunning = sorted.map { entry ->
                        val net = if (entry.status == "verified" && entry.isContribution)
                            entry.netAmount else 0.0
                        running += net
                        entry.copy(runningTotal = running)
                    }.reversed() // display newest first

                    val totalCapital = withRunning
                        .filter { it.status == "verified" && it.isContribution }
                        .sumOf { it.netAmount }

                    _uiState.update { it.copy(
                        entries      = withRunning,
                        totalCapital = totalCapital,
                        totalProfit  = totalProfit,
                        memberName   = name,
                        memberId     = idNo,
                        orgName      = orgName,
                        loading      = false,
                        feeInAcct    = feeInAcct,
                    )}
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}