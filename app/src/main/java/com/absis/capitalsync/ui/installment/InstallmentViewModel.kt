// ui/installment/InstallmentViewModel.kt
package com.absis.capitalsync.ui.installment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class InstallmentUiState(
    val payMode: PayMode                              = PayMode.MONTHLY,
    val selectedMonths: Set<String>                   = emptySet(),
    val selectedSpecial: SpecialSub?                  = null,
    val selectedMethod: String                        = "",
    val selectedAccount: PaymentAccount?              = null,
    val currentMethodAccounts: List<PaymentAccount>   = emptyList(),
    val enabledMethods: List<String>                  = emptyList(),
    val paymentAccounts: Map<String, List<PaymentAccount>> = emptyMap(),
    val paidSpecialIds: Set<String>                   = emptySet(),
    val txId: String                                  = "",
    val customSpecialAmount: String                   = "",
    val baseAmount: Double                            = 0.0,
    val penalty: Double                               = 0.0,
    val feeRate: Double                               = 0.0,
    val totalBase: Double                             = 0.0,
    val totalPenalty: Double                          = 0.0,
    val fee: Double                                   = 0.0,
    val grandTotal: Double                            = 0.0,
    val monthlyEnabled: Boolean                       = true,
    val hasSpecialSubs: Boolean                       = false,
    val hasAnything: Boolean                          = true,
    val orgName: String                               = "",
    val orgLogoUrl: String?                           = null,
    val missedCount: Int                              = 0,
    val isLatePayer: Boolean                          = false,
    val reregPending: Boolean                         = false,
    val reregGranted: Boolean                         = false,
    val isImpersonating: Boolean                      = false,
    val loading: Boolean                              = false,
    val success: Boolean                              = false,
)

private val DEFAULT_METHODS = listOf("bKash", "Nagad", "Rocket", "Bank Transfer", "Cash")

