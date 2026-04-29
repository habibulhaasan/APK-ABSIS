// ui/investments/InvestmentsScreen.kt
package com.absis.capitalsync.ui.investments

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

// ── Data models ───────────────────────────────────────────────────────────────

data class Project(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val sector: String,
    val status: String,           // proposed | active | completed | cancelled
    val returnType: String,       // periodic | lumpsum
    val investedAmount: Double,
    val expectedReturnPct: Double,
    val actualReturnAmount: Double?,
    val profit: Double?,
    val totalReturns: Double,
    val totalExpenses: Double,
    val startDate: String,
    val completedDate: String,
    val notes: String,
    val participatingMembers: Any?, // "all" or List<String>
    val fundSources: Map<String, Double>?,
    val fundSource: String,
)

data class MyShare(
    val capital: Double,
    val capPct: Double,
    val effectiveInvested: Double,
    val profitShare: Double,
    val totalCapital: Double,
)

data class Transaction(
    val id: String,
    val kind: String,             // "return" | "expense"
    val description: String,
    val category: String,
    val amount: Double,
    val date: String,
    val distributedInDistributionId: String?,
)

val STATUS_CONFIG = mapOf(
    "proposed"  to Triple(Color(0xFF92400E), Color(0xFFFEF3C7), Color(0xFFF59E0B)),
    "active"    to Triple(Color(0xFF1E40AF), Color(0xFFDBEAFE), Color(0xFF2563EB)),
    "completed" to Triple(Color(0xFF14532D), Color(0xFFDCFCE7), Color(0xFF16A34A)),
    "cancelled" to Triple(Color(0xFF6B7280), Color(0xFFF3F4F6), Color(0xFF9CA3AF)),
)

