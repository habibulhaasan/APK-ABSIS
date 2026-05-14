// ui/ledger/LedgerScreen.kt
@file:Suppress("SpellCheckingInspection", "unused")

package com.absis.capitalsync.ui.ledger

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.util.Locale

private fun fmtC(n: Number) = "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

private fun getMonthName(ym: String): String {
    try {
        val d = SimpleDateFormat("yyyy-MM", Locale.US).parse(ym) ?: return ym
        return SimpleDateFormat("MMM yyyy", Locale.US).format(d)
    } catch (_: Exception) { return ym }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    vm: LedgerViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    var selectedEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    if (state.loading && state.allEntries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    Scaffold(containerColor = Color(0xFFF8FAFC)) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // ── Header ──
                item {
                    Text(
                        "My Ledger",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "${state.orgName} · All financial records",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }

                // ── Summary Cards ──
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        item {
                            LedgerStatCard("My Capital", fmtC(state.totalCapital), "verified contributions", Color(0xFF15803D), Color(0xFFF0FDF4), Color(0xFFBBF7D0))
                        }
                        if (state.pendingCount > 0) {
                            item {
                                LedgerStatCard("Pending", state.pendingCount.toString(), "awaiting verification", Color(0xFF92400E), Color(0xFFFEF3C7), Color(0xFFFDE68A))
                            }
                        }
                        if (state.totalProfit > 0) {
                            item {
                                LedgerStatCard("Total Profit", fmtC(state.totalProfit), "from distributions", Color(0xFF1D4ED8), Color(0xFFEFF6FF), Color(0xFFBFDBFE))
                            }
                        }
                        if (state.totalEntryFees > 0) {
                            item {
                                LedgerStatCard("Fees Paid", fmtC(state.totalEntryFees), "entry & re-reg (non-refundable)", Color(0xFF7C3AED), Color(0xFFFAF5FF), Color(0xFFDDD6FE))
                            }
                        }
                        item {
                            LedgerStatCard("Total Records", state.allEntries.size.toString(), "${state.allEntries.count { it.status == "verified" }} verified", Color(0xFF0F172A), Color.White, Color(0xFFE2E8F0))
                        }
                    }
                }

                // ── Contribution Note ──
                item {
                    Surface(
                        color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("🟢 Green line = counts as capital contribution", fontSize = 12.sp, color = Color(0xFF475569))
                            Spacer(Modifier.height(4.dp))
                            Text("⚪ Other line = fee / distribution / loan", fontSize = 12.sp, color = Color(0xFF475569))
                            Spacer(Modifier.height(4.dp))
                            Text("📎 = receipt attached (tap to view)", fontSize = 12.sp, color = Color(0xFF475569))
                        }
                    }
                }

                // ── Filters ──
                item {
                    val filters = mutableListOf(
                        "all" to "All", "schedule" to "📅 Monthly Schedule", "monthly" to "Monthly", "special" to "Special Subs", "fees" to "Fees", "profit" to "Profit"
                    )
                    if (state.hasQardHasana) filters.add("loans" to "Loans")

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        items(filters) { (key, label) ->
                            val sel = state.typeFilter == key
                            Surface(
                                onClick = { vm.setTypeFilter(key) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (sel) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = if(sel) FontWeight.Bold else FontWeight.SemiBold, color = if (sel) Color.White else Color(0xFF475569), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }

                    if (state.typeFilter != "schedule") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                            items(listOf("all", "verified", "pending", "rejected")) { f ->
                                val sel = state.statusFilter == f
                                Surface(
                                    onClick = { vm.setStatusFilter(f) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Color(0xFFEFF6FF) else Color.White,
                                    border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                                ) {
                                    Text(f.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color(0xFF1D4ED8) else Color(0xFF475569), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { vm.setSearchQuery(it) },
                            placeholder = { Text("Search description or TxID...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White
                            )
                        )
                    }
                }

                // ── Schedule View ──
                if (state.typeFilter == "schedule") {
                    item {
                        MonthlyScheduleView(
                            state = state,
                            onRowClick = { if (it.rawEntry != null) selectedEntry = it.rawEntry }
                        )
                    }
                } 
                // ── Empty State ──
                else if (state.filteredEntries.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No records found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Try changing the filters above.", fontSize = 14.sp, color = Color(0xFF64748B))
                        }
                    }
                } 
                // ── List View ──
                else {
                    items(state.filteredEntries, key = { it.id }) { entry ->
                        LedgerEntryCard(entry = entry, onClick = { selectedEntry = entry })
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Bottom spacer
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    // ── Details Bottom Sheet ──
    if (selectedEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedEntry = null },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            PaymentDetailSheet(entry = selectedEntry!!, onClose = { selectedEntry = null })
        }
    }
}

// ── Schedule View Components ──────────────────────────────────────────────────
@Composable
fun MonthlyScheduleView(state: LedgerUiState, onRowClick: (ScheduleMonth) -> Unit) {
    Column {
        if (state.joiningDateStr.isNotEmpty()) {
            Surface(
                color = if (state.requireBackpayment) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (state.requireBackpayment) Color(0xFFFDE68A) else Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    if (state.requireBackpayment) "📅 Member since ${state.joiningDateStr} — all months from org start required."
                    else "📅 Member since ${state.joiningDateStr} — ${state.scheduleSummary.preJoinCount} pre-join months greyed out.",
                    fontSize = 12.sp, color = if (state.requireBackpayment) Color(0xFF92400E) else Color(0xFF15803D),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Mini stat row
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniScheduleStat("Paid", state.scheduleSummary.paidCount.toString(), Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
            MiniScheduleStat("Pending", state.scheduleSummary.pendingCount.toString(), Color(0xFFD97706), Color(0xFFFFFBEB), Modifier.weight(1f))
            MiniScheduleStat("Due", state.scheduleSummary.dueCount.toString(), Color(0xFFDC2626), Color(0xFFFEF2F2), Modifier.weight(1f))
            MiniScheduleStat("Capital", fmtC(state.scheduleSummary.capitalAmt), Color(0xFF1D4ED8), Color(0xFFEFF6FF), Modifier.weight(1.5f))
        }

        Surface(
            shape = RoundedCornerShape(12.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                if (state.scheduleMonths.isEmpty()) {
                    Text("No months to display. Set an org Start Date.", modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center, color = Color(0xFF94A3B8))
                } else {
                    state.scheduleMonths.forEachIndexed { i, mo ->
                        ScheduleRowItem(mo, onClick = { onRowClick(mo) })
                        if (i < state.scheduleMonths.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
        Text("Tap a paid month row to view details.", fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
fun ScheduleRowItem(mo: ScheduleMonth, onClick: () -> Unit) {
    val isClickable = mo.rawEntry != null
    val isPreJoin = mo.status == "prejoin"
    
    val bg = when(mo.status) {
        "prejoin" -> Color(0xFFFAFAFA)
        "verified" -> Color(0xFFF0FDF4)
        "overdue" -> Color(0xFFFFF8F8)
        else -> Color.White
    }
    val borderLeft = when(mo.status) {
        "verified" -> Color(0xFF86EFAC)
        "pending" -> Color(0xFFFDE68A)
        "overdue", "rejected" -> Color(0xFFFCA5A5)
        else -> Color.Transparent
    }

    // High performance border without IntrinsicSize.Min
    Box(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(enabled = isClickable, onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getMonthName(mo.monthStr), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if(isPreJoin) Color(0xFF94A3B8) else Color(0xFF0F172A))
                    if (mo.hasReceipt) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = Color(0xFFEFF6FF), shape = CircleShape) { Text("📎", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) }
                    }
                }
                Text(if(isPreJoin || mo.rawEntry == null) "—" else mo.dateStr, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                val (sBg, sFg, sTxt) = when(mo.status) {
                    "prejoin" -> Triple(Color(0xFFF1F5F9), Color(0xFF94A3B8), "N/A")
                    "verified" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "✓ Verified")
                    "pending" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "⏳ Pending")
                    "rejected" -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "✕ Rejected")
                    "overdue" -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "⚠ Overdue")
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "○ Unpaid")
                }
                Surface(color = sBg, shape = RoundedCornerShape(99.dp)) {
                    Text(sTxt, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sFg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(if (isPreJoin || mo.rawEntry == null) "—" else fmtC(mo.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isPreJoin) Color(0xFF94A3B8) else Color(0xFF0F172A))
            }
        }
        
        // Match height left border indicator (Super fast alternative to IntrinsicSize)
        Box(Modifier.matchParentSize()) {
            Box(Modifier.fillMaxHeight().width(4.dp).background(borderLeft))
        }
    }
}

// ── Transaction Card ──
@Composable
fun LedgerEntryCard(entry: LedgerEntry, onClick: () -> Unit) {
    val isVerified = entry.status == "verified"
    val isContrib = entry.isContribution
    
    val borderColor = if (isContrib && isVerified) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
    val leftBorder = when {
        isContrib && isVerified -> Color(0xFF16A34A)
        entry.status == "pending" -> Color(0xFFF59E0B)
        entry.status == "rejected" -> Color(0xFFDC2626)
        else -> Color.Transparent
    }
    val bg = if (isContrib && isVerified) Color(0xFFF0FDF4) else Color.White

    Surface(
        shape = RoundedCornerShape(12.dp), color = bg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        // High performance border without IntrinsicSize.Min
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                        TypeBadge(entry.type, entry.countAsContribution)
                        if (entry.receiptUrl != null) {
                            Spacer(Modifier.width(6.dp))
                            Surface(color = Color(0xFFEFF6FF), shape = CircleShape) { Text("📎", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                        }
                    }
                    Text(entry.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${entry.date} · ${entry.method}", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp))
                    if (entry.txId.isNotEmpty()) {
                        Text("TxID: ${entry.txId.take(12)}...", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(entry.status)
                    Spacer(Modifier.height(8.dp))
                    Text(fmtC(entry.amount), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    if (isContrib && isVerified && entry.baseAmount > 0) {
                        Text("${fmtC(entry.baseAmount)} capital", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }
                    if (entry.penaltyPaid > 0) {
                        Text("+${fmtC(entry.penaltyPaid)} late fee", fontSize = 10.sp, color = Color(0xFFD97706))
                    }
                }
            }
            
            // Match height left border indicator (Super fast)
            Box(Modifier.matchParentSize()) {
                Box(Modifier.fillMaxHeight().width(4.dp).background(leftBorder))
            }
        }
    }
}

// ── Details Bottom Sheet ──
@Composable
fun PaymentDetailSheet(entry: LedgerEntry, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(entry.type, entry.countAsContribution)
            StatusBadge(entry.status)
        }
        
        Spacer(Modifier.height(16.dp))
        
        Surface(
            color = if (entry.isContribution && entry.status == "verified") Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (entry.isContribution && entry.status == "verified") Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("AMOUNT PAID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 0.07.sp)
                Text(fmtC(entry.amount), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                if (entry.isContribution && entry.status == "verified" && entry.baseAmount > 0) {
                    Text("${fmtC(entry.baseAmount)} credited to your capital", fontSize = 13.sp, color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                }
                if (entry.status == "pending") {
                    Text("Awaiting admin verification — capital not yet credited.", fontSize = 12.sp, color = Color(0xFFD97706), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column {
                DetailRow("Date", entry.date)
                if (entry.method != "—") DetailRow("Method", entry.method)
                if (entry.txId.isNotEmpty()) DetailRow("Transaction ID", entry.txId, isMono = true)
                DetailRow("Total Paid", fmtC(entry.amount))
                if (entry.baseAmount > 0 && entry.isContribution) DetailRow("Capital Credit", fmtC(entry.baseAmount))
                if (entry.penaltyPaid > 0) DetailRow("Late Fee", fmtC(entry.penaltyPaid))
                if (entry.gatewayFee > 0) DetailRow("Gateway Fee", fmtC(entry.gatewayFee))
                DetailRow(if (entry.type == "monthly") "Month(s)" else "Description", entry.label)
            }
        }

        if (entry.receiptUrl != null) {
            Text("PAYMENT RECEIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.06.sp, modifier = Modifier.padding(bottom = 8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF1F5F9), modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 16.dp)) {
                SmartImage(source = entry.receiptUrl, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
            }
        }

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0F172A)), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
            Text("Close", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isMono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium, fontFamily = if (isMono) androidx.compose.ui.text.font.FontFamily.Monospace else null, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
    HorizontalDivider(color = Color.White)
}

// ── Shared UI ──
@Composable
fun LedgerStatCard(label: String, value: String, sub: String, color: Color, bg: Color, border: Color) {
    Surface(color = bg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border), modifier = Modifier.width(150.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.06.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MiniScheduleStat(label: String, value: String, color: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun TypeBadge(type: String, countAsContribution: Boolean) {
    val (label, bg, fg, icon, isContrib) = when (type) {
        "monthly" -> arrayOf("Monthly Installment", Color(0xFFDCFCE7), Color(0xFF15803D), "📅", true)
        "general" -> arrayOf("Special Subscription", Color(0xFFDBEAFE), Color(0xFF1D4ED8), "🎯", true)
        "entry_fee" -> arrayOf("Entry Fee", Color(0xFFE0F2FE), Color(0xFF0369A1), "🎫", countAsContribution)
        "reregistration_fee" -> arrayOf("Re-Registration", Color(0xFFEDE9FE), Color(0xFF7C3AED), "🔄", false)
        "profit" -> arrayOf("Profit Distribution", Color(0xFFD1FAE5), Color(0xFF059669), "📊", false)
        "loan_disbursed" -> arrayOf("Loan Disbursed", Color(0xFFFEE2E2), Color(0xFFDC2626), "🤝", false)
        "loan_repayment" -> arrayOf("Loan Repayment", Color(0xFFFEF3C7), Color(0xFF92400E), "↩", false)
        else -> arrayOf("Payment", Color(0xFFF1F5F9), Color(0xFF475569), "💳", false)
    }
    
    Column {
        Surface(color = bg as Color, shape = RoundedCornerShape(99.dp)) {
            Text("$icon $label", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg as Color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        if (type == "entry_fee" || type == "reregistration_fee") {
            Spacer(Modifier.height(4.dp))
            Surface(color = if (isContrib as Boolean) Color(0xFFDCFCE7) else Color(0xFFFEF3C7), shape = RoundedCornerShape(99.dp)) {
                Text(if(isContrib) "↗ Contribution" else "→ Expenses Fund", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if(isContrib) Color(0xFF15803D) else Color(0xFF92400E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg, txt) = when (status) {
        "verified" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "✓ Verified")
        "pending" -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "⏳ Pending")
        else -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "✕ Rejected")
    }
    Surface(color = bg, shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, if (status == "verified") Color(0xFFBBF7D0) else if (status == "pending") Color(0xFFFDE68A) else Color(0xFFFCA5A5))) {
        Text(txt, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}