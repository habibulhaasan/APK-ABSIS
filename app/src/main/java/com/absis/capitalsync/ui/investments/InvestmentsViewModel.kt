// ui/investments/InvestmentsViewModel.kt
package com.absis.capitalsync.ui.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class InvestmentsViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val _projects        = MutableStateFlow<List<Project>>(emptyList())
    private val _loading         = MutableStateFlow(true)
    private val _filter          = MutableStateFlow("all")
    private val _myCapital       = MutableStateFlow(0.0)
    private val _selectedProject = MutableStateFlow<Project?>(null)
    private val _transactions    = MutableStateFlow<List<Transaction>>(emptyList())

    val projects        = _projects.asStateFlow()
    val loading         = _loading.asStateFlow()
    val filter          = _filter.asStateFlow()
    val myCapital       = _myCapital.asStateFlow()
    val selectedProject = _selectedProject.asStateFlow()
    val transactions    = _transactions.asStateFlow()

    private val uid: String get() = auth.currentUser?.uid ?: ""
    var orgId: String = ""; private set

    // capital per member uid — used for share calculations
    private val capitalMap = mutableMapOf<String, Double>()
    private var feeInAcct  = false

    private var projectsListener: ListenerRegistration? = null

    init { bootstrap() }

    // ── Bootstrap: load org context + payments, then listen to projects ────────
    private fun bootstrap() = viewModelScope.launch {
        try {
            val userSnap = db.collection("users").document(uid).get().await()
            orgId        = userSnap.getString("activeOrgId") ?: return@launch

            val orgSnap = db.collection("organizations").document(orgId).get().await()
            feeInAcct   = orgSnap.getBoolean("settings.gatewayFeeInAccounting") ?: false

            // Load all payments to build capitalMap
            val paySnap = db.collection("organizations/$orgId/investments").get().await()
            paySnap.documents.forEach { doc ->
                val status  = doc.getString("status") ?: return@forEach
                val isContr = doc.getBoolean("isContribution") ?: true
                if (status == "verified" && isContr) {
                    val amount     = (doc.getDouble("amount") ?: 0.0)
                    val gatewayFee = if (feeInAcct) 0.0 else (doc.getDouble("gatewayFee") ?: 0.0)
                    val net        = amount - gatewayFee
                    val payUid     = doc.getString("userId") ?: return@forEach
                    capitalMap[payUid] = (capitalMap[payUid] ?: 0.0) + net
                }
            }
            _myCapital.value = capitalMap[uid] ?: 0.0

            // Real-time listener on projects
            projectsListener = db.collection("organizations/$orgId/investmentProjects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    _projects.value = snap.documents.mapNotNull { doc ->
                        doc.toProject()
                    }
                    _loading.value = false
                }

        } catch (e: Exception) {
            _loading.value = false
        }
    }

    // ── Compute my share for a single project ─────────────────────────────────
    // Mirrors getMyShare() in investments/page.js exactly
    fun getMyShare(project: Project): MyShare? {
        val myCapital = capitalMap[uid] ?: 0.0
        if (myCapital <= 0) return null

        // Determine if user participates
        val participatingMembers = project.participatingMembers
        val allParticipate = participatingMembers == null || participatingMembers == "all"

        @Suppress("UNCHECKED_CAST")
        val participantIds: Set<String>? = when {
            allParticipate -> null
            participatingMembers is List<*> -> (participatingMembers as List<String>).toSet()
            else -> null
        }

        if (!allParticipate && participantIds?.contains(uid) == false) return null

        // Total capital of participating members only
        val totalCapital = capitalMap.entries
            .filter { (k, _) -> allParticipate || participantIds?.contains(k) == true }
            .sumOf { it.value }

        if (totalCapital <= 0) return null

        val capShare = myCapital / totalCapital
        val isPeriodic = project.returnType == "periodic"
        val netProfit = when {
            isPeriodic -> project.totalReturns - project.totalExpenses
            project.actualReturnAmount != null ->
                (project.actualReturnAmount) - project.investedAmount
            else -> 0.0
        }

        return MyShare(
            capital           = myCapital.toLong().toDouble(),
            capPct            = capShare * 100,
            effectiveInvested = (capShare * project.investedAmount).toLong().toDouble(),
            profitShare       = (capShare * netProfit).toLong().toDouble(),
            totalCapital      = totalCapital
        )
    }

    // ── Load transactions for selected project ────────────────────────────────
    fun loadTransactions(projectId: String, orgId: String) = viewModelScope.launch {
        _transactions.value = emptyList()
        try {
            val returnsSnap = db.collection(
                "organizations/$orgId/investmentProjects/$projectId/returns"
            ).orderBy("date", Query.Direction.DESCENDING).get().await()

            val expensesSnap = db.collection(
                "organizations/$orgId/investmentProjects/$projectId/projectExpenses"
            ).orderBy("date", Query.Direction.DESCENDING).get().await()

            val returns = returnsSnap.documents.mapNotNull { doc ->
                Transaction(
                    id          = doc.id,
                    kind        = "return",
                    description = doc.getString("description") ?: "",
                    category    = doc.getString("category") ?: "",
                    amount      = doc.getDouble("amount") ?: 0.0,
                    date        = doc.getString("date") ?: "",
                    distributedInDistributionId = doc.getString("distributedInDistributionId")
                )
            }
            val expenses = expensesSnap.documents.mapNotNull { doc ->
                Transaction(
                    id          = doc.id,
                    kind        = "expense",
                    description = doc.getString("description") ?: "",
                    category    = doc.getString("category") ?: "",
                    amount      = doc.getDouble("amount") ?: 0.0,
                    date        = doc.getString("date") ?: "",
                    distributedInDistributionId = null
                )
            }

            // Merge + sort by date descending (same as JS .sort())
            _transactions.value = (returns + expenses).sortedByDescending { it.date }

        } catch (_: Exception) {}
    }

    fun setFilter(f: String)          { _filter.value = f }
    fun selectProject(p: Project?)    { _selectedProject.value = p }

    override fun onCleared() {
        super.onCleared()
        projectsListener?.remove()
    }

    // ── Firestore DocumentSnapshot → Project ──────────────────────────────────
    private fun DocumentSnapshot.toProject(): Project? {
        val id = this.id
        return try {
            Project(
                id                  = id,
                title               = getString("title") ?: return null,
                description         = getString("description") ?: "",
                type                = getString("type") ?: "",
                sector              = getString("sector") ?: "",
                status              = getString("status") ?: "proposed",
                returnType          = getString("returnType") ?: "lumpsum",
                investedAmount      = getDouble("investedAmount") ?: 0.0,
                expectedReturnPct   = getDouble("expectedReturnPct") ?: 0.0,
                actualReturnAmount  = getDouble("actualReturnAmount"),
                profit              = getDouble("profit"),
                totalReturns        = getDouble("totalReturns") ?: 0.0,
                totalExpenses       = getDouble("totalExpenses") ?: 0.0,
                startDate           = getString("startDate") ?: "",
                completedDate       = getString("completedDate") ?: "",
                notes               = getString("notes") ?: "",
                participatingMembers = get("participatingMembers"),
                fundSources         = get("fundSources") as? Map<String, Double>,
                fundSource          = getString("fundSource") ?: ""
            )
        } catch (e: Exception) { null }
    }
}