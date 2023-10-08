package ru.ivanik.ha_vosk.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ModelRepository(val context: Context) {
    val modelDir = "model"

    private fun getModelsFolder(): File {
        return File(context.filesDir, modelDir)
    }

    suspend fun list(): List<String> {
        return withContext(Dispatchers.IO) {
            getModelsFolder().listFiles().map {file -> file.name}
        }
    }

    fun getFile(modelName: String): File {
        return File(getModelsFolder(), modelName)
    }
}