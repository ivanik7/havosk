package ru.ivanik.ha_vosk.lib.recognizer

import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.lang.Exception

class VoskRecognizer(
    val onPartialText: (String) -> Unit,
    val onText: (String) -> Unit,
    val onRecognizerError: (Exception?) -> Unit,
) {
    private var speechService: SpeechService? = null

    fun start(modelPath: File) {
        LibVosk.setLogLevel(LogLevel.INFO);

        val model = Model(modelPath.toString())

        startListen(model)
    }

    fun stop() {
        speechService?.stop()
    }

    private fun startListen(model: Model) {
        val rec = Recognizer(model, 16000.0f)
        speechService = SpeechService(rec, 16000.0f)

        speechService?.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                if (hypothesis !== null) {
                    val text = JSONObject(hypothesis).getString("partial")

                    if (text.length > 0) {
                        onPartialText(text)
                    }
                }
            }

            override fun onResult(hypothesis: String?) {
                if (hypothesis !== null) {
                    val text = JSONObject(hypothesis).getString("text")

                    if (text.length > 0) {
                        onText(text)
                    }
                }
            }

            override fun onFinalResult(hypothesis: String?) {
                if (hypothesis !== null) {
                    val text = JSONObject(hypothesis).getString("text")

                    if (text.length > 0) {
                        onText(text)
                    }
                }
            }

            override fun onError(exception: Exception?) {
                onRecognizerError(exception)
                stop()
            }

            override fun onTimeout() {
                onRecognizerError(Exception("Vosk timeout"))
            }
        })
    }
}
