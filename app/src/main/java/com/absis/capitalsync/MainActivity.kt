package com.absis.capitalsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.compose.rememberNavController
import com.absis.capitalsync.core.navigation.AppNavGraph
import com.absis.capitalsync.core.navigation.Screen
import com.absis.capitalsync.ui.shell.MainShell
import com.absis.capitalsync.ui.theme.CapitalSyncTheme
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // Handle Firebase password-reset deep link
        val startRoute = intent?.data?.let { uri ->
            val mode    = uri.getQueryParameter("mode")
            val oobCode = uri.getQueryParameter("oobCode") ?: ""
            if (mode == "resetPassword" && oobCode.isNotEmpty())
                "reset-password/$oobCode"
            else null
        } ?: Screen.Login.route

        setContent {
            CapitalSyncTheme {
                val navController = rememberNavController()
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