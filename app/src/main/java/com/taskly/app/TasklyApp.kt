package com.taskly.app

import android.app.Application
import com.taskly.app.di.AppContainer
import com.taskly.app.notifications.NotificationHelper

class TasklyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createNotificationChannels(this)
    }
}
