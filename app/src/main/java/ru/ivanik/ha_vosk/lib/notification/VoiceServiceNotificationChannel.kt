package ru.ivanik.ha_vosk.lib.notification

import ru.ivanik.ha_vosk.R


class VoiceServiceNotificationChannel() : NotificationChannel(
    channeld = "voice_service",
    name = R.string.voice_service_notification_channel_name
) {
}