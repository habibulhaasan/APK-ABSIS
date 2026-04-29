// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/shell/MainShell.kt
// Replaces Next.js layout.js + Shell + Sidebar
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.absis.capitalsync.core.navigation.Screen

// ── Bottom nav items ──────────────────────────────────────────────────────────
data class NavItem(
    val route:     String,
    val label:     String,
    val icon:      String,   // emoji icon (no icon font dependency)
    val iconFilled:String,
)

val MEMBER_NAV_ITEMS = listOf(
    NavItem(Screen.Dashboard.route,   "Home",        "🏠", "🏠"),
    NavItem(Screen.Installment.route, "Pay",         "💳", "💳"),
    NavItem(Screen.Investments.route, "Investments", "📈", "📈"),
    NavItem(Screen.Expenses.route,    "Expenses",    "🧾", "🧾"),
    NavItem(Screen.Profile.route,     "Profile",     "👤", "👤"),
)

// ── Main app shell with bottom navigation ─────────────────────────────────────
@Composable
fun MainShell(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backstackEntry?.destination?.route

    // Screens that show the bottom nav bar
    val navRoutes = MEMBER_NAV_ITEMS.map { it.route }.toSet()
    val showNav   = currentRoute in navRoutes

    Scaffold(
        bottomBar = {
            if (showNav) {
                MainBottomBar(
                    items        = MEMBER_NAV_ITEMS,
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                // Pop up to dashboard to avoid huge backstack
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        content(padding)
    }
}

// ── Bottom bar composable ─────────────────────────────────────────────────────
@Composable
fun MainBottomBar(
    items: List<NavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(item.route) },
                icon = {
                    Text(
                        text     = if (selected) item.iconFilled else item.icon,
                        fontSize = 20.sp
                    )
                },
                label = {
                    Text(
                        text       = item.label,
                        fontSize   = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Color(0xFF2563EB),
                    selectedTextColor   = Color(0xFF2563EB),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor      = Color(0xFFEFF6FF)
                )
            )
        }
    }
}
