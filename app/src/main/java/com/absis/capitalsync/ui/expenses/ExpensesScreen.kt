// ui/expenses/ExpensesScreen.kt
package com.absis.capitalsync.ui.expenses

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.absis.capitalsync.ui.common.SmartImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun fmt(n: Number) = "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

fun fmtDate(dateStr: String): String {
    if (dateStr.isEmpty() || dateStr == "—") return "—"
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: return dateStr
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
    } catch (_: Exception) { dateStr }
}

// Map Category to Colors
fun getCategoryColors(category: String): Pair<Color, Color> = when (category) {
    "Office"      -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
    "Meeting"     -> Color(0xFFF0FDF4) to Color(0xFF15803D)
    "Travel"      -> Color(0xFFFDF4FF) to Color(0xFF7C3AED)
    "Utilities"   -> Color(0xFFFFF7ED) to Color(0xFFC2410C)
    "Maintenance" -> Color(0xFFFEFCE8) to Color(0xFFA16207)
    "Marketing"   -> Color(0xFFFDF2F8) to Color(0xFFBE185D)
    "Legal"       -> Color(0xFFF0F9FF) to Color(0xFF0369A1)
    else          -> Color(0xFFF8FAFC) to Color(0xFF475569) // Other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(vm: ExpensesViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    var selectedExpense by remember { mutableStateOf<ExpenseItem?>(null) }

    Scaffold(containerColor = Color(0xFFF8FAFC)) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {

                // ── Header ──
                item {
                    Text("Expenses", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text("Organisation spending overview", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 20.dp))
                }

                if (state.loading && state.items.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    }
                    return@LazyColumn
                }

                // ── Summary Cards ──
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        item {
                            ExpenseStatCard("Total Expenses", fmt(state.total), "${state.items.size} entries", Color(0xFFDC2626), Color(0xFFFEF2F2), Color(0xFFFECACA))
                        }
                        item {
                            val curMonthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
                            ExpenseStatCard("This Month", fmt(state.thisMonthTotal), curMonthLabel, Color(0xFFC2410C), Color(0xFFFFF7ED), Color(0xFFFED7AA))
                        }
                        if (state.topCategory != null) {
                            item {
                                ExpenseStatCard("Top Category", state.topCategory!!.first, fmt(state.topCategory!!.second), Color(0xFF0F172A), Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                            }
                        }
                        item {
                            ExpenseStatCard("Categories", state.categoryCount.toString(), "in use", Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFBBF7D0))
                        }
                    }
                }

                // ── Category Breakdown ──
                if (state.total > 0 && state.categoryBreakdown.size > 1) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 12.dp))

                                state.categoryBreakdown.forEach { cat ->
                                    val colors = getCategoryColors(cat.category)
                                    Column(Modifier.padding(bottom = 10.dp)) {
                                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(cat.category, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                            Text(
                                                "${fmt(cat.amount)}  (${cat.percentage}%)",
                                                fontSize = 12.sp, color = Color(0xFF64748B)
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { cat.percentage / 100f },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                            color = colors.second,
                                            trackColor = Color(0xFFF1F5F9)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Transactions List ──
                item {
                    Text("All Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 10.dp))
                }

                if (state.items.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧾", fontSize = 40.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("No expenses recorded", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("The organisation hasn't logged any expenses yet.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        }
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Header Row
                                Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(16.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("DATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.weight(0.8f))
                                    Text("TITLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f))
                                    Text("AMOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))

                                // Items
                                state.items.forEachIndexed { i, item ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(if (i % 2 == 0) Color.White else Color(0xFFFAFAFA))
                                            .clickable { selectedExpense = item }
                                            .padding(16.dp, 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(fmtDate(item.date), fontSize = 11.sp, color = Color(0xFF64748B), modifier = Modifier.weight(0.8f))

                                        Column(Modifier.weight(1.2f).padding(end = 8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                                CategoryBadge(item.category)
                                                if (item.receiptUrl != null || item.receiptFileId != null) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Surface(color = Color(0xFFEFF6FF), shape = CircleShape) { Text("📎", fontSize = 8.sp, modifier = Modifier.padding(5.dp, 2.dp)) }
                                                }
                                            }
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (item.notes.isNotEmpty()) {
                                                Text(item.notes, fontSize = 11.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                            }
                                        }

                                        Text(fmt(item.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626), textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
                                    }
                                    if (i < state.items.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
                                }

                                // Footer
                                HorizontalDivider(color = Color(0xFFFCA5A5), thickness = 2.dp)
                                Row(Modifier.fillMaxWidth().background(Color(0xFFFEF2F2)).padding(16.dp, 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total (${state.items.size} entries)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text(fmt(state.total), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) } // Bottom padding
            }
        }
    }

    // ── Expense Detail Modal ──
    if (selectedExpense != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedExpense = null },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ExpenseDetailSheet(expense = selectedExpense!!, onClose = { selectedExpense = null })
        }
    }
}

// ── Shared UI Components ──────────────────────────────────────────────────────

@Composable
fun ExpenseStatCard(label: String, value: String, sub: String, color: Color, bg: Color, border: Color) {
    Surface(
        color = bg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border),
        modifier = Modifier.width(140.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.06.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (bg, fg) = getCategoryColors(category)
    Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
        Text(category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(8.dp, 2.dp))
    }
}

@Composable
fun ExpenseDetailSheet(expense: ExpenseItem, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(expense.category)
            Text(fmtDate(expense.date), fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(14.dp))
        Text(expense.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), lineHeight = 24.sp)
        Spacer(Modifier.height(16.dp))

        Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp), modifier = Modifier.wrapContentWidth()) {
            Column(Modifier.padding(16.dp, 12.dp)) {
                Text("AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.07.sp, modifier = Modifier.padding(bottom = 4.dp))
                Text(fmt(expense.amount), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
            }
        }

        if (expense.notes.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("NOTES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.07.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Text(expense.notes, fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (expense.receiptUrl != null || expense.receiptFileId != null) {
            val url = expense.receiptUrl ?: "https://drive.google.com/thumbnail?id=${expense.receiptFileId}&sz=w1200"
            Surface(
                color = Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(Modifier.background(Color(0xFFF1F5F9)).fillMaxWidth().padding(12.dp, 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📎 ${expense.receiptName?.ifEmpty { "Receipt" } ?: "Receipt"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) {
                        SmartImage(source = url, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(12.dp))
                    }
                }
            }
        } else {
            Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Text("No receipt attached to this expense.", fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF64748B)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Text("Close", fontWeight = FontWeight.SemiBold)
        }
    }
}