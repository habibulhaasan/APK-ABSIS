package com.absis.capitalsync.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.absis.capitalsync.MainActivity
import com.absis.capitalsync.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CapitalSyncMessagingService : FirebaseMessagingService() {

    // 1. Triggers when a remote push notification arrives while the app is OPEN
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Notification"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        // Uses our reusable showNotification helper
        showNotification(this, title, body, CHANNEL_ID, false, System.currentTimeMillis().toInt())
    }

    // 2. Triggers when Firebase assigns a new push token
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        saveTokenToFirestore(uid, token)
    }

    companion object {
        const val CHANNEL_ID = "absis_capital_alerts"
        const val CHANNEL_INSTALLMENT = "absis_installment_reminders"
        const val NOTIF_ID_INSTALLMENT = 2001

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val alertChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Capital Sync Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Notifications for payments and organization updates" }

                val reminderChannel = NotificationChannel(
                    CHANNEL_INSTALLMENT,
                    "Installment Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Daily reminders for unpaid monthly installments" }

                notificationManager.createNotificationChannel(alertChannel)
                notificationManager.createNotificationChannel(reminderChannel)
            }
        }

        // ── Helper to save FCM Token manually on Login ──
        fun saveFcmToken(uid: String) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (token != null) {
                        saveTokenToFirestore(uid, token)
                    }
                }
            }
        }

        private fun saveTokenToFirestore(uid: String, token: String) {
            FirebaseFirestore.getInstance().collection("users").document(uid).set(
                mapOf("fcmToken" to token),
                SetOptions.merge()
            )
        }

        // ── Helper functions for local Background Workers ──
        fun showNotification(
            context: Context,
            title: String,
            body: String,
            channelId: String = CHANNEL_ID,
            sticky: Boolean = false,
            notifId: Int = System.currentTimeMillis().toInt()
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round) // Ensure you have this icon in res/mipmap
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setOngoing(sticky) 
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notifId, notificationBuilder.build())
        }

        fun cancelInstallmentReminder(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIF_ID_INSTALLMENT)
        }
    }
}