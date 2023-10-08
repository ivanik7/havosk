package ru.ivanik.ha_vosk.lib.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

abstract class NotificationChannel(
    val channeld: String,
    val name: Int,
) {
    fun create(context: Context) {
        val channel = NotificationChannel(channeld,
            context.getString(name), NotificationManager.IMPORTANCE_NONE)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }
}