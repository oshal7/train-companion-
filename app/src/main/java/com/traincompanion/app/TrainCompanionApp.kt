package com.traincompanion.app

import android.app.Application
import com.traincompanion.app.alarm.NotificationHelper
import com.traincompanion.app.data.AppContainer

class TrainCompanionApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
        NotificationHelper.ensureChannel(this)
    }
}
