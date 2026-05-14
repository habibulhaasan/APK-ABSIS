package com.absis.capitalsync.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.absis.capitalsync.ui.common.PullToRefreshLayout
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun fmt(n: Number) = "৳${NumberFormat.getNumberInstance(Locale.US).format(n.toLong())}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToInstallment: () -> Unit,
    onNavigateToLedger:      () -> Unit,
    onNavigateToLoans:       () -> Unit,
    onNavigateToAdmin:       () -> Unit,
    onNavigateToExpenses:    () -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val data    by vm.dashData.collectAsState()
    val notifs  by vm.notifications.collectAsState()
    val loading by vm.loading.collectAsState()
    val user    by vm.userData.collectAsState()

    // ── Trim "Md." and get first name ──
    val rawName = user?.get("nameEnglish") as? String ?: "Member"
    val trimmedName = rawName
        .replace(Regex("^(Md\\.?|Mohammed|Mohammad)\\s+", RegexOption.IGNORE_CASE), "")
        .split(" ")
        .firstOrNull() ?: "Member"

    var showNotifSheet by remember { mutableStateOf(false) }

    // When notification sheet is opened, mark them as read automatically
    LaunchedEffect(showNotifSheet) {
        if (showNotifSheet) {
            vm.markNotificationsAsRead()
        }
    }

    PullToRefreshLayout(
        isRefreshing = loading,
        onRefresh    = { vm.refresh() },
        modifier     = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (vm.isOrgAdmin) {
                Surface(
                    onClick = onNavigateToAdmin,
                    shape   = RoundedCornerShape(9.dp),
                    color   = Color(0xFFF5F3FF),
                    border  = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🛠", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("You're viewing your personal member dashboard", fontSize = 12.sp, color = Color(0xFF6D28D9), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("Admin →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                    }
                }
            }

            // ── Greeting Header ──
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Assalamu Alaikum, $trimmedName",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF0F172A)
                    )
                    Text(vm.orgName, fontSize = 14.sp, color = Color.Black)
                }
                
                val hasUnread = notifs.any { it["read"] != true }
                IconButton(
                    onClick = { showNotifSheet = true },
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White).border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    BadgedBox(badge = { if (hasUnread) Badge(containerColor = Color(0xFFEF4444), modifier = Modifier.offset(x = (-4).dp, y = 4.dp)) }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color(0xFF475569))
                    }
                }
            }

            if (loading && data == null) {
                Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF2563EB)) }
                return@Column
            }

            data?.let { d ->
                if (d.isNewMember) {
                    BannerCard(bg = Color(0xFFEFF6FF), border = Color(0xFFBFDBFE), onClick = onNavigateToInstallment) {
                        Column(Modifier.weight(1f)) {
                            Text("👋 Welcome to the fund!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E40AF))
                            Text("Make your first installment to join the capital pool.", fontSize = 12.sp, color = Color(0xFF3B82F6))
                        }
                        Text("Pay now →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    }
                }

                if (!d.isNewMember && !d.paidThisMonth) {
                    BannerCard(bg = Color(0xFFFFFBEB), border = Color(0xFFFDE68A), onClick = onNavigateToInstallment) {
                        Column(Modifier.weight(1f)) {
                            Text("📅 This month's installment not yet paid", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                            Text("Stay up to date — tap to pay now.", fontSize = 12.sp, color = Color(0xFFB45309))
                        }
                        Text("Pay →", fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    }
                }

                Spacer(Modifier.height(4.dp))
                StatGrid(d)

                if (d.usedExpenses > 0) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        onClick = onNavigateToExpenses,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("🧾 Organisation Expenses", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("View all →", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("TOTAL SPENT", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(fmt(d.usedExpenses), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                                        Text("${d.expenseCount} entries", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("THIS MONTH", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(fmt(d.expensesThisMonth), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC2410C))
                                        Text(SimpleDateFormat("MMM yyyy", Locale.US).format(Date()), fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                d.latestDist?.let { dist ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Latest Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(dist.periodLabel, fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MiniStat("MY SHARE", fmt(d.myLatestShare), Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
                                MiniStat("GROSS PROFIT", fmt(dist.grossProfit), Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                                MiniStat("RATE/৳100", if (dist.totalCapital > 0) fmt((dist.distributableProfit / dist.totalCapital) * 100) else "—", Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (d.myPayments.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Payments", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                TextButton(onClick = onNavigateToInstallment, contentPadding = PaddingValues(0.dp)) {
                                    Text("Pay now →", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            d.myPayments.take(5).forEachIndexed { i, p ->
                                Row(
                                    Modifier.fillMaxWidth().background(if (i % 2 == 0) Color.White else Color(0xFFFAFAFA)).padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.date, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                        Spacer(Modifier.height(2.dp))
                                        Text(p.method, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(fmt(p.amount), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(Modifier.height(4.dp))
                                        StatusBadge(p.status)
                                    }
                                }
                                if (i < 4 && i < d.myPayments.lastIndex) HorizontalDivider(color = Color(0xFFF8FAFC))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick  = onNavigateToInstallment, shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)), modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("+ Pay Installment", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    
                    if (vm.hasCapitalLedger) {
                        OutlinedButton(
                            onClick = onNavigateToLedger, shape = RoundedCornerShape(8.dp), modifier = Modifier.height(48.dp)
                        ) { Text("My Capital", fontSize = 13.sp, color = Color(0xFF475569)) }
                    }
                }
            }
        }
    }

    if (showNotifSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotifSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("🔔 Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 16.dp))
                
                if (notifs.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        Text("You're all caught up! No new notifications.", fontSize = 14.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    notifs.forEach { n ->
                        Surface(
                            color = if (n["read"] == true) Color.White else Color(0xFFF0F9FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (n["read"] == true) Color(0xFFE2E8F0) else Color(0xFFBAE6FD)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(n["message"] as? String ?: "", fontSize = 14.sp, color = Color(0xFF0F172A), lineHeight = 20.sp)
                                val ts = n["createdAt"] as? Timestamp
                                val dateStr = ts?.toDate()?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(it) } ?: "Just now"
                                Text(dateStr, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Small reusable composables ────────────────────────────────────────────────

@Composable
fun BannerCard(
    bg:      Color,
    border:  Color,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(10.dp),
        color    = bg,
        border   = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(Modifier.padding(12.dp, 14.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun StatGrid(d: DashboardData) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("My Capital", fmt(d.myCapital), "${d.myCapPct}% of pool", Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
        if (d.myTotalProfit > 0) {
            StatCard("Total Profit", fmt(d.myTotalProfit), "distributions", Color(0xFF1D4ED8), Color(0xFFEFF6FF), Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Total Fund", fmt(d.totalCapital), "org-wide", Color(0xFF0F172A), Color(0xFFFAFAFA), Modifier.weight(1f))
        if (d.myVerified > 0) {
            StatCard("Payments Made", d.myVerified.toString(), if (d.myPending > 0) "${d.myPending} pending" else "verified", Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
        }
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
        Column(Modifier.padding(10.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
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
        Text(status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(8.dp, 3.dp))
    }
}