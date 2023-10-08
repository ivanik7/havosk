package ru.ivanik.ha_vosk.lib

import android.app.ActivityManager
import android.content.Context

class ServiceUtils(private val context: Context) {
    fun isServiceRunning( serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == serviceClass.name }
    }
}