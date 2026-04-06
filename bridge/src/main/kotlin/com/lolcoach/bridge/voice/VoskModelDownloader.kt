package com.lolcoach.bridge.voice

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Extracting : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class VoskModelDownloader(private val modelDir: String) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    private val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    private val ZIP_FILE_NAME = "vosk-model.zip"

    fun isModelPresent(): Boolean {
        val dir = File(modelDir)
        return dir.exists() && dir.isDirectory && dir.list()?.isNotEmpty() == true
    }

    suspend fun downloadAndExtract() = withContext(Dispatchers.IO) {
        if (isModelPresent()) {
            _downloadState.value = DownloadState.Completed
            return@withContext
        }

        try {
            val destinationDir = File(modelDir)
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            val zipFile = File(destinationDir, ZIP_FILE_NAME)
            
            // Download
            _downloadState.value = DownloadState.Downloading(0f)
            val url = URL(MODEL_URL)
            val connection = url.openConnection()
            val totalSize = connection.contentLengthLong

            url.openStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            _downloadState.value = DownloadState.Downloading(totalRead.toFloat() / totalSize)
                        }
                    }
                }
            }

            // Extraction
            _downloadState.value = DownloadState.Extracting
            extractZip(zipFile, destinationDir)
            
            // Cleanup zip
            zipFile.delete()

            _downloadState.value = DownloadState.Completed
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Unknown error during download")
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile.mkdirs()
                    FileOutputStream(newFile).use { output ->
                        zipInput.copyTo(output)
                    }
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        
        // Vosk model zip often contains a root folder like "vosk-model-small-en-us-0.15/"
        // We might want to move its content to the root of destDir if needed, 
        // but WakeWordDetector can just point to the subdirectory.
        // For simplicity, let's check if there's a single directory inside and return that path or similar.
    }
}
