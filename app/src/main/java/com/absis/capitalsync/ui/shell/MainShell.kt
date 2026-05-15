// ui/shell/MainShell.kt
@file:Suppress("unused", "UNUSED_VARIABLE")

package com.absis.capitalsync.ui.shell

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
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
import com.google.firebase.auth.FirebaseAuth

private val NAV_ROUTES = setOf(
    Screen.Dashboard.route, Screen.Installment.route,
    Screen.Profile.route,   Screen.Investments.route,
    Screen.Expenses.route,  Screen.Loans.route,
    Screen.Ledger.route,    Screen.AppInfo.route
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
            currentVersion  = "2.0.1", 
            jsonUrl         = "https://absis-backup.vercel.app/app-release.json",
            apkDownloadUrl  = "https://absis-backup.vercel.app/absis-capital-sync.apk",
            fallbackJsonUrl = "https://absis.netlify.app/app-release.json",
            fallbackApkUrl  = "https://absis.netlify.app/absis-capital-sync.apk"
        )
    }

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
            onDismiss = { showMoreSheet = false },
            onLogout = {
                showMoreSheet = false
                FirebaseAuth.getInstance().signOut()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}

@Composable
private fun CustomFloatingBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp) 
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavItem(Icons.Filled.Home, "Home", currentRoute == Screen.Dashboard.route) { onNavigate(Screen.Dashboard.route) }
                    
                    // Fixed: Using AutoMirrored for the List icon
                    NavItem(Icons.AutoMirrored.Filled.List, "Ledger", currentRoute == Screen.Ledger.route) { onNavigate(Screen.Ledger.route) }
                }

                Spacer(Modifier.width(60.dp)) 

                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NavItem(Icons.Filled.Person, "Profile", currentRoute == Screen.Profile.route) { onNavigate(Screen.Profile.route) }
                    NavItem(
                        icon = Icons.Filled.Menu, 
                        label = "More", 
                        isActive = currentRoute in setOf(Screen.Investments.route, Screen.Expenses.route, Screen.Loans.route, Screen.AppInfo.route),
                        onClick = { onNavigate("more") }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
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

@Composable
private fun MoreBottomSheet(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Are you sure you want to log out of your account?", color = Color(0xFF475569)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onDismiss))

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
                Triple(Screen.Investments.route, Icons.Outlined.Analytics, "Investment Projects"),
                Triple(Screen.Expenses.route,    Icons.Outlined.ReceiptLong, "Expenses"),
                Triple(Screen.Loans.route,       Icons.Outlined.Handshake, "My Loans"),
                Triple(Screen.Ledger.route,      Icons.Outlined.Book, "My Capital Ledger"),
                Triple(Screen.AppInfo.route,     Icons.Outlined.Info, "App Information"), 
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
                        Icon(icon, contentDescription = label, tint = if (isActive) Color(0xFF2563EB) else Color(0xFF64748B), modifier = Modifier.size(24.dp))
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (isActive) Color(0xFF2563EB) else Color(0xFF0F172A))
                        if (isActive) {
                            Spacer(Modifier.weight(1f))
                            Surface(color = Color(0xFF2563EB), shape = RoundedCornerShape(50.dp), modifier = Modifier.size(6.dp)) {}
                        }
                    }
                }
            }

            HorizontalDivider()
            Surface(
                onClick  = { showLogoutDialog = true },
                color    = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Fixed: Using AutoMirrored for the Exit door icon
                    Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Log Out", tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                    Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                }
            }
        }
    }
}