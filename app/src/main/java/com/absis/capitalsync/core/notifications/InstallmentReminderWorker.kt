package com.absis.capitalsync.core.notifications

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class InstallmentReminderWorker(
    ctx:    Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    companion object {
        const val WORK_NAME = "installment_reminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<InstallmentReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db  = FirebaseFirestore.getInstance()

        return try {
            val userSnap = db.collection("users").document(uid).get().await()
            val orgId    = userSnap.getString("activeOrgId") ?: return Result.success()
            val orgSnap  = db.collection("organizations").document(orgId).get().await()

            @Suppress("UNCHECKED_CAST")
            val settings = orgSnap.get("settings") as? Map<String, Any> ?: emptyMap()

            val monthlyEnabled = settings["monthlyEnabled"] as? Boolean ?: true
            if (!monthlyEnabled) return Result.success()

            val cal      = Calendar.getInstance()
            val curMonth = "${cal.get(Calendar.YEAR)}-${
                String.format("%02d", cal.get(Calendar.MONTH) + 1)
            }"

            val paySnap = db.collection("organizations/$orgId/investments")
                .whereEqualTo("userId", uid)
                .get().await()

            val paidThisMonth = paySnap.documents.any { doc ->
                val status = doc.getString("status") ?: return@any false
                if (status == "rejected") return@any false
                @Suppress("UNCHECKED_CAST")
                (doc.get("paidMonths") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.contains(curMonth) == true
            }

            if (!paidThisMonth) {
                showNotification(
                    context   = applicationContext,
                    title     = "📅 Installment Due",
                    body      = "Your installment for $curMonth has not been paid yet. Tap to pay now.",
                    channelId = CapitalSyncMessagingService.CHANNEL_INSTALLMENT,
                    sticky    = true,
                    notifId   = "installment".hashCode(),
                )
            } else {
                val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                nm.cancel("installment".hashCode())
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}