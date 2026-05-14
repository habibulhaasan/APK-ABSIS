// ui/shell/MainShell.kt
package com.absis.capitalsync.ui.shell

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.absis.capitalsync.core.navigation.Screen
import com.absis.capitalsync.core.updater.AppUpdateChecker

// Routes that show the bottom bar
private val NAV_ROUTES = setOf(
    Screen.Dashboard.route, Screen.Installment.route,
    Screen.Profile.route,   Screen.Investments.route,
    Screen.Expenses.route,  Screen.Loans.route,
    Screen.Ledger.route,
)

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
                CustomFloatingBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == "more") {
                            showMoreSheet = true
                        } else if (route != currentRoute) {
                            navController.navigate(route) {
                                // Correct backstack logic preventing tab lockouts
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
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
        AppUpdateChecker(
            currentVersion = "2.0.0", // Matches your build.gradle.kts versionName
            jsonUrl        = "https://absis-backup.vercel.app/app-release.json",
            apkDownloadUrl = "https://absis-backup.vercel.app/absis-capital-sync.apk"
        )
    }

    // ── "More" bottom sheet ───────────
    if (showMoreSheet) {
        MoreBottomSheet(
            currentRoute = currentRoute,
            onNavigate   = { route ->
                showMoreSheet = false
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                }
            },
            onDismiss = { showMoreSheet = false }
        )
    }
}

// ── Custom Floating Bottom Navigation ─────────────────────────────────────────
@Composable
private fun CustomFloatingBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    // Explicit sizing container ensures the FAB is not clipped by Scaffold's bounds
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Navigation Background
        Surface(
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavItem(Icons.Filled.Home, "Home", currentRoute == Screen.Dashboard.route) { onNavigate(Screen.Dashboard.route) }
                    NavItem(Icons.Filled.List, "Ledger", currentRoute == Screen.Ledger.route) { onNavigate(Screen.Ledger.route) }
                }

                // Space for floating center button
                Spacer(Modifier.width(60.dp)) 

                // Right Side
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavItem(Icons.Filled.Person, "Profile", currentRoute == Screen.Profile.route) { onNavigate(Screen.Profile.route) }
                    NavItem(
                        icon = Icons.Filled.Menu, 
                        label = "More", 
                        isActive = currentRoute in setOf(Screen.Investments.route, Screen.Expenses.route, Screen.Loans.route),
                        onClick = { onNavigate("more") }
                    )
                }
            }
        }

        // Center Floating "Pay" Button, anchored absolutely to break out of the white bounds securely
        Box(
            modifier = Modifier
                .padding(bottom = 32.dp) // Pushes it completely above the surface properly
                .navigationBarsPadding() // Adapts padding for phones with gesture navigation
                .size(58.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(if (currentRoute == Screen.Installment.route) Color(0xFF0F172A) else Color(0xFF2563EB))
                .clickable { onNavigate(Screen.Installment.route) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Pay", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    val color = if (isActive) Color(0xFF2563EB) else Color(0xFF94A3B8)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
    }
}

// ── More bottom sheet ─────────────────────────────
@Composable
private fun MoreBottomSheet(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .clickable(onClick = onDismiss)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(bottom = 32.dp)
                .clickable(enabled = false) { }
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.size(36.dp, 4.dp), RoundedCornerShape(2.dp), color = Color(0xFFE2E8F0)) {}
            }

            Text("More", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).padding(bottom = 8.dp))
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
                        Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(icon, fontSize = 20.sp)
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isActive) Color(0xFF2563EB) else Color(0xFF0F172A))
                        if (isActive) {
                            Spacer(Modifier.weight(1f))
                            Surface(color = Color(0xFF2563EB), shape = RoundedCornerShape(50.dp), modifier = Modifier.size(6.dp)) {}
                        }
                    }
                }
            }
        }
    }
}