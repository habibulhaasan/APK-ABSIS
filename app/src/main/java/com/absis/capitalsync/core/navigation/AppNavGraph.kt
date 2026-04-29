// core/navigation/AppNavGraph.kt
package com.absis.capitalsync.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.absis.capitalsync.ui.auth.ForgotPasswordScreen
import com.absis.capitalsync.ui.auth.LoginScreen
import com.absis.capitalsync.ui.auth.RegisterScreen
import com.absis.capitalsync.ui.auth.ResetPasswordScreen
import com.absis.capitalsync.ui.dashboard.DashboardScreen
import com.absis.capitalsync.ui.expenses.ExpensesScreen
import com.absis.capitalsync.ui.installment.InstallmentScreen
import com.absis.capitalsync.ui.investments.InvestmentsScreen
import com.absis.capitalsync.ui.ledger.LedgerScreen
import com.absis.capitalsync.ui.loans.LoansScreen
import com.absis.capitalsync.ui.profile.ProfileScreen
import com.absis.capitalsync.ui.theme.SubtitleGray

// ── Route definitions ─────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Register       : Screen("register")
    object ForgotPassword : Screen("forgot-password")
    object ResetPassword  : Screen("reset-password/{oobCode}") {
        fun createRoute(oobCode: String) = "reset-password/$oobCode"
    }
    object Dashboard      : Screen("dashboard")
    object Installment    : Screen("installment")
    object Investments    : Screen("investments")
    object Expenses       : Screen("expenses")
    object Profile        : Screen("profile")
    object Loans          : Screen("loans")
    object Ledger         : Screen("ledger")
    object Admin          : Screen("admin")
}

@Composable
fun AppNavGraph(
    navController:    NavHostController,
    paddingValues:    PaddingValues,
    startDestination: String = Screen.Login.route,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = Modifier.padding(paddingValues)
    ) {

        // ── Auth (no bottom nav) ──────────────────────────────────────────────

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToDashboard      = {
                    navController.navigate(Screen.Dashboard.route) { popUpTo(0) }
                },
                onNavigateToAdmin          = {
                    navController.navigate(Screen.Admin.route) { popUpTo(0) }
                },
                onNavigateToSuperAdmin     = {
                    // Superadmin panel — build separately
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToRegister       = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Reset password — receives oobCode from deep link or nav arg
        composable(
            route     = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("oobCode") {
                    type         = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            val oobCode = backStack.arguments?.getString("oobCode") ?: ""
            ResetPasswordScreen(
                oobCode = oobCode,
                onDone  = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }

        // ── Member screens (bottom nav visible) ───────────────────────────────

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToInstallment = { navController.navigate(Screen.Installment.route) },
                onNavigateToLedger      = { navController.navigate(Screen.Ledger.route) },
                onNavigateToLoans       = { navController.navigate(Screen.Loans.route) },
                onNavigateToAdmin       = { navController.navigate(Screen.Admin.route) }
            )
        }

        composable(Screen.Installment.route) {
            InstallmentScreen(
                onNavigateToLedger = { navController.navigate(Screen.Ledger.route) }
            )
        }

        composable(Screen.Investments.route) {
            InvestmentsScreen()
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen()
        }

        composable(Screen.Loans.route) {
            LoansScreen()
        }

        composable(Screen.Ledger.route) {
            LedgerScreen()
        }

        // ── Admin (placeholder) ───────────────────────────────────────────────
        composable(Screen.Admin.route) {
            AdminPlaceholder(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ── Temporary admin placeholder ───────────────────────────────────────────────
@Composable
private fun AdminPlaceholder(onBack: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Text(
                "🛠 Admin Panel",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.material3.Text(
                "Admin screens are built separately.",
                color = com.absis.capitalsync.ui.theme.SubtitleGray,
                modifier = Modifier.padding(top = 8.dp)
            )
            androidx.compose.material3.Spacer(Modifier.height(20.dp))
            androidx.compose.material3.OutlinedButton(onClick = onBack) {
                androidx.compose.material3.Text("← Go Back")
            }
        }
    }
}