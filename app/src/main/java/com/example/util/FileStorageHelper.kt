package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileStorageHelper {
    fun copyFileToInternalStorage(context: Context, uri: Uri, originalName: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val filesDir = File(context.filesDir, "course_materials")
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            
            val safeName = "${UUID.randomUUID()}_${originalName.takeLast(30)}"
            val file = File(filesDir, safeName)
            
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.close()
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
