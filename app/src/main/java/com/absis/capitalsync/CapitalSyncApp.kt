package com.absis.capitalsync

import android.app.Application
import com.absis.capitalsync.core.notifications.CapitalSyncMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CapitalSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CapitalSyncMessagingService.createChannels(this)
    }
}