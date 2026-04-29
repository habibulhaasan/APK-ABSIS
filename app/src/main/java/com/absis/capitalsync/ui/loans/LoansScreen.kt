
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/loans/LoansScreen.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.loans

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.Locale

private fun fmtL(n: Number) =
    "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

private fun loanStatusConfig(status: String): Triple<Color, Color, String> = when (status) {
    "disbursed" -> Triple(Color(0xFF1E40AF), Color(0xFFDBEAFE), "Active")
    "repaid"    -> Triple(Color(0xFF14532D), Color(0xFFDCFCE7), "Repaid")
    "approved"  -> Triple(Color(0xFF92400E), Color(0xFFFEF3C7), "Approved")
    "rejected"  -> Triple(Color(0xFF6B7280), Color(0xFFF3F4F6), "Rejected")
    else        -> Triple(Color(0xFF92400E), Color(0xFFFEF3C7), "Applied")
}

private fun repayStatusConfig(status: String): Triple<Color, Color, String> = when (status) {
    "paid"    -> Triple(Color(0xFF15803D), Color(0xFFDCFCE7), "Paid")
    "overdue" -> Triple(Color(0xFFDC2626), Color(0xFFFEE2E2), "Overdue")
    else      -> Triple(Color(0xFF92400E), Color(0xFFFEF3C7), "Pending")
}

@Composable
fun LoansScreen(vm: LoansViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // ── Header ──
            item {
                Text("My Loans", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A))
                Text("Qard Hasana — interest-free loans", fontSize = 14.sp,
                    color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 16.dp))
            }

            // ── Summary cards ──
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    LoanStatCard(
                        label    = "Active Loans",
                        value    = state.activeCount.toString(),
                        sub      = "currently disbursed",
                        color    = Color(0xFF1E40AF),
                        bg       = Color(0xFFDBEAFE),
                        modifier = Modifier.weight(1f)
                    )
                    LoanStatCard(
                        label    = "Outstanding",
                        value    = fmtL(state.totalOutstanding),
                        sub      = "total balance due",
                        color    = Color(0xFFDC2626),
                        bg       = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Empty state ──
            if (state.loans.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🤝", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No loans found", fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text("You have not applied for any loans yet.",
                            fontSize = 13.sp, color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center)
                    }
                }
            }

            // ── Loan cards ──
            items(state.loans, key = { it.id }) { loan ->
                LoanCard(loan = loan, onClick = { vm.selectLoan(loan) })
                Spacer(Modifier.height(10.dp))
            }
        }

        // ── Loan detail bottom sheet ──
        state.selectedLoan?.let { loan ->
            LoanDetailSheet(loan = loan, onClose = { vm.selectLoan(null) })
        }
    }
}

// ── Loan card ─────────────────────────────────────────────────────────────────

@Composable
fun LoanCard(loan: LoanItem, onClick: () -> Unit) {
    val (fg, bg, label) = loanStatusConfig(loan.status)
    val isDisbursed      = loan.status == "disbursed"

    // Next pending repayment
    val today       = java.util.Calendar.getInstance()
    val nextRepay   = loan.repaymentSchedule
        .filter { it.status == "pending" }
        .minByOrNull { it.dueDate }

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White,
        border   = BorderStroke(
            if (isDisbursed) 1.5.dp else 1.dp,
            if (isDisbursed) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp, 14.dp)) {

            // Top row: purpose + status
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(loan.purpose, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = Color(0xFF0F172A))
                    if (loan.disbursedDate.isNotEmpty()) {
                        Text("Disbursed: ${loan.disbursedDate}", fontSize = 11.sp,
                            color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = fg, modifier = Modifier.padding(10.dp, 3.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Amount row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LoanMiniStat("Loan Amount",   fmtL(loan.amount),             Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                if (isDisbursed)
                    LoanMiniStat("Outstanding", fmtL(loan.outstandingBalance), Color(0xFFDC2626), Color(0xFFFEF2F2), Modifier.weight(1f))
                if (loan.durationMonths > 0)
                    LoanMiniStat("Duration", "${loan.durationMonths} months",  Color(0xFF475569), Color(0xFFF8FAFC), Modifier.weight(1f))
            }

            // Next repayment banner
            if (isDisbursed && nextRepay != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color  = Color(0xFFFDF4FF),
                    shape  = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(10.dp, 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🔁 Next repayment", fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                            Text("Due: ${nextRepay.dueDate}", fontSize = 11.sp,
                                color = Color(0xFF9333EA))
                        }
                        Text(fmtL(nextRepay.amount), fontWeight = FontWeight.Bold,
                            fontSize = 13.sp, color = Color(0xFF7C3AED))
                    }
                }
            }
        }
    }
}

