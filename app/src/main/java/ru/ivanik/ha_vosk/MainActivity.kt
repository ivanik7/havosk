package ru.ivanik.ha_vosk

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.ivanik.ha_vosk.lib.ServiceUtils
import ru.ivanik.ha_vosk.lib.UnZip
import ru.ivanik.ha_vosk.repository.DataStoreRepository
import ru.ivanik.ha_vosk.repository.ModelRepository


class MainActivity : ComponentActivity() {

    private val dataStoreRepository = DataStoreRepository(this)
    private val modelRepository = ModelRepository(this)
    private val serviceUtils = ServiceUtils(this)

    var serviceRunning by mutableStateOf<Boolean>(true)

    private val voiceServiceStopedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            serviceRunning = false;
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HavoskTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        topBar = {
                            TopAppBar(

                                title = {
                                    Text(getString(R.string.ha_vosk_title))
                                }
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                if (serviceRunning) {
                                    stopVoiceService()
                                } else {
                                    startVoiceService()
                                }

                                serviceRunning = !serviceRunning
                            }) {
                                if (serviceRunning) Icon(Icons.Default.Close, contentDescription = "Stop")
                                     else Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            }
                        }
                    ) {innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding),
                        ) {
                            var modelsList by remember { mutableStateOf<List<String>?>(null) }
                            LaunchedEffect(null) {
                                modelsList = modelRepository.list()
                            }

                            SettingsString("Home Assistant URL", Preferences.HA_URL, !serviceRunning)
                            SettingsString("Home Assistant token", Preferences.HA_TOKEN, !serviceRunning, isPassword = true)
                            SettingsString("Wake word", Preferences.WAKE_WORD, !serviceRunning)
                            SettingsSelect("Model", modelsList, Preferences.MODEL, !serviceRunning)

                            // TODO: link to Vosk models list page
                            Button(
                                onClick = { selectModel() },
                                enabled = !serviceRunning,
                            ) {
                                Text("Import model")
                            }
                        }
                    }
                }
            }
        }

        serviceRunning = serviceUtils.isServiceRunning(VoiceService::class.java)
        registerReceiver(voiceServiceStopedReceiver, IntentFilter(IntentName.VOICE_SERVICE_STOPED))
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(voiceServiceStopedReceiver)
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

    // TODO: от Газима, рекомендую вынести во ViewModel
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun SettingsString(title: String, prefName: String, enabled: Boolean = true, isPassword: Boolean = false) {
        var settingsValue by remember { mutableStateOf(TextFieldValue()) }
        var isLoaded by remember { mutableStateOf(false) }
        val isEnabled by remember { derivedStateOf { enabled && isLoaded } }

        LaunchedEffect(prefName) {
            withContext(Dispatchers.IO) {
                dataStoreRepository.get(prefName)?.let {
                    settingsValue = TextFieldValue(it)
                }
                isLoaded = true
            }
        }

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            ,
            value = settingsValue,
            onValueChange = {value ->
                settingsValue = value

                lifecycleScope.launch(Dispatchers.IO) {
                    dataStoreRepository.save(prefName, value.text)
                }
            },
            maxLines = 1,
            label = {
                Text(title)
            },
            enabled = isEnabled,
            visualTransformation =
                if (isPassword) PasswordVisualTransformation()
                else VisualTransformation.None
        )
    }

    @Composable
    fun SettingsSelect(title: String, items: List<String>, prefName: String, enabled: Boolean = true) {
        var settingsValue by remember { mutableStateOf("") }
        var isExist by remember { mutableStateOf(false) }

        fun onSelect(value: String) {
            settingsValue = value

            lifecycleScope.launch {
                dataStoreRepository.save(prefName, value)
            }
        }

        var expanded by remember { mutableStateOf(false) }

        LaunchedEffect(prefName) {
            dataStoreRepository.get(prefName)?.let {
                settingsValue = it
                isExist = true
            }
        }

        // Move to Select component
        Box(modifier = Modifier
        ) {
            // TODO: Button like textbox
            Button(
                onClick = {
                    expanded = true
                },
                enabled = enabled,
                content = {
                    // TODO: Check if selectd model exits, use isExist value
                    Text(settingsValue)
                }
            )
            if (items.isNotEmpty()) {
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
