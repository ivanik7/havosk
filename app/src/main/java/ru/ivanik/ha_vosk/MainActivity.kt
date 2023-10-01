package ru.ivanik.ha_vosk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ivanik.ha_vosk.ui.theme.HavoskTheme
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation


class MainActivity : ComponentActivity() {

    val dataStoreRepository = DataStoreRepository(this)
    var haUrl: String? by mutableStateOf(null)
    var haToken: String? by mutableStateOf(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            HavoskTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        Text("Home Assistant URL")

                        haUrl?.let {
                            TextField(
                                value = it,
                                onValueChange = { value: String ->
                                    haUrl = value

                                    lifecycleScope.launch {
                                        dataStoreRepository.save(Preferences.HA_URL, value)
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                label = {
                                    Text("URL")
                                }
                            )
                        }


                        haToken?.let {
                            TextField(
                                value = it,
                                onValueChange = { value: String ->
                                    haToken = value

                                    lifecycleScope.launch {
                                        dataStoreRepository.save(Preferences.HA_TOKEN, value)
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                label = {
                                    Text("Token")
                                },
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }


                        Button(onClick = { selectModel() }) {
                            Text("Import model")
                        }
                        Button(onClick = { startVoiceService() }) {
                            Text("Start")
                        }
                        Button(onClick = { stopVoiceService() }) {
                            Text("Stop")
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            haUrl = dataStoreRepository.get(Preferences.HA_URL) ?: ""
            haToken = dataStoreRepository.get(Preferences.HA_TOKEN) ?: ""
        }
    }

    val importModel = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        Log.i("import", uri.toString())

        // TODO: вынести в сервис
        if (uri == null) {
            return@registerForActivityResult
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val model = File(getFilesDir(), "model")

                model.deleteRecursively()

                getContentResolver().openInputStream(uri)?.use {
                    UnZip.unzip(it, model)
                }
            }
        }
    }

    fun selectModel() {
        importModel.launch("application/zip")
    }

    fun startVoiceService() {
//        TODO: запрос разрешения на уведомления
        val permissionCheck = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.RECORD_AUDIO
        )

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.RECORD_AUDIO),
                1
            )

            return
        }

        startForegroundService(Intent(this, VoiceService::class.java).also {
            it.action = VoiceService.Actions.START.toString()
        })
    }

    fun stopVoiceService() {
        startService(Intent(this, VoiceService::class.java).also {
            it.action = VoiceService.Actions.STOP.toString()
        })
    }
}