// ── Loan detail sheet ─────────────────────────────────────────────────────────

@Composable
fun LoanDetailSheet(loan: LoanItem, onClose: () -> Unit) {
    val (fg, bg, label) = loanStatusConfig(loan.status)
    val paidCount   = loan.repaymentSchedule.count { it.status == "paid" }
    val totalCount  = loan.repaymentSchedule.size
    val progressPct = if (totalCount > 0) paidCount.toFloat() / totalCount else 0f

    // Scrim
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(onClick = onClose)
    )

    // Sheet
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.90f)
            .align(Alignment.BottomCenter)
            .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Drag handle
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center) {
            Surface(Modifier.size(36.dp, 4.dp), RoundedCornerShape(2.dp), color = Color(0xFFE2E8F0)) {}
        }

        // Header
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(loan.purpose, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A))
                Spacer(Modifier.height(6.dp))
                Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = fg, modifier = Modifier.padding(10.dp, 3.dp))
                }
            }
            IconButton(onClick = onClose) {
                Surface(Modifier.size(32.dp), RoundedCornerShape(50.dp),
                    color = Color(0xFFF1F5F9)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", fontSize = 16.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        HorizontalDivider()

        // Scrollable content
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Loan summary stats ──
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                LoanMiniStat("Loan Amount",    fmtL(loan.amount),             Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                LoanMiniStat("Outstanding",    fmtL(loan.outstandingBalance), Color(0xFFDC2626), Color(0xFFFEF2F2), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                LoanMiniStat("Duration",  "${loan.durationMonths} months",     Color(0xFF475569), Color(0xFFF8FAFC), Modifier.weight(1f))
                LoanMiniStat("Disbursed", loan.disbursedDate.ifEmpty { "—" }, Color(0xFF475569), Color(0xFFF8FAFC), Modifier.weight(1f))
            }

            // ── Repayment progress ──
            if (totalCount > 0) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Repayment Progress", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("$paidCount / $totalCount paid", fontSize = 12.sp, color = Color(0xFF64748B))
                }
                LinearProgressIndicator(
                    progress        = { progressPct },
                    modifier        = Modifier.fillMaxWidth().height(8.dp),
                    color           = Color(0xFF15803D),
                    trackColor      = Color(0xFFE2E8F0),
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Repayment schedule ──
            if (loan.repaymentSchedule.isNotEmpty()) {
                Text("Repayment Schedule", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 10.dp))

                loan.repaymentSchedule.forEachIndexed { i, entry ->
                    val (sFg, sBg, sLabel) = repayStatusConfig(entry.status)
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = sBg,
                        border = BorderStroke(1.dp,
                            when (entry.status) {
                                "paid"    -> Color(0xFFBBF7D0)
                                "overdue" -> Color(0xFFFCA5A5)
                                else      -> Color(0xFFFDE68A)
                            }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp, 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Installment number circle
                                Surface(
                                    Modifier.size(28.dp), RoundedCornerShape(50.dp),
                                    color = when (entry.status) {
                                        "paid"    -> Color(0xFF15803D)
                                        "overdue" -> Color(0xFFDC2626)
                                        else      -> Color(0xFF92400E)
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${i + 1}", fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Column {
                                    Text("Due: ${entry.dueDate}", fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    if (entry.paidDate.isNotEmpty()) {
                                        Text("Paid: ${entry.paidDate}", fontSize = 11.sp,
                                            color = Color(0xFF15803D))
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(fmtL(entry.amount), fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp, color = Color(0xFF0F172A))
                                Surface(color = Color.White.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(99.dp)) {
                                    Text(sLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = sFg, modifier = Modifier.padding(7.dp, 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Notes
            if (loan.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color  = Color(0xFFFFFBEB),
                    shape  = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📝 ${loan.notes}", fontSize = 13.sp, color = Color(0xFF78350F),
                        modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Small reusable composables ────────────────────────────────────────────────

@Composable
fun LoanStatCard(label: String, value: String, sub: String,
                 color: Color, bg: Color, modifier: Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B), letterSpacing = 0.06.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
fun LoanMiniStat(label: String, value: String, color: Color, bg: Color, modifier: Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8), letterSpacing = 0.06.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}