// ui/dashboard/DashboardViewModel.kt
package com.absis.capitalsync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PaymentItem(val date: String, val method: String, val amount: Double, val status: String)
data class RepaymentItem(val dueDate: String, val amount: Double, val loanPurpose: String)
data class DistributionData(val periodLabel: String, val grossProfit: Double, val distributableProfit: Double, val totalCapital: Double)
data class DashboardData(
    val myCapital: Double, val myCapPct: String, val myPending: Int, val myVerified: Int,
    val myTotalProfit: Double, val myLatestShare: Double, val latestDist: DistributionData?,
    val activeLoans: Int, val outstanding: Double, val myPayments: List<PaymentItem>,
    val totalCapital: Double, val paidThisMonth: Boolean, val nextRepayment: RepaymentItem?,
    val isNewMember: Boolean
)

class DashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _dashData      = MutableStateFlow<DashboardData?>(null)
    private val _notifications = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    private val _loading       = MutableStateFlow(true)
    private val _userData      = MutableStateFlow<Map<String, Any>?>(null)

    val dashData      = _dashData.asStateFlow()
    val notifications = _notifications.asStateFlow()
    val loading       = _loading.asStateFlow()
    val userData      = _userData.asStateFlow()

    var isOrgAdmin = false; var hasCapitalLedger = false; var hasQardHasana = false
    var orgName    = "Organization"

    init { loadDashboard() }

    private fun loadDashboard() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val userSnap = db.collection("users").document(uid).get().await()
        val uData    = userSnap.data ?: return@launch
        val orgId    = uData["activeOrgId"] as? String ?: return@launch
        _userData.value = uData

        val orgSnap  = db.collection("organizations").document(orgId).get().await()
        val orgData  = orgSnap.data ?: mapOf<String, Any>()
        orgName      = orgData["name"] as? String ?: "Organization"

        // Parallel fetch
        val (paySnap, distSnap, loanSnap) = Triple(
            db.collection("organizations/$orgId/investments").get().await(),
            db.collection("organizations/$orgId/profitDistributions")
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await(),
            db.collection("organizations/$orgId/loans").get().await()
        )

        val payments   = paySnap.documents.map { it.data?.plus("id" to it.id) ?: mapOf() }
        val myPayments = payments.filter { it["userId"] == uid }
        val myCapital  = myPayments
            .filter { it["status"] == "verified" && it["isContribution"] != false }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
        val totalCapital = payments
            .filter { it["status"] == "verified" && it["isContribution"] != false }
            .sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }

        val myVerified = myPayments.count { it["status"] == "verified" }
        val myPending  = myPayments.count { it["status"] == "pending" }
        val myCapPct   = if (totalCapital > 0) "%.1f".format((myCapital / totalCapital) * 100) else "0"

        val dists       = distSnap.documents.mapNotNull { it.data }.filter { it["status"] == "distributed" }
        val latestDist  = dists.firstOrNull()
        val myLatestShare = latestDist?.let { d ->
            @Suppress("UNCHECKED_CAST")
            (d["memberShares"] as? List<Map<String, Any>>)
                ?.find { it["userId"] == uid }?.let { (it["shareAmount"] as? Number)?.toDouble() } ?: 0.0
        } ?: 0.0
        val myTotalProfit = dists.sumOf { d ->
            @Suppress("UNCHECKED_CAST")
            (d["memberShares"] as? List<Map<String, Any>>)
                ?.find { it["userId"] == uid }?.let { (it["shareAmount"] as? Number)?.toDouble() } ?: 0.0
        }

        val activeLoans = loanSnap.documents.filter { it["userId"] == uid && it["status"] == "disbursed" }
        val outstanding = activeLoans.sumOf { (it["outstandingBalance"] as? Number)?.toDouble() ?: 0.0 }

        val pmtItems = myPayments.take(5).map {
            PaymentItem(
                date   = it["createdAt"]?.toString() ?: "—",
                method = it["method"] as? String ?: "—",
                amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                status = it["status"] as? String ?: "—"
            )
        }

        val distData = latestDist?.let {
            DistributionData(
                periodLabel          = it["periodLabel"] as? String ?: "",
                grossProfit          = (it["grossProfit"] as? Number)?.toDouble() ?: 0.0,
                distributableProfit  = (it["distributableProfit"] as? Number)?.toDouble() ?: 0.0,
                totalCapital         = (it["totalCapital"] as? Number)?.toDouble() ?: 0.0,
            )
        }

        _dashData.value = DashboardData(
            myCapital = myCapital, myCapPct = myCapPct, myPending = myPending, myVerified = myVerified,
            myTotalProfit = myTotalProfit, myLatestShare = myLatestShare, latestDist = distData,
            activeLoans = activeLoans.size, outstanding = outstanding, myPayments = pmtItems,
            totalCapital = totalCapital, paidThisMonth = false, nextRepayment = null,
            isNewMember = myVerified == 0 && myPending == 0
        )
        _loading.value = false

        // Load notifications
        try {
            val nSnap = db.collection("organizations/$orgId/notifications")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(3).get().await()
            _notifications.value = nSnap.documents.mapNotNull { it.data }
        } catch (_: Exception) {}
    }
}