// app/src/main/java/com/absis/capitalsync/core/notifications/CapitalSyncMessagingService.kt
package com.absis.capitalsync.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.absis.capitalsync.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CapitalSyncMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_GENERAL     = "general_notifications"
        const val CHANNEL_INSTALLMENT = "installment_reminders"
        const val CHANNEL_PAYMENT     = "payment_notifications"
        const val CHANNEL_LOAN        = "loan_notifications"

        // Stable notification IDs
        const val NOTIF_ID_INSTALLMENT = 1001
        const val NOTIF_ID_PAYMENT     = 1002
        const val NOTIF_ID_LOAN        = 1003

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_GENERAL,
                        "General Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Admin messages and app updates" }
                )

                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_INSTALLMENT,
                        "Installment Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Monthly installment due reminders"
                        enableVibration(true)
                    }
                )

                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_PAYMENT,
                        "Payment Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Payment verification updates"
                        enableVibration(true)
                    }
                )

                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_LOAN,
                        "Loan Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Loan status updates"
                        enableVibration(true)
                    }
                )
            }
        }

        /**
         * Cancel the sticky installment reminder notification.
         * Called from InstallmentReminderWorker when payment is detected as paid.
         */
        fun cancelInstallmentReminder(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID_INSTALLMENT)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Capital Sync"
        val body  = message.notification?.body  ?: message.data["body"]
                 ?: message.data["message"]     ?: ""
        val type  = message.data["type"] ?: "general"

        when (type) {
            "installment_reminder" -> {
                showNotification(
                    context   = applicationContext,
                    title     = title,
                    body      = body,
                    channelId = CHANNEL_INSTALLMENT,
                    sticky    = true,
                    notifId   = NOTIF_ID_INSTALLMENT,
                )
            }
            "payment_verified" -> {
                showNotification(
                    context   = applicationContext,
                    title     = title,
                    body      = body,
                    channelId = CHANNEL_PAYMENT,
                    sticky    = false,
                    notifId   = NOTIF_ID_PAYMENT,
                )
            }
            "loan_update" -> {
                showNotification(
                    context   = applicationContext,
                    title     = title,
                    body      = body,
                    channelId = CHANNEL_LOAN,
                    sticky    = false,
                    notifId   = NOTIF_ID_LOAN,
                )
            }
            else -> {
                showNotification(
                    context   = applicationContext,
                    title     = title,
                    body      = body,
                    channelId = CHANNEL_GENERAL,
                    sticky    = false,
                    notifId   = System.currentTimeMillis().toInt(),
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmToken", token)
        }
    }
}

fun showNotification(
    context:   Context,
    title:     String,
    body:      String,
    channelId: String  = CapitalSyncMessagingService.CHANNEL_GENERAL,
    sticky:    Boolean = false,
    notifId:   Int     = System.currentTimeMillis().toInt(),
) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pi = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(!sticky)
        .setOngoing(sticky)
        .setPriority(
            if (sticky) NotificationCompat.PRIORITY_HIGH
            else NotificationCompat.PRIORITY_DEFAULT
        )
        .setContentIntent(pi)

    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
}