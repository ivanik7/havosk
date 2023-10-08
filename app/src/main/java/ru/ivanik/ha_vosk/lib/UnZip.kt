package ru.ivanik.ha_vosk.lib

import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object UnZip {
    val BUFFER_SIZE = 4096
    fun unzip(fileInputStream: InputStream, location: File) {
        Log.v("Decompress", "Out: $location")

        try {
            val zipInputStream = ZipInputStream(fileInputStream)

            while (true) {
                val zipEntry = zipInputStream.nextEntry ?: break

                Log.v("Decompress", "Unzipping " + zipEntry.name)

                val file = File(location, zipEntry.name)

                if (zipEntry.isDirectory) {
                    if (!file.isDirectory) {
                        file.mkdirs()
                    }
                } else {
                    val fileOutputStream = BufferedOutputStream(FileOutputStream(file))

                    val bytesIn = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (zipInputStream.read(bytesIn).also { read = it } != -1) {
                        fileOutputStream.write(bytesIn, 0, read)
                    }

                    zipInputStream.closeEntry()
                    fileOutputStream.close()
                }
            }
            Log.v("Decompress", "done")
        } catch (e: Exception) {
            Log.e("Decompress", "unzip", e)
        }
    }
}