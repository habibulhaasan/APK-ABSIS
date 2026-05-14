package com.absis.capitalsync.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class LedgerEntry(
    val id: String,
    val source: String, // investment, entryFee, distribution, loan, loan_repay
    val date: String,
    val timestampMs: Long,
    val type: String,
    val label: String,
    val method: String,
    val txId: String,
    val amount: Double,
    val baseAmount: Double,
    val gatewayFee: Double,
    val penaltyPaid: Double,
    val netAmount: Double,
    val status: String,
    val paidMonths: List<String>,
    val isContribution: Boolean,
    val countAsContribution: Boolean,
    val runningTotal: Double = 0.0,
    val receiptUrl: String? = null
)

data class ScheduleMonth(
    val monthStr: String,
    val status: String, // "verified", "pending", "rejected", "overdue", "unpaid", "prejoin"
    val amount: Double,
    val baseAmount: Double,
    val dateStr: String,
    val hasReceipt: Boolean,
    val rawEntry: LedgerEntry?
)

data class LedgerUiState(
    val loading: Boolean = true,
    val memberName: String = "",
    val memberId: String = "",
    val orgName: String = "",
    val feeInAcct: Boolean = false,

    // Filter controls
    val typeFilter: String = "all",
    val statusFilter: String = "all",
    val searchQuery: String = "",

    // Data
    val allEntries: List<LedgerEntry> = emptyList(),
    val filteredEntries: List<LedgerEntry> = emptyList(),
    val scheduleMonths: List<ScheduleMonth> = emptyList(),
    val scheduleSummary: ScheduleSummary = ScheduleSummary(),

    // Stats
    val totalCapital: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalEntryFees: Double = 0.0,
    val pendingCount: Int = 0,
    
    // Config
    val hasQardHasana: Boolean = false,
    val requireBackpayment: Boolean = false,
    val joiningDateStr: String = ""
)

data class ScheduleSummary(
    val paidCount: Int = 0,
    val pendingCount: Int = 0,
    val dueCount: Int = 0,
    val capitalAmt: Double = 0.0,
    val preJoinCount: Int = 0
)

