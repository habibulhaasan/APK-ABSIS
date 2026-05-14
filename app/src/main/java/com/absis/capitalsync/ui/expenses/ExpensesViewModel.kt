package com.absis.capitalsync.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ExpenseItem(
    val id: String,
    val date: String,
    val title: String,
    val category: String,
    val amount: Double,
    val notes: String,
    val receiptFileId: String?,
    val receiptUrl: String?,
    val receiptName: String?
)

data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Int
)

data class ExpensesUiState(
    val loading: Boolean = true,
    val items: List<ExpenseItem> = emptyList(),
    val total: Double = 0.0,
    val thisMonthTotal: Double = 0.0,
    val topCategory: Pair<String, Double>? = null,
    val categoryCount: Int = 0,
    val categoryBreakdown: List<CategoryStat> = emptyList()
)

@HiltViewModel
class ExpensesViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null

    init { bootstrap() }

    fun refresh() {
        _uiState.update { it.copy(loading = true) }
        listener?.remove()
        bootstrap()
    }

    private fun bootstrap() = viewModelScope.launch {
        try {
            val uid = auth.currentUser?.uid ?: return@launch
            val userSnap = db.collection("users").document(uid).get().await()
            val orgId = userSnap.getString("activeOrgId") ?: return@launch

            listener = db.collection("organizations/$orgId/expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener

                    val items = snap.documents.map { doc ->
                        ExpenseItem(
                            id = doc.id,
                            date = doc.getString("date") ?: "—",
                            title = doc.getString("title") ?: "—",
                            category = doc.getString("category")?.ifEmpty { "Other" } ?: "Other",
                            amount = doc.getDouble("amount") ?: 0.0,
                            notes = doc.getString("notes") ?: "",
                            receiptFileId = doc.getString("receiptFileId"),
                            receiptUrl = doc.getString("receiptUrl"),
                            receiptName = doc.getString("receiptName")
                        )
                    }

                    // ── Calculate Stats (matching Next.js logic) ──
                    val total = items.sumOf { it.amount }

                    val curMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                    val thisMonthTotal = items
                        .filter { it.date.startsWith(curMonthStr) }
                        .sumOf { it.amount }

                    val byCat = items.groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    val sortedCats = byCat.entries.sortedByDescending { it.value }
                    val topCategory = sortedCats.firstOrNull()?.let { it.key to it.value }
                    val categoryCount = byCat.size

                    val top4Breakdown = sortedCats.take(4).map {
                        val pct = if (total > 0) Math.round((it.value / total) * 100).toInt() else 0
                        CategoryStat(it.key, it.value, pct)
                    }

                    _uiState.update {
                        it.copy(
                            loading = false,
                            items = items,
                            total = total,
                            thisMonthTotal = thisMonthTotal,
                            topCategory = topCategory,
                            categoryCount = categoryCount,
                            categoryBreakdown = top4Breakdown
                        )
                    }
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