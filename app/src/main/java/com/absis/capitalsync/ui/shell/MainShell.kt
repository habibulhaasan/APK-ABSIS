// ui/shell/MainShell.kt  — full replacement
package com.absis.capitalsync.ui.shell

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.absis.capitalsync.core.navigation.Screen

// ── 4 bottom-nav tabs ─────────────────────────────────────────────────────────
private data class NavTab(val route: String, val label: String, val icon: String)

private val NAV_TABS = listOf(
    NavTab(Screen.Dashboard.route,   "Home",    "🏠"),
    NavTab(Screen.Installment.route, "Pay",     "💳"),
    NavTab(Screen.Profile.route,     "Profile", "👤"),
    NavTab("more",                   "More",    "☰"),
)

// Routes that show the bottom bar
private val NAV_ROUTES = setOf(
    Screen.Dashboard.route, Screen.Installment.route,
    Screen.Profile.route,   Screen.Investments.route,
    Screen.Expenses.route,  Screen.Loans.route,
    Screen.Ledger.route,
)

// ── Shell ─────────────────────────────────────────────────────────────────────
@Composable
fun MainShell(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backstackEntry?.destination?.route
    val showNav        = currentRoute in NAV_ROUTES || currentRoute == "more"

    var showMoreSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (showNav) {
                BottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        if (route == "more") {
                            showMoreSheet = true
                        } else if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = Color(0xFFF8FAFC),
    ) { padding ->
        content(padding)
    }

    // ── "More" bottom sheet ───────────────────────────────────────────────────
    if (showMoreSheet) {
        MoreBottomSheet(
            currentRoute = currentRoute,
            onNavigate   = { route ->
                showMoreSheet = false
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
            },
            onDismiss = { showMoreSheet = false }
        )
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────
@Composable
private fun BottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        NAV_TABS.forEach { tab ->
            val selected = when (tab.route) {
                "more"  -> currentRoute in setOf(
                    Screen.Investments.route, Screen.Expenses.route,
                    Screen.Loans.route, Screen.Ledger.route
                )
                else    -> currentRoute == tab.route
            }
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(tab.route) },
                icon     = { Text(tab.icon, fontSize = 20.sp) },
                label    = {
                    Text(tab.label, fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor   = Color(0xFF2563EB),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor      = Color(0xFFEFF6FF),
                )
            )
        }
    }
}

// ── More bottom sheet ─────────────────────────────────────────────────────────
@Composable
private fun MoreBottomSheet(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // We wrap the sheet in a Box to use Alignment.BottomCenter
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Scrim (Background overlay)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(onClick = onDismiss)
        )

        // 2. Sheet - Now valid because it's inside a BoxScope
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomCenter) // This now works!
                .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(bottom = 32.dp)
                .clickable(enabled = false) { } // Prevents clicks on sheet from dismissing it
        ) {
            // Handle
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center) {
                Surface(Modifier.size(36.dp, 4.dp), RoundedCornerShape(2.dp), color = Color(0xFFE2E8F0)) {}
            }

            Text("More", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = Color(0xFF0F172A), modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 12.dp))

            HorizontalDivider()

            val items = listOf(
                Triple(Screen.Investments.route, "📈", "Investment Projects"),
                Triple(Screen.Expenses.route,    "🧾", "Expenses"),
                Triple(Screen.Loans.route,       "🤝", "My Loans"),
                Triple(Screen.Ledger.route,      "📋", "My Capital Ledger"),
            )

            items.forEach { (route, icon, label) ->
                val isActive = currentRoute == route
                Surface(
                    onClick  = { onNavigate(route) },
                    color    = if (isActive) Color(0xFFEFF6FF) else Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(20.dp, 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(icon, fontSize = 20.sp)
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = if (isActive) Color(0xFF2563EB) else Color(0xFF0F172A))
                        
                        if (isActive) {
                            Spacer(Modifier.weight(1f))
                            Surface(color = Color(0xFF2563EB), shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.size(6.dp)) {}
                        }
                    }
                }
            }
        }
    }
}