@HiltViewModel
class LedgerViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    // Raw cached data
    private var cachedOtherEntries = emptyList<LedgerEntry>()
    private var orgSettings = emptyMap<String, Any>()
    private var orgFeatures = emptyMap<String, Any>()

    init {
        bootstrap()
    }

    fun refresh() {
        _uiState.update { it.copy(loading = true) }
        listener?.remove()
        bootstrap()
    }

    fun setTypeFilter(type: String) {
        _uiState.update { it.copy(typeFilter = type) }
        applyFilters()
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun bootstrap() = viewModelScope.launch {
        try {
            val uid = auth.currentUser?.uid ?: return@launch
            val userSnap = db.collection("users").document(uid).get().await()
            val orgId = userSnap.getString("activeOrgId") ?: return@launch
            val name = userSnap.getString("nameEnglish") ?: userSnap.getString("displayName") ?: "Member"
            val idNo = userSnap.getString("idNo") ?: "—"

            val orgSnap = db.collection("organizations").document(orgId).get().await()
            val orgName = orgSnap.getString("name") ?: ""
            
            @Suppress("UNCHECKED_CAST")
            orgSettings = orgSnap.get("settings") as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            orgFeatures = orgSnap.get("orgFeatures") as? Map<String, Any> ?: emptyMap()
            val feeInAcct = orgSettings["gatewayFeeInAccounting"] as? Boolean ?: false
            val hasQard = orgFeatures["qardHasana"] as? Boolean ?: false

            val memberSnap = db.collection("organizations/$orgId/members").document(uid).get().await()
            val joiningDateStr = memberSnap.getString("joiningDate") ?: ""

            _uiState.update {
                it.copy(
                    memberName = name,
                    memberId = idNo,
                    orgName = orgName,
                    feeInAcct = feeInAcct,
                    hasQardHasana = hasQard,
                    requireBackpayment = orgSettings["requireBackpayment"] as? Boolean ?: false,
                    joiningDateStr = joiningDateStr
                )
            }

            // 1. Fetch Entry Fees
            val entryFeesSnap = db.collection("organizations/$orgId/entryFees").whereEqualTo("userId", uid).get().await()
            val entryFees = entryFeesSnap.documents.map { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                val ts = doc.getTimestamp("createdAt") ?: doc.getTimestamp("paidAt")
                val notes = doc.getString("notes") ?: ""
                LedgerEntry(
                    id = "ef_${doc.id}", source = "entryFee",
                    date = formatTs(ts), timestampMs = ts?.toDate()?.time ?: 0L,
                    type = "entry_fee", label = if (notes.isNotEmpty()) "Entry Fee — $notes" else "Entry Fee",
                    method = doc.getString("method") ?: "—", txId = "",
                    amount = amount, baseAmount = 0.0, gatewayFee = 0.0, penaltyPaid = 0.0, netAmount = 0.0,
                    status = "verified", paidMonths = emptyList(),
                    isContribution = false, countAsContribution = false
                )
            }

            // 2. Fetch Distributions
            val distSnap = db.collection("organizations/$orgId/profitDistributions").whereEqualTo("status", "distributed").get().await()
            val dists = distSnap.documents.mapNotNull { doc ->
                @Suppress("UNCHECKED_CAST")
                val shares = doc.get("memberShares") as? List<Map<String, Any>> ?: emptyList()
                val myShare = shares.find { it["userId"] == uid } ?: return@mapNotNull null
                val amt = (myShare["shareAmount"] as? Number)?.toDouble() ?: 0.0
                val ts = doc.getTimestamp("createdAt")
                LedgerEntry(
                    id = "dist_${doc.id}", source = "distribution",
                    date = formatTs(ts), timestampMs = ts?.toDate()?.time ?: 0L,
                    type = "profit", label = doc.getString("periodLabel") ?: doc.getString("year") ?: "—",
                    method = "—", txId = "", amount = amt, baseAmount = amt, gatewayFee = 0.0, penaltyPaid = 0.0, netAmount = amt,
                    status = "verified", paidMonths = emptyList(), isContribution = false, countAsContribution = false
                )
            }

            // 3. Fetch Loans
            val loansSnap = db.collection("organizations/$orgId/loans").whereEqualTo("userId", uid).get().await()
            val loans = loansSnap.documents.flatMap { doc ->
                val list = mutableListOf<LedgerEntry>()
                val status = doc.getString("status")
                val purpose = doc.getString("purpose") ?: "Loan"
                if (status == "disbursed" || status == "repaid") {
                    val ts = doc.getTimestamp("disbursedAt") ?: doc.getTimestamp("createdAt")
                    val amt = doc.getDouble("amount") ?: 0.0
                    list.add(
                        LedgerEntry(
                            id = "loan_d_${doc.id}", source = "loan",
                            date = formatTs(ts), timestampMs = ts?.toDate()?.time ?: 0L,
                            type = "loan_disbursed", label = purpose, method = "—", txId = "",
                            amount = amt, baseAmount = 0.0, gatewayFee = 0.0, penaltyPaid = 0.0, netAmount = 0.0,
                            status = "verified", paidMonths = emptyList(), isContribution = false, countAsContribution = false
                        )
                    )
                }
                @Suppress("UNCHECKED_CAST")
                val repayments = doc.get("repayments") as? List<Map<String, Any>> ?: emptyList()
                repayments.forEachIndexed { i, rep ->
                    val ts = rep["createdAt"] as? Timestamp
                    val amt = (rep["amount"] as? Number)?.toDouble() ?: 0.0
                    list.add(
                        LedgerEntry(
                            id = "loan_r_${doc.id}_$i", source = "loan_repay",
                            date = formatTs(ts), timestampMs = ts?.toDate()?.time ?: 0L,
                            type = "loan_repayment", label = "Loan Repayment — $purpose", method = rep["method"] as? String ?: "—", txId = "",
                            amount = amt, baseAmount = 0.0, gatewayFee = 0.0, penaltyPaid = 0.0, netAmount = 0.0,
                            status = "verified", paidMonths = emptyList(), isContribution = false, countAsContribution = false
                        )
                    )
                }
                list
            }

            cachedOtherEntries = entryFees + dists + loans

            // 4. Listen to Investments
            listener = db.collection("organizations/$orgId/investments")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    val investments = snap.documents.map { doc ->
                        val amount = doc.getDouble("amount") ?: 0.0
                        val penalty = doc.getDouble("penaltyPaid") ?: 0.0
                        val gatewayFee = doc.getDouble("gatewayFee") ?: 0.0
                        val isContr = doc.getBoolean("isContribution") != false
                        val countAsContr = doc.getBoolean("countAsContribution") ?: isContr
                        
                        @Suppress("UNCHECKED_CAST")
                        val paidMonths = (doc.get("paidMonths") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        
                        val type = doc.getString("paymentType") ?: if (paidMonths.isNotEmpty()) "monthly" else doc.getString("specialSubType") ?: "general"
                        val label = if (paidMonths.isNotEmpty()) paidMonths.joinToString(", ") else doc.getString("specialSubTitle") ?: "—"
                        val baseAmount = if (isContr) (doc.getDouble("baseAmount") ?: (amount - penalty - gatewayFee)) else 0.0
                        val netAmount = if (feeInAcct) baseAmount else baseAmount - gatewayFee
                        val ts = doc.getTimestamp("createdAt")

                        LedgerEntry(
                            id = doc.id, source = "investment",
                            date = formatTs(ts), timestampMs = ts?.toDate()?.time ?: 0L,
                            type = type, label = label, method = doc.getString("method") ?: "—", txId = doc.getString("txId") ?: "",
                            amount = amount, baseAmount = baseAmount, gatewayFee = gatewayFee, penaltyPaid = penalty, netAmount = netAmount,
                            status = doc.getString("status") ?: "pending", paidMonths = paidMonths,
                            isContribution = isContr, countAsContribution = countAsContr,
                            receiptUrl = doc.getString("receiptUrl") ?: doc.getString("receiptFileId")
                        )
                    }

                    // Combine and Sort
                    val allCombined = (investments + cachedOtherEntries).sortedByDescending { it.timestampMs }

                    // Calculate running totals (bottom to top)
                    var running = 0.0
                    val withRunning = allCombined.reversed().map { entry ->
                        val net = if (entry.status == "verified" && entry.isContribution) entry.netAmount else 0.0
                        running += net
                        entry.copy(runningTotal = running)
                    }.reversed()

                    _uiState.update { it.copy(allEntries = withRunning) }
                    
                    // Generate Schedule & Stats
                    buildScheduleAndApplyFilters()
                }

        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false) }
        }
    }

    private fun buildScheduleAndApplyFilters() {
        val state = _uiState.value
        val entries = state.allEntries

        // Calculate Stats
        val totalCapital = entries.filter { it.status == "verified" && it.isContribution }.sumOf { it.baseAmount - if (state.feeInAcct) 0.0 else it.gatewayFee }
        val totalProfit = entries.filter { it.type == "profit" }.sumOf { it.amount }
        val totalEntryFees = entries.filter { it.type == "entry_fee" || it.type == "reregistration_fee" }.sumOf { it.amount }
        val pendingCount = entries.count { it.status == "pending" }

        // Build Schedule
        val startDateStr = orgSettings["startDate"] as? String ?: ""
        val effectiveStartDate = if (state.requireBackpayment || state.joiningDateStr.isEmpty()) startDateStr 
                                 else if (startDateStr.isEmpty()) state.joiningDateStr 
                                 else if (state.joiningDateStr > startDateStr) state.joiningDateStr else startDateStr

        val allOrgMonths = getMonthsList(startDateStr)
        val memberMonths = getMonthsList(effectiveStartDate)
        val preJoinSet = if (!state.requireBackpayment && state.joiningDateStr.isNotEmpty() && startDateStr.isNotEmpty()) 
                            allOrgMonths.filter { it < effectiveStartDate.take(7) }.toSet() else emptySet()

        val monthStatusMap = mutableMapOf<String, LedgerEntry>()
        entries.filter { it.source == "investment" }.forEach { entry ->
            entry.paidMonths.forEach { mo ->
                if (!monthStatusMap.containsKey(mo) || entry.status == "verified") {
                    monthStatusMap[mo] = entry
                }
            }
        }

        val dueDay = (orgSettings["dueDate"] as? Number)?.toInt() ?: 10
        val scheduleList = allOrgMonths.reversed().map { mo ->
            val rec = monthStatusMap[mo]
            val isPreJoin = preJoinSet.contains(mo)
            val isLate = !isPreJoin && rec == null && checkIsLate(mo, dueDay)

            val status = when {
                isPreJoin -> "prejoin"
                rec?.status == "verified" -> "verified"
                rec?.status == "pending" -> "pending"
                rec?.status == "rejected" -> "rejected"
                isLate -> "overdue"
                else -> "unpaid"
            }

            ScheduleMonth(
                monthStr = mo, status = status,
                amount = rec?.amount ?: 0.0, baseAmount = rec?.baseAmount ?: 0.0,
                dateStr = rec?.date ?: "—", hasReceipt = rec?.receiptUrl != null, rawEntry = rec
            )
        }

        val scheduleSum = ScheduleSummary(
            paidCount = memberMonths.count { monthStatusMap[it]?.status == "verified" },
            pendingCount = memberMonths.count { monthStatusMap[it]?.status == "pending" },
            dueCount = memberMonths.count { !monthStatusMap.containsKey(it) },
            capitalAmt = memberMonths.mapNotNull { monthStatusMap[it] }.filter { it.status == "verified" }.sumOf { it.baseAmount },
            preJoinCount = preJoinSet.size
        )

        _uiState.update { 
            it.copy(
                scheduleMonths = scheduleList,
                scheduleSummary = scheduleSum,
                totalCapital = totalCapital,
                totalProfit = totalProfit,
                totalEntryFees = totalEntryFees,
                pendingCount = pendingCount,
                loading = false
            )
        }
        
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val q = state.searchQuery.lowercase()

        val filtered = state.allEntries.filter { r ->
            if (state.typeFilter == "monthly" && r.type != "monthly") return@filter false
            if (state.typeFilter == "special" && r.type != "general") return@filter false
            if (state.typeFilter == "fees" && r.type != "entry_fee" && r.type != "reregistration_fee") return@filter false
            if (state.typeFilter == "profit" && r.type != "profit") return@filter false
            if (state.typeFilter == "loans" && r.type != "loan_disbursed" && r.type != "loan_repayment") return@filter false
            if (state.statusFilter != "all" && r.status != state.statusFilter) return@filter false
            
            if (q.isNotEmpty()) {
                if (!r.label.lowercase().contains(q) &&
                    !r.txId.lowercase().contains(q) &&
                    !r.method.lowercase().contains(q)) return@filter false
            }
            true
        }

        _uiState.update { it.copy(filteredEntries = filtered) }
    }

    // ── Date Helpers ──
    private fun formatTs(ts: Timestamp?): String {
        if (ts == null) return "—"
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(ts.toDate())
    }

    private fun getMonthsList(startStr: String): List<String> {
        if (startStr.isEmpty()) return emptyList()
        return try {
            val months = mutableListOf<String>()
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val start = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startStr) ?: return emptyList()
            val cal = Calendar.getInstance().apply { time = start; set(Calendar.DAY_OF_MONTH, 1) }
            val now = Calendar.getInstance()
            while (!cal.after(now)) {
                months.add(sdf.format(cal.time))
                cal.add(Calendar.MONTH, 1)
            }
            months
        } catch (e: Exception) { emptyList() }
    }

    private fun checkIsLate(month: String, dueDay: Int): Boolean {
        return try {
            val parts = month.split("-")
            val dueDate = Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, dueDay, 0, 0, 0)
            }
            Calendar.getInstance().after(dueDate)
        } catch (e: Exception) { false }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}