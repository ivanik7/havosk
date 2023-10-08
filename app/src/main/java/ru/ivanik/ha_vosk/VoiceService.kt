package ru.ivanik.ha_vosk

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.ivanik.ha_vosk.lib.homeAssistantClient.HomeAssistantClient
import ru.ivanik.ha_vosk.lib.homeAssistantClient.conversationProcess.Request
import ru.ivanik.ha_vosk.lib.notification.VoiceServiceNotificationChannel
import ru.ivanik.ha_vosk.lib.recognizer.VoskRecognizer
import ru.ivanik.ha_vosk.repository.DataStoreRepository
import ru.ivanik.ha_vosk.repository.ModelRepository
import java.lang.Exception

class VoiceService() : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var haClient: HomeAssistantClient? = null;

    private val notificationId = 1

    private var notification: Notification.Builder? = null

    private val recognizer = VoskRecognizer(
        onPartialText = {onPartialText(it)},
        onText = {onFullText(it)},
        onRecognizerError = {onRecognitionError(it)}
    )

    val dataStoreRepository = DataStoreRepository(this)
    val modelRepository = ModelRepository(this)

    val voiceServiceNotificationChannel = VoiceServiceNotificationChannel()

    var wakeWord: String = ""

    var notificationManager: NotificationManager? = null

    enum class Actions {
        START, STOP
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("VoiceService", "onStartCommand ${intent?.action}")

        when(intent?.action) {
            Actions.START.toString() -> performStart()
            Actions.STOP.toString() -> {
                performStop()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY;
    }

    override fun onBind(intent: Intent): IBinder {
        TODO()
    }

    fun performStart() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val createdNotification = createNotification()

        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(notificationId, createdNotification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(notificationId, createdNotification.build())
        }

        scope.launch {
            wakeWord = dataStoreRepository.get(Preferences.WAKE_WORD) ?: ""
        }

        scope.launch {
            val haUrl = dataStoreRepository.get(Preferences.HA_URL)
            val haToken = dataStoreRepository.get(Preferences.HA_TOKEN)

            if (haUrl == null || haToken == null) {
                Toast.makeText(applicationContext, getString(R.string.ha_url_is_null), Toast.LENGTH_LONG).show()

                stopSelf()
                return@launch
            }

            haClient = HomeAssistantClient(haUrl, haToken)
        }

        scope.launch {
            val modelName = dataStoreRepository.get(Preferences.MODEL)

            if (modelName == null) {
                Toast.makeText(applicationContext, getString(R.string.model_is_null), Toast.LENGTH_LONG).show()

                stopSelf()
                return@launch
            }

            try {
                recognizer.start(modelRepository.getFile(modelName))
            } catch (e: Exception) {
                Toast.makeText(applicationContext, getString(R.string.recognition_start_fail) + "\n" + e.message, Toast.LENGTH_LONG).show()

                stopSelf()
                return@launch
            }
        }
    }

    fun performStop() {
        recognizer.stop()
//        TODO: stop HA client
    }

    fun createNotification(): Notification.Builder {
        voiceServiceNotificationChannel.create(this)

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, VoiceService::class.java).apply {
            action = Actions.STOP.toString()
        }

        val stopPendingIntent =
            PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = Notification.Builder(this, voiceServiceNotificationChannel.channeld)
            .setContentTitle(getText(R.string.speech_recognition))
            .setContentText(getText(R.string.kek))
            .setTicker(getText(R.string.kek))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.mipmap.ic_launcher, getString(R.string.stop), stopPendingIntent)

        notification = notificationBuilder

        return notificationBuilder
    }

    fun onPartialText(text: String) {
        updateNotification(text)
    }

    fun onFullText(text: String) {
        updateNotification(text)

        if (text.contains(wakeWord)) {
            val textWithoutWakeWord = text
                .replaceBefore(wakeWord, "")
                .replace(wakeWord, "")
                .trim()

            Toast.makeText(applicationContext, textWithoutWakeWord, Toast.LENGTH_SHORT).show()
            sendToHomeAssistant(textWithoutWakeWord)
        }
    }

    fun onRecognitionError(e: Exception?) {
        Toast.makeText(applicationContext, getString(R.string.recognition_error) + "\n" + e?.message, Toast.LENGTH_LONG).show()
    }

    fun updateNotification(text: String) {
        notification?.let {
            it.setContentText(text)

            notificationManager?.notify(notificationId, it.build())
        }
    }

    fun sendToHomeAssistant(text: String) {
        scope.launch {
            haClient?.let {
                it.conversationProcess(Request(text, "ru"))
            }
        }
    }
}