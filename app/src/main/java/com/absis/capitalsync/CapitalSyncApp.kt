package com.absis.capitalsync

import android.app.Application
import com.absis.capitalsync.core.notifications.CapitalSyncMessagingService
import com.absis.capitalsync.core.notifications.InstallmentReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CapitalSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CapitalSyncMessagingService.createChannels(this)
        InstallmentReminderWorker.schedule(this)
    }
}