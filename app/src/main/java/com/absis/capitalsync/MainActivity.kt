package com.absis.capitalsync

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.absis.capitalsync.core.navigation.AppNavGraph
import com.absis.capitalsync.core.navigation.Screen
import com.absis.capitalsync.core.notifications.CapitalSyncMessagingService
import com.absis.capitalsync.core.notifications.InstallmentReminderWorker // ADDED IMPORT
import com.absis.capitalsync.ui.shell.MainShell
import com.absis.capitalsync.ui.theme.CapitalSyncTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // ADDED: Schedule the background worker for daily installment checks
        InstallmentReminderWorker.schedule(this)

        setContent {
            CapitalSyncTheme {
                val navController = rememberNavController()

                // Determine start screen
                val startRoute: String = when {
                    // Firebase password-reset deep link
                    intent?.data?.getQueryParameter("mode") == "resetPassword" -> {
                        val oobCode = intent.data?.getQueryParameter("oobCode") ?: ""
                        "reset-password/$oobCode"
                    }
                    // Already logged in → skip login screen
                    FirebaseAuth.getInstance().currentUser != null ->
                        Screen.Dashboard.route
                    // Not logged in
                    else -> Screen.Login.route
                }

                MainShell(navController = navController) { padding ->
                    AppNavGraph(
                        navController    = navController,
                        paddingValues    = padding,
                        startDestination = startRoute,
                    )
                }
            }
        }
    }
}