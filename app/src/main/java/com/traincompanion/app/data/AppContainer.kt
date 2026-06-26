package com.traincompanion.app.data

import android.content.Context
import com.traincompanion.app.alarm.AlarmScheduler
import com.traincompanion.app.rail.RailDataProvider
import com.traincompanion.app.rail.RailDataProviderFactory

/** Lightweight manual-DI container - avoids pulling in Hilt/Dagger annotation processing for v1. */
class AppContainer private constructor(context: Context) {
    val securePrefs = SecurePrefs(context)
    val secureFileStore = SecureFileStore(context)
    val documentsRepository = DocumentsRepository(secureFileStore)
    val contactsRepository = ContactsRepository(secureFileStore)
    val alarmsRepository = AlarmsRepository(secureFileStore)
    val pnrRepository = PnrRepository(secureFileStore)
    val alarmScheduler = AlarmScheduler(context)

    fun currentRailProvider(): RailDataProvider = RailDataProviderFactory.create(securePrefs.getSettings())

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
