package com.absis.capitalsync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.absis.capitalsync.core.navigation.AppNavGraph
import com.absis.capitalsync.core.navigation.Screen
import com.absis.capitalsync.core.notifications.CapitalSyncMessagingService
import com.absis.capitalsync.core.notifications.InstallmentReminderWorker
import com.absis.capitalsync.ui.shell.MainShell
import com.absis.capitalsync.ui.theme.CapitalSyncTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. Declare the permission launcher at the CLASS level (Outside of onCreate)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> 
        // Result is handled silently, no empty 'if' body anymore!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // 2. Create Notification Channels
        CapitalSyncMessagingService.createChannels(this)

        // 3. Ask for Notification Permission on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // 4. Schedule the background worker for daily installment checks
        InstallmentReminderWorker.schedule(this)

        // 5. Ensure FCM Token is generated and saved on startup
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null && token != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), SetOptions.merge())
                }
            }
        }

        // 6. Start your Compose UI
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