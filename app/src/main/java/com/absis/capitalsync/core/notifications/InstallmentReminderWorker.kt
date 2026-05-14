package com.absis.capitalsync.core.notifications

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class InstallmentReminderWorker(
    ctx: Context,
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
            // 1. Load user's active org
            val userSnap = db.collection("users").document(uid).get().await()
            val orgId    = userSnap.getString("activeOrgId") ?: return Result.success()

            // 2. Load org settings
            val orgSnap  = db.collection("organizations").document(orgId).get().await()

            @Suppress("UNCHECKED_CAST")
            val settings = orgSnap.get("settings") as? Map<String, Any> ?: emptyMap()

            val monthlyEnabled = settings["monthlyEnabled"] as? Boolean ?: true
            if (!monthlyEnabled) {
                // Monthly installments disabled — cancel any lingering reminder
                CapitalSyncMessagingService.cancelInstallmentReminder(applicationContext)
                return Result.success()
            }

            // 3. Build current month key e.g. "2026-05"
            // FIXED: Added Locale.US to String.format
            val cal      = Calendar.getInstance()
            val curMonth = "${cal.get(Calendar.YEAR)}-${
                String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
            }"

            // 4. Check due day — only remind on or after the due day
            val dueDay = (settings["dueDate"] as? Number)?.toInt() ?: 10
            if (cal.get(Calendar.DAY_OF_MONTH) < dueDay) {
                // Before due day — cancel any leftover sticky reminder from last month
                CapitalSyncMessagingService.cancelInstallmentReminder(applicationContext)
                return Result.success()
            }

            // 5. Query this member's payments for the current month
            val paySnap = db.collection("organizations/$orgId/investments")
                .whereEqualTo("userId", uid)
                .get().await()

            val paidThisMonth = paySnap.documents.any { doc ->
                val status = doc.getString("status") ?: return@any false
                if (status == "rejected") return@any false

                @Suppress("UNCHECKED_CAST")
                val paidMonths = (doc.get("paidMonths") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()

                paidMonths.contains(curMonth)
            }

            // FIXED: Added CapitalSyncMessagingService prefix to function calls
            if (paidThisMonth) {
                // Paid — cancel the sticky notification if it's showing
                CapitalSyncMessagingService.cancelInstallmentReminder(applicationContext)
            } else {
                // Not paid — show or refresh the sticky notification
                CapitalSyncMessagingService.showNotification(
                    context   = applicationContext,
                    title     = "📅 Installment Due",
                    body      = "Your installment for $curMonth has not been paid yet. Tap to pay now.",
                    channelId = CapitalSyncMessagingService.CHANNEL_INSTALLMENT,
                    sticky    = true,
                    notifId   = CapitalSyncMessagingService.NOTIF_ID_INSTALLMENT,
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}