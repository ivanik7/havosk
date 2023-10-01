package ru.ivanik.ha_vosk

import android.app.Notification
import android.app.NotificationChannel
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
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import ru.ivanik.ha_vosk.homeAssistantClient.HomeAssistantClient
import ru.ivanik.ha_vosk.homeAssistantClient.conversationProcess.Request
import java.io.IOException

class VoiceService() : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private var haClient: HomeAssistantClient? = null;

    private var model: Model? = null

    private var speechService: SpeechService? = null

    private val notificationId = 1

    private var notification: Notification.Builder? = null

    val dataStoreRepository = DataStoreRepository(this)

    enum class Actions {
        START, STOP
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("VoiceService", "onStartCommand ${intent?.action}")

        when(intent?.action) {
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> {
                speechService?.stop();
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY;
    }

    override fun onBind(intent: Intent): IBinder {
        TODO()
    }

    fun start() {
        startAsForeground()
        initModel()
        startListen()

        scope.launch {
            val haUrl = dataStoreRepository.get(Preferences.HA_URL) ?: ""
            val haToken = dataStoreRepository.get(Preferences.HA_TOKEN) ?: ""

            haClient = HomeAssistantClient(haUrl, haToken)

            Log.i("haUrl", haUrl)
        }
    }

    fun startAsForeground() {
        // TODO: Move to another class
        val channel = NotificationChannel("keklol",
            "HA-Vosk background service", NotificationManager.IMPORTANCE_NONE)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, VoiceService::class.java).apply {
            action = Actions.STOP.toString()
        }

        val stopPendingIntent =
            PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        notification = Notification.Builder(this, "keklol")
            .setContentTitle(getText(R.string.speech_recognition))
            .setContentText(getText(R.string.kek))
            .setTicker(getText(R.string.kek))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.mipmap.ic_launcher, getString(R.string.stop),
                stopPendingIntent)

        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(notificationId, notification!!.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(notificationId, notification!!.build())
        }
    }

    fun initModel() {
        LibVosk.setLogLevel(LogLevel.INFO);

        model = Model(this.getFilesDir().toString() + "/model/vosk-model-small-ru-0.22/")
    }

    fun startListen() {
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    val text = JSONObject(hypothesis).getString("partial")

                    if (text.length > 0) {
                        updateNotification(text)
                    }
                }

                override fun onResult(hypothesis: String?) {
                    val text = JSONObject(hypothesis).getString("text")

                    if (text.length > 0) {
                        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                        updateNotification(text)
                        sendToHomeAssistant(text)

                        Log.i("result", text)
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                }

                override fun onError(exception: java.lang.Exception?) {
                    Toast.makeText(applicationContext, exception?.message, Toast.LENGTH_LONG).show()
                    stopSelf()
                }

                override fun onTimeout() {
                    Toast.makeText(applicationContext, "timeout", Toast.LENGTH_LONG).show()
                    stopSelf()
                }

            })
        } catch (e: IOException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    fun updateNotification(text: String) {
        notification?.setContentText(text)

        // TODO: lazy
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(notificationId, notification!!.build())
    }

    fun sendToHomeAssistant(text: String) {
        haClient?.let {
            scope.launch {
                it.conversationProcess(Request(text, "ru"))
            }
        }
    }
}