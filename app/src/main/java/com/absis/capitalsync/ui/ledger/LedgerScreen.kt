// ui/ledger/LedgerScreen.kt
package com.absis.capitalsync.ui.ledger

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

private fun fmtC(n: Number) =
    "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

private fun payTypeLabel(type: String): String = when (type) {
    "monthly"             -> "Monthly Installment"
    "entry_fee"           -> "Entry Fee"
    "reregistration_fee"  -> "Re-Registration Fee"
    else                  -> "Special Subscription"
}

private fun payTypeColor(type: String): Color = when (type) {
    "monthly"            -> Color(0xFF2563EB)
    "entry_fee"          -> Color(0xFF7C3AED)
    "reregistration_fee" -> Color(0xFF7C3AED)
    else                 -> Color(0xFF15803D)
}

@Composable
fun LedgerScreen(vm: LedgerViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header
        item {
            Text(
                "My Capital Ledger",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                "${state.orgName} · ${state.memberName} (${state.memberId})",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Summary stats
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                LedgerStatCard(
                    label    = "My Capital",
                    value    = fmtC(state.totalCapital),
                    sub      = "verified contributions",
                    color    = Color(0xFF15803D),
                    bg       = Color(0xFFF0FDF4),
                    modifier = Modifier.weight(1f)
                )
                LedgerStatCard(
                    label    = "Total Profit",
                    value    = fmtC(state.totalProfit),
                    sub      = "all distributions",
                    color    = Color(0xFF1D4ED8),
                    bg       = Color(0xFFEFF6FF),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section title
        item {
            Text(
                "Payment History",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = Color(0xFF0F172A),
                modifier   = Modifier.padding(bottom = 10.dp, top = 4.dp)
            )
        }

        // Empty state
        if (state.entries.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📋", fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No payments yet",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                    Text(
                        "Your payments will appear here once submitted.",
                        fontSize  = 13.sp,
                        color     = Color(0xFF94A3B8),
                        modifier  = Modifier.padding(top = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Ledger entries
        items(state.entries, key = { it.id }) { entry ->
            LedgerEntryCard(entry = entry, feeInAcct = state.feeInAcct)
            Spacer(Modifier.height(8.dp))
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun LedgerEntryCard(entry: LedgerEntry, feeInAcct: Boolean) {
    val isVerified = entry.status == "verified"
    val isPending  = entry.status == "pending"
    val isRejected = entry.status == "rejected"

    val borderColor = when {
        isVerified -> Color(0xFFBBF7D0)
        isPending  -> Color(0xFFFDE68A)
        else       -> Color(0xFFFCA5A5)
    }
    val bgColor = when {
        isVerified -> Color.White
        isPending  -> Color(0xFFFFFBEB)
        else       -> Color(0xFFFFF5F5)
    }

    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = bgColor,
        border   = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp, 12.dp)) {

            // Top row: type label + amount
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        payTypeLabel(entry.type),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 13.sp,
                        color      = payTypeColor(entry.type)
                    )
                    Text(
                        entry.date,
                        fontSize = 11.sp,
                        color    = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        fmtC(entry.amount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp,
                        color      = Color(0xFF0F172A)
                    )
                    val (sBg, sFg) = when {
                        isVerified -> Color(0xFFDCFCE7) to Color(0xFF15803D)
                        isPending  -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                        else       -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
                    }
                    Surface(
                        color    = sBg,
                        shape    = RoundedCornerShape(99.dp),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            entry.status,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = sFg,
                            modifier   = Modifier.padding(7.dp, 2.dp)
                        )
                    }
                }
            }

            // Details row
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        entry.method,
                        fontSize = 11.sp,
                        color    = Color(0xFF475569),
                        modifier = Modifier.padding(7.dp, 3.dp)
                    )
                }

                if (entry.gatewayFee > 0) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Fee: ${fmtC(entry.gatewayFee)}",
                            fontSize = 11.sp,
                            color    = Color(0xFF92400E),
                            modifier = Modifier.padding(7.dp, 3.dp)
                        )
                    }
                }

                if (entry.penaltyPaid > 0) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Late: ${fmtC(entry.penaltyPaid)}",
                            fontSize = 11.sp,
                            color    = Color(0xFFDC2626),
                            modifier = Modifier.padding(7.dp, 3.dp)
                        )
                    }
                }
            }

            // Paid months
            if (entry.paidMonths.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Months: ${entry.paidMonths.joinToString(", ") { it.replace("-", "/") }}",
                    fontSize = 11.sp,
                    color    = Color(0xFF64748B)
                )
            }

            // Running capital total
            if (isVerified && entry.isContribution && entry.runningTotal > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                    color    = Color(0xFFF1F5F9)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Net contribution",  fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        fmtC(entry.netAmount),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF15803D)
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Running capital total", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        fmtC(entry.runningTotal),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerStatCard(
    label:    String,
    value:    String,
    sub:      String,
    color:    Color,
    bg:       Color,
    modifier: Modifier
) {
    Surface(
        color    = bg,
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
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