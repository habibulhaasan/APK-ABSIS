package com.absis.capitalsync.ui.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header
            item {
                Text(
                    "My Loans",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Interest-free loans",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Summary cards
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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

            // Empty state
            if (state.loans.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🤝", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No loans found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "You have not applied for any loans yet.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Loan cards
            items(items = state.loans, key = { it.id }) { loan ->
                LoanCard(loan = loan, onClick = { vm.selectLoan(loan) })
                Spacer(Modifier.height(10.dp))
            }
        }

        // Loan detail bottom sheet
        state.selectedLoan?.let { loan ->
            LoanDetailSheet(loan = loan, onClose = { vm.selectLoan(null) })
        }
    }
}

// ── Loan card ──────────────────────────────────────────────────────────────────

@Composable
fun LoanCard(loan: LoanItem, onClick: () -> Unit) {
    val (fg, bg, label) = loanStatusConfig(loan.status)
    val isDisbursed = loan.status == "disbursed"

    val nextRepay = loan.repaymentSchedule
        .filter { it.status == "pending" }
        .minByOrNull { it.dueDate }

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(12.dp),
        color    = Color.White,
        border   = BorderStroke(
            width = if (isDisbursed) 1.5.dp else 1.dp,
            color = if (isDisbursed) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        loan.purpose,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF0F172A)
                    )
                    if (loan.disbursedDate.isNotEmpty()) {
                        Text(
                            "Disbursed: ${loan.disbursedDate}",
                            fontSize = 11.sp,
                            color    = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text(
                        label,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = fg,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Amount row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LoanMiniStat(
                    label    = "Loan Amount",
                    value    = fmtL(loan.amount),
                    color    = Color(0xFF0F172A),
                    bg       = Color(0xFFF8FAFC),
                    modifier = Modifier.weight(1f)
                )
                if (isDisbursed) {
                    LoanMiniStat(
                        label    = "Outstanding",
                        value    = fmtL(loan.outstandingBalance),
                        color    = Color(0xFFDC2626),
                        bg       = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (loan.durationMonths > 0) {
                    LoanMiniStat(
                        label    = "Duration",
                        value    = "${loan.durationMonths} months",
                        color    = Color(0xFF475569),
                        bg       = Color(0xFFF8FAFC),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Next repayment banner
            if (isDisbursed && nextRepay != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color    = Color(0xFFFDF4FF),
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🔁 Next repayment",
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF7C3AED)
                            )
                            Text(
                                "Due: ${nextRepay.dueDate}",
                                fontSize = 11.sp,
                                color    = Color(0xFF9333EA)
                            )
                        }
                        Text(
                            fmtL(nextRepay.amount),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp,
                            color      = Color(0xFF7C3AED)
                        )
                    }
                }
            }
        }
    }
}

// ── Loan detail sheet ──────────────────────────────────────────────────────────

@Composable
fun LoanDetailSheet(loan: LoanItem, onClose: () -> Unit) {
    val (fg, bg, label) = loanStatusConfig(loan.status)
    val paidCount   = loan.repaymentSchedule.count { it.status == "paid" }
    val totalCount  = loan.repaymentSchedule.size
    val progressPct = if (totalCount > 0) paidCount.toFloat() / totalCount else 0f

    Box(modifier = Modifier.fillMaxSize()) {

        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable { onClose() }
        )

        // Sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .align(Alignment.BottomCenter)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(width = 36.dp, height = 4.dp),
                    shape    = RoundedCornerShape(2.dp),
                    color    = Color(0xFFE2E8F0)
                ) {}
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        loan.purpose,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0F172A)
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(99.dp)
                    ) {
                        Text(
                            label,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = fg,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape    = RoundedCornerShape(50.dp),
                        color    = Color(0xFFF1F5F9)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✕", fontSize = 16.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            HorizontalDivider()

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Loan summary stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LoanMiniStat(
                        label    = "Loan Amount",
                        value    = fmtL(loan.amount),
                        color    = Color(0xFF0F172A),
                        bg       = Color(0xFFF8FAFC),
                        modifier = Modifier.weight(1f)
                    )
                    LoanMiniStat(
                        label    = "Outstanding",
                        value    = fmtL(loan.outstandingBalance),
                        color    = Color(0xFFDC2626),
                        bg       = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LoanMiniStat(
                        label    = "Duration",
                        value    = "${loan.durationMonths} months",
                        color    = Color(0xFF475569),
                        bg       = Color(0xFFF8FAFC),
                        modifier = Modifier.weight(1f)
                    )
                    LoanMiniStat(
                        label    = "Disbursed",
                        value    = loan.disbursedDate.ifEmpty { "—" },
                        color    = Color(0xFF475569),
                        bg       = Color(0xFFF8FAFC),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Repayment progress
                if (totalCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Repayment Progress",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                        Text(
                            "$paidCount / $totalCount paid",
                            fontSize = 12.sp,
                            color    = Color(0xFF64748B)
                        )
                    }
                    LinearProgressIndicator(
                        progress   = { progressPct },
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color      = Color(0xFF15803D),
                        trackColor = Color(0xFFE2E8F0)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Repayment schedule
                if (loan.repaymentSchedule.isNotEmpty()) {
                    Text(
                        "Repayment Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        modifier   = Modifier.padding(bottom = 10.dp)
                    )

                    loan.repaymentSchedule.forEachIndexed { i, entry ->
                        val (sFg, sBg, sLabel) = repayStatusConfig(entry.status)
                        val borderColor = when (entry.status) {
                            "paid"    -> Color(0xFFBBF7D0)
                            "overdue" -> Color(0xFFFCA5A5)
                            else      -> Color(0xFFFDE68A)
                        }
                        val circleColor = when (entry.status) {
                            "paid"    -> Color(0xFF15803D)
                            "overdue" -> Color(0xFFDC2626)
                            else      -> Color(0xFF92400E)
                        }

                        Surface(
                            shape    = RoundedCornerShape(10.dp),
                            color    = sBg,
                            border   = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 7.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical   = 10.dp
                                ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(28.dp),
                                        shape    = RoundedCornerShape(50.dp),
                                        color    = circleColor
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${i + 1}",
                                                fontSize   = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color      = Color.White
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            "Due: ${entry.dueDate}",
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Color(0xFF0F172A)
                                        )
                                        if (entry.paidDate.isNotEmpty()) {
                                            Text(
                                                "Paid: ${entry.paidDate}",
                                                fontSize = 11.sp,
                                                color    = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        fmtL(entry.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 13.sp,
                                        color      = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        color = Color.White.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(99.dp)
                                    ) {
                                        Text(
                                            sLabel,
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = sFg,
                                            modifier   = Modifier.padding(
                                                horizontal = 7.dp,
                                                vertical   = 2.dp
                                            )
                                        )
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
                        color    = Color(0xFFFFFBEB),
                        shape    = RoundedCornerShape(8.dp),
                        border   = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "📝 ${loan.notes}",
                            fontSize = 13.sp,
                            color    = Color(0xFF78350F),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        } // end Sheet Column
    } // end Box
}

// ── Reusable composables ───────────────────────────────────────────────────────

@Composable
fun LoanStatCard(
    label:    String,
    value:    String,
    sub:      String,
    color:    Color,
    bg:       Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color    = bg,
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                label.uppercase(),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF64748B),
                letterSpacing = 0.06.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(
                sub,
                fontSize = 11.sp,
                color    = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
fun LoanMiniStat(
    label:    String,
    value:    String,
    color:    Color,
    bg:       Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color    = bg,
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                label.uppercase(),
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF94A3B8),
                letterSpacing = 0.06.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}