@HiltViewModel
class InstallmentViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _uiState      = MutableStateFlow(InstallmentUiState())
    private val _specialSubs  = MutableStateFlow<List<SpecialSub>>(emptyList())
    private val _unpaidMonths = MutableStateFlow<List<String>>(emptyList())

    val uiState      = _uiState.asStateFlow()
    val specialSubs  = _specialSubs.asStateFlow()
    val unpaidMonths = _unpaidMonths.asStateFlow()

    private val uid: String get() = auth.currentUser?.uid ?: ""
    private var orgId: String = ""
    private var allMonths: List<String> = emptyList()
    private var paidMonths: Set<String> = emptySet()
    private var subsListener: ListenerRegistration? = null

    init { bootstrap() }

    // ── Bootstrap ─────────────────────────────────────────────────────────────
    private fun bootstrap() = viewModelScope.launch {
        try {
            val userSnap   = db.collection("users").document(uid).get().await()
            orgId          = userSnap.getString("activeOrgId") ?: return@launch

            val orgSnap    = db.collection("organizations").document(orgId).get().await()
            val settings   = orgSnap.get("settings") as? Map<String, Any> ?: emptyMap()
            val orgFeatures= orgSnap.get("orgFeatures") as? Map<String, Any> ?: emptyMap()

            val memberSnap = db.collection("organizations/$orgId/members")
                .document(uid).get().await()
            val membership = memberSnap.data ?: emptyMap()

            // ── Parse settings ──
            val monthlyEnabled   = settings["monthlyEnabled"] as? Boolean ?: true
            val baseAmount       = (settings["baseAmount"] as? Number)?.toDouble() ?: 0.0
            val customAmount     = (membership["customAmount"] as? Number)?.toDouble()
            val effectiveBase    = if (settings["uniformAmount"] == false && customAmount != null)
                                       customAmount else baseAmount
            val dueDay           = (settings["dueDate"] as? Number)?.toInt() ?: 10
            val lateFeeEnabled   = settings["lateFeeEnabled"] as? Boolean ?: false
            val penaltyAmt       = if (lateFeeEnabled) (settings["penalty"] as? Number)?.toDouble() ?: 0.0 else 0.0
            val enabledMethods   = (settings["paymentMethods"] as? List<*>)?.filterIsInstance<String>()
                                   ?: DEFAULT_METHODS
            val requireBackpay   = settings["requireBackpayment"] as? Boolean ?: false
            val joiningDateStr   = membership["joiningDate"] as? String ?: ""
            val orgStartStr      = settings["startDate"] as? String ?: ""
            val latePayerEnabled = settings["latePayerEnabled"] as? Boolean ?: false
            val latePayerThresh  = (settings["latePayerAfterMonths"] as? Number)?.toInt() ?: 1
            val reregAutoAssign  = settings["reregAutoAssign"] as? Boolean ?: false
            val reregThreshold   = (settings["reregAfterMonths"] as? Number)?.toInt() ?: 3
            val reregGranted     = membership["reregGranted"] as? Boolean ?: false
            val orgName          = orgSnap.getString("name") ?: ""
            val orgLogoUrl       = orgSnap.getString("logoURL")

            // ── Payment accounts ──
            @Suppress("UNCHECKED_CAST")
            val rawAccounts = settings["paymentAccounts"] as? Map<String, List<Map<String, Any>>> ?: emptyMap()
            val paymentAccounts = rawAccounts.mapValues { (_, accs) ->
                accs.map { a ->
                    PaymentAccount(
                        id      = a["id"] as? String ?: UUID.randomUUID().toString(),
                        label   = a["label"] as? String ?: "",
                        number  = a["number"] as? String ?: "",
                        enabled = a["enabled"] as? Boolean ?: true
                    )
                }
            }

            // ── Effective start date ──
            val effectiveStart = when {
                requireBackpay || joiningDateStr.isEmpty() -> orgStartStr
                orgStartStr.isEmpty()                      -> joiningDateStr
                joiningDateStr > orgStartStr               -> joiningDateStr
                else                                       -> orgStartStr
            }

            // ── Build month list ──
            allMonths  = buildMonthList(effectiveStart)

            // ── Load payments ──
            val paySnap = db.collection("organizations/$orgId/investments")
                .whereEqualTo("userId", uid).get().await()

            val paid   = mutableSetOf<String>()
            val pSpec  = mutableSetOf<String>()
            paySnap.documents.forEach { doc ->
                val status = doc.getString("status") ?: return@forEach
                if (status == "rejected") return@forEach
                @Suppress("UNCHECKED_CAST")
                (doc.get("paidMonths") as? List<*>)?.filterIsInstance<String>()
                    ?.forEach { paid.add(it) }
                doc.getString("specialSubId")?.let { pSpec.add(it) }
            }
            paidMonths = paid

            val unpaid = allMonths.filter { it !in paid }
            _unpaidMonths.value = unpaid

            // ── Late payer detection ──
            val missedCount = allMonths.count { it !in paid && isMonthPast(it, dueDay) }
            val isLatePayer = latePayerEnabled && missedCount > latePayerThresh
            val reregRequired = reregAutoAssign && missedCount >= reregThreshold
            val reregSubPaid  = pSpec.any { sid ->
                _specialSubs.value.find { it.id == sid }?.type == "reregistration_fee"
            }
            val reregPending = reregRequired && !reregSubPaid && !reregGranted

            // ── Gateway fees ──
            @Suppress("UNCHECKED_CAST")
            val gatewayFees = settings["gatewayFees"] as? Map<String, Map<String, Any>> ?: emptyMap()

            val initialMethod = enabledMethods.firstOrNull() ?: ""
            val initialAccounts = paymentAccounts[initialMethod]?.filter { it.enabled } ?: emptyList()

            _uiState.value = InstallmentUiState(
                enabledMethods       = enabledMethods,
                paymentAccounts      = paymentAccounts,
                currentMethodAccounts= initialAccounts,
                selectedMethod       = initialMethod,
                selectedAccount      = if (initialAccounts.size == 1) initialAccounts[0] else null,
                baseAmount           = effectiveBase,
                penalty              = penaltyAmt,
                monthlyEnabled       = monthlyEnabled,
                hasAnything          = monthlyEnabled,
                orgName              = orgName,
                orgLogoUrl           = orgLogoUrl,
                paidSpecialIds       = pSpec,
                missedCount          = missedCount,
                isLatePayer          = isLatePayer,
                reregPending         = reregPending,
                reregGranted         = reregGranted,
            )

            // Store gateway fees map for later fee calculations
            _gatewayFees = gatewayFees

            // ── Real-time special subs listener ──
            listenToSpecialSubs()

        } catch (e: Exception) {
            // handle gracefully
        }
    }

    private var _gatewayFees: Map<String, Map<String, Any>> = emptyMap()

    // ── Special subs live listener ─────────────────────────────────────────────
    private fun listenToSpecialSubs() {
        subsListener = db.collection("organizations/$orgId/specialSubscriptions")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val now  = System.currentTimeMillis()
                val subs = snap.documents.mapNotNull { doc ->
                    val active   = doc.getBoolean("active") ?: false
                    val deadline = doc.getString("deadline") ?: return@mapNotNull null
                    val deadlineMs = parseDate(deadline) ?: return@mapNotNull null
                    if (!active || deadlineMs < now) return@mapNotNull null
                    val daysLeft = ((deadlineMs - now) / (1000 * 60 * 60 * 24)).toInt()
                    SpecialSub(
                        id               = doc.id,
                        title            = doc.getString("title") ?: "",
                        description      = doc.getString("description") ?: "",
                        amount           = doc.getDouble("amount") ?: 0.0,
                        deadline         = deadline,
                        daysLeft         = daysLeft,
                        type             = doc.getString("type") ?: "general",
                        allowCustomAmount= doc.getBoolean("allowCustomAmount") ?: false,
                        active           = active
                    )
                }.sortedBy { it.deadline }
                _specialSubs.value = subs
                _uiState.update { it.copy(
                    hasSpecialSubs = subs.isNotEmpty(),
                    hasAnything    = it.monthlyEnabled || subs.isNotEmpty()
                )}
            }
    }

    // ── UI actions ────────────────────────────────────────────────────────────

    fun setPayMode(mode: PayMode) {
        _uiState.update { it.copy(
            payMode        = mode,
            selectedMonths = if (mode == PayMode.SPECIAL) emptySet() else it.selectedMonths,
            selectedSpecial= if (mode == PayMode.MONTHLY) null else it.selectedSpecial,
        )}
        recalculate()
    }

    fun toggleMonth(month: String) {
        _uiState.update { state ->
            val updated = state.selectedMonths.toMutableSet()
            if (month in updated) updated.remove(month) else updated.add(month)
            state.copy(selectedMonths = updated)
        }
        recalculate()
    }

    fun selectSpecialSub(sub: SpecialSub?) {
        _uiState.update { it.copy(selectedSpecial = sub, customSpecialAmount = "") }
        recalculate()
    }

    fun setCustomAmount(value: String) {
        _uiState.update { it.copy(customSpecialAmount = value) }
        recalculate()
    }

    fun selectMethod(method: String) {
        val accounts = _uiState.value.paymentAccounts[method]?.filter { it.enabled } ?: emptyList()
        _uiState.update { it.copy(
            selectedMethod        = method,
            currentMethodAccounts = accounts,
            selectedAccount       = if (accounts.size == 1) accounts[0] else null,
            txId                  = "",
        )}
        recalculate()
    }

    fun selectAccount(account: PaymentAccount?) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun setTxId(value: String) {
        _uiState.update { it.copy(txId = value) }
    }

    // ── Recalculate totals ────────────────────────────────────────────────────
    private fun recalculate() {
        val state = _uiState.value
        val isSpecial = state.payMode == PayMode.SPECIAL && state.selectedSpecial != null

        val specialBase = if (isSpecial) {
            val sub = state.selectedSpecial!!
            if (sub.allowCustomAmount) state.customSpecialAmount.toDoubleOrNull() ?: sub.amount
            else sub.amount
        } else 0.0

        val totalBase = if (isSpecial) specialBase
                        else state.selectedMonths.size * state.baseAmount

        val totalPenalty = if (isSpecial) 0.0
                           else state.selectedMonths.count { isMonthLate(it, 10) } * state.penalty

        val feeRate = getGatewayFeeRate(state.selectedMethod)
        val fee     = ((totalBase + totalPenalty) * feeRate).toLong().toDouble()
        val grand   = totalBase + totalPenalty + fee

        _uiState.update { it.copy(
            totalBase    = totalBase,
            totalPenalty = totalPenalty,
            feeRate      = feeRate,
            fee          = fee,
            grandTotal   = grand,
        )}
    }

    private fun getGatewayFeeRate(method: String): Double {
        val cfg = _gatewayFees[method] ?: return 0.0
        val enabled = cfg["enabled"] as? Boolean ?: false
        val rate    = (cfg["rate"] as? Number)?.toDouble() ?: 0.0
        return if (enabled) rate / 100.0 else 0.0
    }

    // ── Submit payment ────────────────────────────────────────────────────────
    fun submitPayment() = viewModelScope.launch {
        val state = _uiState.value
        if (state.isImpersonating) return@launch
        _uiState.update { it.copy(loading = true) }
        try {
            val acc = state.selectedAccount
                ?: state.currentMethodAccounts.firstOrNull()

            val payload = mutableMapOf<String, Any>(
                "userId"       to uid,
                "amount"       to state.grandTotal,
                "method"       to state.selectedMethod,
                "txId"         to state.txId.trim(),
                "status"       to "pending",
                "createdAt"    to FieldValue.serverTimestamp(),
                "accountId"    to (acc?.id ?: ""),
                "accountLabel" to (acc?.label ?: ""),
                "accountNumber"to (acc?.number ?: ""),
            )

            if (state.payMode == PayMode.MONTHLY) {
                payload["paidMonths"]    = state.selectedMonths.toList()
                payload["baseAmount"]    = state.totalBase
                payload["penaltyPaid"]   = state.totalPenalty
                payload["gatewayFee"]    = state.fee
                payload["paymentType"]   = "monthly"
                payload["isContribution"]= true
            } else {
                val sub     = state.selectedSpecial!!
                val subType = sub.type.ifEmpty { "general" }
                val isContr = subType == "general"
                payload["specialSubId"]          = sub.id
                payload["specialSubTitle"]        = sub.title
                payload["specialSubType"]         = subType
                payload["baseAmount"]             = sub.amount
                payload["gatewayFee"]             = state.fee
                payload["paidMonths"]             = emptyList<String>()
                payload["paymentType"]            = subType
                payload["isContribution"]         = isContr
                payload["countAsContribution"]    = isContr
            }

            db.collection("organizations/$orgId/investments").add(payload).await()
            _uiState.update { it.copy(success = true, loading = false) }

        } catch (e: Exception) {
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(
            success        = false,
            selectedMonths = emptySet(),
            selectedSpecial= null,
            txId           = "",
            customSpecialAmount = "",
            selectedAccount= null,
        )}
        recalculate()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildMonthList(startDate: String): List<String> {
        if (startDate.isEmpty()) return emptyList()
        val months = mutableListOf<String>()
        val sdf    = SimpleDateFormat("yyyy-MM", Locale.US)
        return try {
            val start = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startDate) ?: return emptyList()
            val cal   = Calendar.getInstance().apply { time = start; set(Calendar.DAY_OF_MONTH, 1) }
            val now   = Calendar.getInstance()
            while (!cal.after(now)) {
                months.add(sdf.format(cal.time))
                cal.add(Calendar.MONTH, 1)
            }
            months
        } catch (e: Exception) { emptyList() }
    }

    private fun isMonthLate(month: String, dueDay: Int): Boolean {
        return try {
            val parts = month.split("-")
            val y = parts[0].toInt(); val m = parts[1].toInt()
            val dueDate = Calendar.getInstance().apply { set(y, m - 1, dueDay, 0, 0, 0) }
            Calendar.getInstance().after(dueDate)
        } catch (e: Exception) { false }
    }

    private fun isMonthPast(month: String, dueDay: Int) = isMonthLate(month, dueDay)

    private fun parseDate(dateStr: String): Long? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
        } catch (e: Exception) { null }
    }

    override fun onCleared() {
        super.onCleared()
        subsListener?.remove()
    }
}