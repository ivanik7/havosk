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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import ru.ivanik.ha_vosk.lib.UnZip
import ru.ivanik.ha_vosk.repository.DataStoreRepository
import ru.ivanik.ha_vosk.repository.ModelRepository


class MainActivity : ComponentActivity() {

    val dataStoreRepository = DataStoreRepository(this)
    val modelRepository = ModelRepository(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HavoskTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        var modelsList by remember { mutableStateOf<List<String>?>(null) }
                        LaunchedEffect(null) {
                            modelsList = modelRepository.list()
                        }

                        var serviceRunning by remember { mutableStateOf<Boolean>(false) }
                        // TODO: subscribe to service state

                        SettingsString("Home Assistant URL", Preferences.HA_URL, !serviceRunning)
                        SettingsString("Home Assistant token", Preferences.HA_TOKEN, !serviceRunning)
                        SettingsString("Wake word", Preferences.WAKE_WORD, !serviceRunning)
                        SettingsSelect("Model", modelsList, Preferences.MODEL, !serviceRunning)

                        // TODO: link to Vosk models list page
                        Button(
                            onClick = { selectModel() },
                            enabled = !serviceRunning,
                        ) {
                            Text("Import model")
                        }

                        Row {
                            Button(
                                onClick = {
                                    startVoiceService()
                                    serviceRunning = true
                                },
                                enabled = !serviceRunning,
                            ) {
                                Text("Start")
                            }
                            Button(
                                onClick = {
                                    stopVoiceService()
                                    serviceRunning = false
                                },
                                enabled = serviceRunning,
                            ) {
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }
    }

    val importModel = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        Log.i("import", uri.toString())

        // TODO: move to service
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

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun SettingsString(title: String, prefName: String, enabled: Boolean = true) {
        var settingsValue by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(prefName) {
            settingsValue = dataStoreRepository.get(prefName) ?: ""
        }

        TextField(
            value = settingsValue ?: "",
            onValueChange = {value ->
                settingsValue = value

                lifecycleScope.launch {
                    dataStoreRepository.save(prefName, value)
                }
            },
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            label = {
                Text(title)
            },
            enabled = enabled && settingsValue !== null
        )
    }

    @Composable
    fun SettingsSelect(title: String, items: List<String>?, prefName: String, enabled: Boolean = true) {
        var settingsValue by remember { mutableStateOf<String?>(null) }

        fun onSelect(value: String) {
            settingsValue = value

            lifecycleScope.launch {
                dataStoreRepository.save(prefName, value)
            }
        }

        var expanded by remember { mutableStateOf(false) }

        LaunchedEffect(prefName) {
            settingsValue = dataStoreRepository.get(prefName) ?: ""
        }

        // Move to Select component
        Box(modifier = Modifier
//            .fillMaxSize()
//            .wrapContentSize(Alignment.TopStart)
        ) {
            // TODO: Button like textbox
            Button(
                onClick = {
                    expanded = true
                },
                content = {
                    // TODO: Check if selectd model exits
                    Text(settingsValue ?: "")
                }
            )
            if (items != null) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    },
                    content = {
                        items.map {
                            DropdownMenuItem(
                                onClick = {
                                    onSelect(it)
                                },
                                text = {
                                    Text(it)
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}
