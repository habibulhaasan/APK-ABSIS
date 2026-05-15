// core/navigation/AppNavGraph.kt
package com.absis.capitalsync.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.absis.capitalsync.ui.appinfo.AppInfoScreen
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
    object AppInfo        : Screen("app_info")
}

@Composable
fun AppNavGraph(
    navController:    NavHostController,
    paddingValues:    PaddingValues,
    startDestination: String = Screen.Login.route,
) {
    NavHost(
        navController      = navController,
        startDestination   = startDestination,
        modifier           = Modifier.padding(paddingValues),
        // ── Smooth Transitions ──
        enterTransition    = { fadeIn(tween(250)) + scaleIn(initialScale = 0.98f, animationSpec = tween(250)) },
        exitTransition     = { fadeOut(tween(250)) + scaleOut(targetScale = 1.02f, animationSpec = tween(250)) },
        popEnterTransition = { fadeIn(tween(250)) + scaleIn(initialScale = 1.02f, animationSpec = tween(250)) },
        popExitTransition  = { fadeOut(tween(250)) + scaleOut(targetScale = 0.98f, animationSpec = tween(250)) }
    ) {

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToDashboard      = { navController.navigate(Screen.Dashboard.route) { popUpTo(0) } },
                onNavigateToAdmin          = { navController.navigate(Screen.Admin.route) { popUpTo(0) } },
                onNavigateToSuperAdmin     = { }, // Superadmin panel
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onNavigateToRegister       = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route     = Screen.ResetPassword.route,
            arguments = listOf(navArgument("oobCode") { type = NavType.StringType; defaultValue = "" })
        ) { backStack ->
            val oobCode = backStack.arguments?.getString("oobCode") ?: ""
            ResetPasswordScreen(oobCode = oobCode, onDone = { navController.navigate(Screen.Login.route) { popUpTo(0) } })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToInstallment = { navController.navigate(Screen.Installment.route) },
                onNavigateToLedger      = { navController.navigate(Screen.Ledger.route) },
                onNavigateToLoans       = { navController.navigate(Screen.Loans.route) },
                onNavigateToAdmin       = { navController.navigate(Screen.Admin.route) },
                onNavigateToExpenses    = { navController.navigate(Screen.Expenses.route) }
            )
        }

        composable(Screen.Installment.route) {
            InstallmentScreen(onNavigateToLedger = { navController.navigate(Screen.Ledger.route) })
        }

        composable(Screen.Investments.route) { InvestmentsScreen() }
        composable(Screen.Expenses.route) { ExpensesScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
        composable(Screen.Loans.route) { LoansScreen() }
        composable(Screen.Ledger.route) { LedgerScreen() }
        
        composable(Screen.AppInfo.route) { 
            AppInfoScreen(onBack = { navController.popBackStack() }) 
        }

        composable(Screen.Admin.route) {
            AdminPlaceholder(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun AdminPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🛠 Admin Panel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Admin screens are built separately.", color = SubtitleGray, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onBack) { Text("← Go Back") }
        }
    }
}