fun fmt(n: Number) = "৳${"%,.0f".format(n.toDouble())}"
fun fmtSigned(n: Double) = if (n < 0) "−${fmt(-n)}" else "+${fmt(n)}"
fun pct(n: Double) = "${"%.2f".format(n)}%"
fun cap(s: String) = s.replaceFirstChar { it.uppercase() }.replace("_", " ")

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun InvestmentsScreen(vm: InvestmentsViewModel = hiltViewModel()) {
    val projects  by vm.projects.collectAsState()
    val loading   by vm.loading.collectAsState()
    val filter    by vm.filter.collectAsState()
    val myCapital by vm.myCapital.collectAsState()
    val selected  by vm.selectedProject.collectAsState()

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💹", fontSize = 32.sp)
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
                Text("Loading projects…", color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 8.dp))
            }
        }
        return
    }

    val myProjects     = projects.filter { vm.getMyShare(it) != null }
    val totalMyProfit  = myProjects
        .filter { it.status == "completed" || it.returnType == "periodic" }
        .sumOf { vm.getMyShare(it)?.profitShare ?: 0.0 }
    val totalOrgInvested = projects.sumOf { it.investedAmount }
    val activeCount    = projects.count { it.status == "active" }

    val filtered = projects.filter { p ->
        when (filter) {
            "mine"      -> vm.getMyShare(p) != null
            "active"    -> p.status == "active"
            "completed" -> p.status == "completed"
            else        -> true
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

            // ── Header ──
            item {
                Text("Investment Projects", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A), modifier = Modifier.padding(top = 16.dp))
                Text("Track the organisation's portfolio and your personal share.",
                    fontSize = 14.sp, color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 16.dp))
            }

            // ── Summary stats ──
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    InvStat("My Capital", fmt(myCapital), "Your verified contributions",
                        Color(0xFF92400E), Color(0xFFFEF3C7), Modifier.weight(1f))
                    InvStat("My Projects", myProjects.size.toString(), "of ${projects.size} total",
                        Color(0xFF1E40AF), Color(0xFFDBEAFE), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    InvStat(
                        label = "My P&L",
                        value = if (totalMyProfit != 0.0) fmtSigned(totalMyProfit) else "—",
                        sub   = "Across active & completed",
                        color = if (totalMyProfit >= 0) Color(0xFF15803D) else Color(0xFFDC2626),
                        bg    = if (totalMyProfit >= 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    )
                    InvStat("Org Invested", fmt(totalOrgInvested), "$activeCount active project(s)",
                        Color(0xFF0F172A), Color(0xFFF8FAFC), Modifier.weight(1f))
                }
            }

            // ── No capital warning ──
            if (myCapital == 0.0) {
                item {
                    Surface(
                        color = Color(0xFFFFFBEB), shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text("⚠️ You have no verified capital yet — your profit share will be calculated once your installments are verified.",
                            fontSize = 13.sp, color = Color(0xFF92400E), modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // ── Filter chips ──
            item {
                val tabs = listOf(
                    "all"       to "All (${projects.size})",
                    "mine"      to "My Projects (${myProjects.size})",
                    "active"    to "Active (${activeCount})",
                    "completed" to "Completed (${projects.count { it.status == "completed" }})",
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(tabs) { (key, label) ->
                        val sel = filter == key
                        Surface(
                            onClick = { vm.setFilter(key) },
                            shape = RoundedCornerShape(99.dp),
                            color = if (sel) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) Color.White else Color(0xFF64748B),
                                modifier = Modifier.padding(14.dp, 6.dp))
                        }
                    }
                }
            }

            // ── Empty state ──
            if (filtered.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💹", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (filter == "mine") "You don't participate in any projects yet"
                            else "No projects found",
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                        )
                        Text(
                            if (filter == "mine") "Your capital will be counted once the admin creates projects and your payments are verified."
                            else "No investment projects match this filter.",
                            fontSize = 13.sp, color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // ── Project cards ──
            items(filtered, key = { it.id }) { p ->
                val myShare    = vm.getMyShare(p)
                val isPeriodic = p.returnType == "periodic"
                val netProfit  = if (isPeriodic) p.totalReturns - p.totalExpenses
                                 else p.profit

                ProjectCard(
                    project   = p,
                    myShare   = myShare,
                    netProfit = netProfit,
                    onClick   = { vm.selectProject(p) }
                )
                Spacer(Modifier.height(10.dp))
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // ── Bottom sheet detail ──
        selected?.let { proj ->
            ProjectDetailSheet(
                project = proj,
                myShare = vm.getMyShare(proj),
                orgId   = vm.orgId,
                onClose = { vm.selectProject(null) }
            )
        }
    }
}

// ── Project Card ──────────────────────────────────────────────────────────────

@Composable
fun ProjectCard(project: Project, myShare: MyShare?, netProfit: Double?, onClick: () -> Unit) {
    val (statusFg, statusBg, statusDot) = STATUS_CONFIG[project.status]
        ?: Triple(Color(0xFF92400E), Color(0xFFFEF3C7), Color(0xFFF59E0B))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (myShare != null) Color(0xFFFFFDF5) else Color.White,
        border = BorderStroke(
            if (myShare != null) 1.5.dp else 1.dp,
            if (myShare != null) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp, 14.dp)) {

            // Top row: title + net profit / invested
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(project.title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        if (myShare != null) {
                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(99.dp)) {
                                Text("👤 Participating", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E), modifier = Modifier.padding(7.dp, 2.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        // Status badge
                        Surface(color = statusBg, shape = RoundedCornerShape(99.dp)) {
                            Row(Modifier.padding(10.dp, 3.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Surface(Modifier.size(6.dp), RoundedCornerShape(50.dp), color = statusDot) {}
                                Text(cap(project.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusFg)
                            }
                        }
                        Text(project.type, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        if (project.sector.isNotEmpty())
                            Text("· ${project.sector}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(
                            if (project.returnType == "periodic") "🔄 Periodic" else "📦 Lump Sum",
                            fontSize = 11.sp,
                            color = if (project.returnType == "periodic") Color(0xFF7E22CE) else Color(0xFF15803D)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (netProfit != null) "Org net" else "Invested",
                        fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        if (netProfit != null) fmtSigned(netProfit) else fmt(project.investedAmount),
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = when {
                            netProfit == null    -> Color(0xFF92400E)
                            netProfit >= 0       -> Color(0xFF15803D)
                            else                 -> Color(0xFFDC2626)
                        }
                    )
                }
            }

            // My share row
            if (myShare != null) {
                HorizontalDivider(color = Color(0xFFFDE68A), modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "MY CAPITAL"     to fmt(myShare.capital),
                        "MY SHARE"       to pct(myShare.capPct),
                        "EFF. INVESTED"  to fmt(myShare.effectiveInvested),
                        "MY P&L"         to if (myShare.profitShare != 0.0) fmtSigned(myShare.profitShare) else "—",
                    ).forEach { (lbl, value) ->
                        Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(6.dp, 6.dp)) {
                                Text(lbl, fontSize = 9.sp, color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp)
                                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = when {
                                        value.startsWith("+") -> Color(0xFF15803D)
                                        value.startsWith("−") -> Color(0xFFDC2626)
                                        else -> Color(0xFF0F172A)
                                    },
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Row {
                    Text("Invested: ", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Text(fmt(project.investedAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E))
                    if (project.expectedReturnPct > 0) {
                        Text(" · Expected: ", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(pct(project.expectedReturnPct), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Project Detail Bottom Sheet ───────────────────────────────────────────────

@Composable
fun ProjectDetailSheet(
    project: Project,
    myShare: MyShare?,
    orgId: String,
    onClose: () -> Unit
) {
    val isPeriodic = project.returnType == "periodic"
    val netProfit  = if (isPeriodic) project.totalReturns - project.totalExpenses else project.profit
    var activeTab  by remember { mutableStateOf("overview") }

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
            .fillMaxHeight(0.92f)
            .align(Alignment.BottomCenter)  // NOTE: this needs to be inside a Box
            .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Drag handle
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center) {
            Surface(Modifier.size(36.dp, 4.dp), RoundedCornerShape(2.dp), color = Color(0xFFE2E8F0)) {}
        }

        // Header
        Row(
            Modifier.fillMaxWidth().padding(12.dp, 12.dp, 12.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(project.title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    val (fg, bg, dot) = STATUS_CONFIG[project.status]
                        ?: Triple(Color(0xFF92400E), Color(0xFFFEF3C7), Color(0xFFF59E0B))
                    Surface(color = bg, shape = RoundedCornerShape(99.dp)) {
                        Row(Modifier.padding(10.dp, 3.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Surface(Modifier.size(6.dp), RoundedCornerShape(50.dp), color = dot) {}
                            Text(cap(project.status), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                    Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                        Text(project.type, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8), modifier = Modifier.padding(10.dp, 3.dp))
                    }
                    Surface(
                        color = if (isPeriodic) Color(0xFFFAF5FF) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isPeriodic) "🔄 Periodic" else "📦 Lump Sum",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (isPeriodic) Color(0xFF7E22CE) else Color(0xFF14532D),
                            modifier = Modifier.padding(10.dp, 3.dp))
                    }
                }
            }
            IconButton(onClick = onClose) {
                Surface(Modifier.size(32.dp), RoundedCornerShape(50.dp),
                    color = Color(0xFFF1F5F9), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✕", fontSize = 16.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        // Tab bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            listOf("overview" to "Overview", "transactions" to if (isPeriodic) "Transactions" else "Returns")
                .forEach { (id, label) ->
                    val sel = activeTab == id
                    TextButton(
                        onClick = { activeTab = id },
                        modifier = Modifier
                            .padding(0.dp)
                            .border(
                                width = 0.dp,
                                color = Color.Transparent,
                                shape = RoundedCornerShape(0.dp)
                            )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, fontSize = 13.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) Color(0xFF2563EB) else Color(0xFF64748B))
                            if (sel) Surface(Modifier.height(2.dp).width(40.dp), color = Color(0xFF2563EB)) {}
                        }
                    }
                }
        }

        // Tab content
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            when (activeTab) {
                "overview"     -> OverviewTab(project, myShare, netProfit, isPeriodic)
                "transactions" -> TransactionsTab(project, orgId, isPeriodic)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Overview Tab ──────────────────────────────────────────────────────────────

@Composable
fun OverviewTab(project: Project, myShare: MyShare?, netProfit: Double?, isPeriodic: Boolean) {
    if (project.description.isNotEmpty()) {
        Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(project.description, fontSize = 13.sp, color = Color(0xFF475569),
                lineHeight = 20.sp, modifier = Modifier.padding(12.dp))
        }
    }

    Text("PROJECT FINANCIALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B),
        letterSpacing = 0.07.sp, modifier = Modifier.padding(bottom = 8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        InvStat("Invested", fmt(project.investedAmount), "", Color(0xFF92400E), Color(0xFFFEF3C7), Modifier.weight(1f))
        if (project.expectedReturnPct > 0)
            InvStat("Expected", pct(project.expectedReturnPct),
                fmt(project.investedAmount * project.expectedReturnPct / 100),
                Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
    }

    if (isPeriodic) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            InvStat("Returns", fmt(project.totalReturns), "", Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
            InvStat("Expenses", fmt(project.totalExpenses), "", Color(0xFFDC2626), Color(0xFFFEF2F2), Modifier.weight(1f))
        }
        netProfit?.let { np ->
            InvStat("Net Profit / Loss", fmtSigned(np), "",
                if (np >= 0) Color(0xFF15803D) else Color(0xFFDC2626),
                if (np >= 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                Modifier.fillMaxWidth().padding(bottom = 10.dp))
        }
    } else {
        project.actualReturnAmount?.let { ret ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                InvStat("Returned", fmt(ret), "", Color(0xFF1D4ED8), Color(0xFFEFF6FF), Modifier.weight(1f))
                netProfit?.let { np ->
                    InvStat(
                        if (np >= 0) "Profit" else "Loss",
                        fmtSigned(np),
                        "${if (np >= 0) "+" else ""}${pct(if (project.investedAmount > 0) (np / project.investedAmount) * 100 else 0.0)} ROI",
                        if (np >= 0) Color(0xFF15803D) else Color(0xFFDC2626),
                        if (np >= 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // My share card
    if (myShare != null) {
        Spacer(Modifier.height(6.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, Color(0xFFFDE68A)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column {
                Surface(color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("👤  Your Share", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                    }
                }
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InvStat("Your Capital", fmt(myShare.capital), "", Color(0xFF0F172A), Color.White, Modifier.weight(1f))
                    InvStat("Capital Share", pct(myShare.capPct), "", Color(0xFF0F172A), Color.White, Modifier.weight(1f))
                }
                Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InvStat("Eff. Invested", fmt(myShare.effectiveInvested), "", Color(0xFF0F172A), Color.White, Modifier.weight(1f))
                    InvStat(
                        if (myShare.profitShare >= 0) "Your Profit" else "Your Loss",
                        fmtSigned(myShare.profitShare), "",
                        if (myShare.profitShare >= 0) Color(0xFF15803D) else Color(0xFFDC2626),
                        if (myShare.profitShare >= 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    } else {
        Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text("ℹ️ You are not a direct participant in this project. Your capital still contributes to the organisation.",
                fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(14.dp))
        }
    }

    // Dates + notes
    if (project.startDate.isNotEmpty() || project.completedDate.isNotEmpty() || project.notes.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            if (project.startDate.isNotEmpty())
                Text("📅 Started: ${project.startDate}", fontSize = 12.sp, color = Color(0xFF64748B))
            if (project.completedDate.isNotEmpty())
                Text("🏁 Completed: ${project.completedDate}", fontSize = 12.sp, color = Color(0xFF64748B))
        }
        if (project.notes.isNotEmpty()) {
            Surface(color = Color(0xFFFFFBEB), shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()) {
                Text("Notes: ${project.notes}", fontSize = 13.sp, color = Color(0xFF78350F),
                    modifier = Modifier.padding(12.dp))
            }
        }
    }
}

// ── Transactions Tab ──────────────────────────────────────────────────────────

@Composable
fun TransactionsTab(project: Project, orgId: String, isPeriodic: Boolean, vm: InvestmentsViewModel = hiltViewModel()) {
    val txns by vm.transactions.collectAsState()

    LaunchedEffect(project.id) { vm.loadTransactions(project.id, orgId) }

    if (!isPeriodic && project.actualReturnAmount != null) {
        Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text("📦 Lump-sum project. Final return of ${fmt(project.actualReturnAmount)} was recorded when completed.",
                fontSize = 13.sp, color = Color(0xFF1E40AF), modifier = Modifier.padding(12.dp))
        }
    }

    val returns  = txns.filter { it.kind == "return" }
    val expenses = txns.filter { it.kind == "expense" }
    val totalR   = returns.sumOf { it.amount }
    val totalE   = expenses.sumOf { it.amount }
    val net      = totalR - totalE

    // Mini summary
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        InvStat("Returns",  fmt(totalR), "", Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
        InvStat("Expenses", fmt(totalE), "", Color(0xFFDC2626), Color(0xFFFEF2F2), Modifier.weight(1f))
        InvStat("Net", fmtSigned(net), "",
            if (net >= 0) Color(0xFF15803D) else Color(0xFFDC2626),
            if (net >= 0) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
            Modifier.weight(1f))
    }

    if (txns.isEmpty()) {
        Surface(color = Color(0xFFFAFAFA), shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()) {
            Text("No transactions recorded yet.", fontSize = 13.sp, color = Color(0xFF94A3B8),
                modifier = Modifier.padding(28.dp), )
        }
    } else {
        txns.forEach { tx ->
            val isReturn = tx.kind == "return"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isReturn) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, if (isReturn) Color(0xFFBBF7D0) else Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)
            ) {
                Row(Modifier.padding(10.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(32.dp), RoundedCornerShape(50.dp),
                        color = if (isReturn) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (isReturn) "↑" else "↓", fontSize = 15.sp,
                                color = if (isReturn) Color(0xFF15803D) else Color(0xFFDC2626))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tx.description.ifEmpty { cap(tx.category) },
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)) {
                            Surface(
                                color = if (isReturn) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(cap(tx.category), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = if (isReturn) Color(0xFF15803D) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(7.dp, 2.dp))
                            }
                            if (isReturn && tx.distributedInDistributionId != null) {
                                Surface(color = Color(0xFFDBEAFE), shape = RoundedCornerShape(5.dp)) {
                                    Text("Distributed", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8), modifier = Modifier.padding(7.dp, 2.dp))
                                }
                            }
                        }
                        Text(tx.date, fontSize = 11.sp, color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(
                        "${if (isReturn) "+" else "−"}${fmt(tx.amount)}",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = if (isReturn) Color(0xFF15803D) else Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

// ── Reusable stat tile ────────────────────────────────────────────────────────

@Composable
fun InvStat(label: String, value: String, sub: String, color: Color, bg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp, 14.dp)) {
            Text(label.uppercase(), fontSize = 11.sp, color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.07.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            if (sub.isNotEmpty())
                Text(sub, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 3.dp))
        }
    }
}