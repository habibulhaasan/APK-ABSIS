// ui/dashboard/DashboardScreen.kt
package com.absis.capitalsync.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.Locale

fun fmt(n: Number) = "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

@Composable
fun DashboardScreen(
    onNavigateToInstallment: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val data    by vm.dashData.collectAsState()
    val notifs  by vm.notifications.collectAsState()
    val loading by vm.loading.collectAsState()
    val user    by vm.userData.collectAsState()

    val firstName = (user?.get("nameEnglish") as? String ?: "Member").split(" ").first()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // ── Admin banner ──
        if (vm.isOrgAdmin) {
            Surface(
                onClick = onNavigateToAdmin,
                shape = RoundedCornerShape(9.dp), color = Color(0xFFF5F3FF),
                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(Modifier.padding(9.dp, 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🛠", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("You're viewing your personal member dashboard",
                        fontSize = 12.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("Admin →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }
            }
        }

        // ── Page header ──
        Text("Welcome, $firstName 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text(vm.orgName, fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        data?.let { d ->

            // ── New member nudge ──
            if (d.isNewMember) {
                BannerCard(
                    bg = Color(0xFFEFF6FF), border = Color(0xFFBFDBFE),
                    onClick = onNavigateToInstallment
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("👋 Welcome to the fund!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E40AF))
                        Text("Make your first installment to join the capital pool.", fontSize = 12.sp, color = Color(0xFF3B82F6))
                    }
                    Text("Pay now →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                }
            }

            // ── Installment reminder ──
            if (!d.isNewMember && !d.paidThisMonth) {
                BannerCard(
                    bg = Color(0xFFFFFBEB), border = Color(0xFFFDE68A),
                    onClick = onNavigateToInstallment
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("📅 This month's installment not yet paid",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                        Text("Stay up to date — tap to pay now.", fontSize = 12.sp, color = Color(0xFFB45309))
                    }
                    Text("Pay →", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                }
            }

            // ── Loan repayment reminder ──
            d.nextRepayment?.let { rep ->
                BannerCard(
                    bg = Color(0xFFFDF4FF), border = Color(0xFFE9D5FF),
                    onClick = onNavigateToLoans
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🔁 Loan repayment due ${rep.dueDate}",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF7C3AED))
                        Text("${fmt(rep.amount)}${if (rep.loanPurpose.isNotEmpty()) " · ${rep.loanPurpose}" else ""}",
                            fontSize = 12.sp, color = Color(0xFF9333EA))
                    }
                    Text("View →", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }
            }

            // ── Stats grid ──
            Spacer(Modifier.height(4.dp))
            StatGrid(d, onNavigateToInstallment, onNavigateToLoans)

            // ── Latest distribution ──
            d.latestDist?.let { dist ->
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Latest Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(dist.periodLabel, fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniStat("MY SHARE",    fmt(d.myLatestShare), Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
                            MiniStat("GROSS PROFIT", fmt(dist.grossProfit), Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                            MiniStat("RATE/৳100",
                                if (dist.totalCapital > 0) fmt((dist.distributableProfit / dist.totalCapital) * 100) else "—",
                                Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Notifications ──
            if (notifs.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("🔔 Notifications", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 8.dp))
                        HorizontalDivider()
                        notifs.forEachIndexed { i, n ->
                            Column(Modifier
                                .fillMaxWidth()
                                .background(if (n["read"] == true) Color.White else Color(0xFFF0F9FF))
                                .padding(10.dp, 10.dp)) {
                                Text(n["message"] as? String ?: "", fontSize = 13.sp, lineHeight = 20.sp)
                                n["createdAt"]?.let {
                                    Text(it.toString(), fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                            if (i < notifs.lastIndex) HorizontalDivider(color = Color(0xFFF8FAFC))
                        }
                    }
                }
            }

            // ── Recent Payments ──
            if (d.myPayments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent Payments", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            TextButton(onClick = onNavigateToInstallment, contentPadding = PaddingValues(0.dp)) {
                                Text("Pay now →", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider()
                        d.myPayments.take(5).forEachIndexed { i, p ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (i % 2 == 0) Color.White else Color(0xFFFAFAFA))
                                    .padding(10.dp, 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.date, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(p.method, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(fmt(p.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    StatusBadge(p.status)
                                }
                            }
                            if (i < 4) HorizontalDivider(color = Color(0xFFF8FAFC))
                        }
                    }
                }
            }

            // ── Quick links ──
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onNavigateToInstallment,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.weight(1f)
                ) { Text("+ Pay Installment", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                if (vm.hasCapitalLedger) {
                    OutlinedButton(onClick = onNavigateToLedger, shape = RoundedCornerShape(8.dp)) {
                        Text("My Capital", fontSize = 13.sp)
                    }
                }
                if (vm.hasQardHasana) {
                    OutlinedButton(onClick = onNavigateToLoans, shape = RoundedCornerShape(8.dp)) {
                        Text("My Loans", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Small reusable composables ─────────────────────────────────────────────

@Composable
fun BannerCard(bg: Color, border: Color, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(10.dp), color = bg,
        border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(Modifier.padding(12.dp, 14.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun StatGrid(d: DashboardData, onInstallment: () -> Unit, onLoans: () -> Unit) {
    val cols = 2
    // Use LazyVerticalGrid in real implementation — simplified here for readability
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("My Capital", fmt(d.myCapital), "${d.myCapPct}% of total pool",
            Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
        if (d.myTotalProfit > 0)
            StatCard("Total Profit", fmt(d.myTotalProfit), "all distributions",
                Color(0xFF1D4ED8), Color(0xFFEFF6FF), Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Total Fund", fmt(d.totalCapital), "org-wide capital",
            Color(0xFF0F172A), Color(0xFFFAFAFA), Modifier.weight(1f))
        if (d.myVerified > 0)
            StatCard("Payments Made", d.myVerified.toString(),
                if (d.myPending > 0) "${d.myPending} pending" else "all verified",
                Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, sub: String, valueColor: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), letterSpacing = 0.07.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(sub, fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun MiniStat(label: String, value: String, valueColor: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp, 10.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "verified" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "pending"  -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        else       -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    }
    Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
        Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(7.dp, 1.dp))
    }
}