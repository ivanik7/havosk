package ru.ivanik.ha_vosk

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import java.io.IOException
import java.util.logging.Logger

class VoiceService : Service(), RecognitionListener {
    private var model: Model? = null

    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("VoiceService", "onStartCommand")

        startAsForeground()
        initModel()
        startListen()

        return START_STICKY;
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    fun startAsForeground() {
        val chan = NotificationChannel("keklol",
            "kek lol", NotificationManager.IMPORTANCE_NONE)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, MyBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }

        val stopPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, stopIntent, 0)

        val notification: Notification = Notification.Builder(this, "keklol")
            .setContentTitle(getText(R.string.speech_recognition))
            .setContentText(getText(R.string.kek))
            .setTicker(getText(R.string.kek))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction((R.mipmap.ic_launcher, getString(R.string.stop),
                stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    fun initModel() {
        model = Model(this.getExternalFilesDir(null).toString() + "/vosk-model-small-ru-0.22/")
    }

    fun startListen() {
        if (speechService != null) {
            speechService!!.stop()
            speechService = null
        } else {
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService!!.startListening(this)
            } catch (e: IOException) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()

            }
        }
    }

    override fun onResult(hypothesis: String) {
        val j = JSONObject(hypothesis)

        val text = j.getString("text")

        if (text.length > 0) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }

        Log.i("result", hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {


//        Toast.makeText(this, hypothesis, Toast.LENGTH_SHORT).show()



//        Log.i("final",hypothesis)
    }

    override fun onPartialResult(hypothesis: String) {
//        Toast.makeText(this, "$hypothesis... partial", Toast.LENGTH_SHORT).show()

//        Log.i("part", hypothesis)
    }

    override fun onError(e: Exception) {
        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
    }

    override fun onTimeout() {
        Toast.makeText(this, "timeout", Toast.LENGTH_LONG).show()
    }
}