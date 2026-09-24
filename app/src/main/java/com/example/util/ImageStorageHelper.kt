package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val imagesDir = File(context.filesDir, "notebook_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(imagesDir, fileName)
            
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
