package com.lolcoach.app.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PiperTtsManager(
    private val piperPath: String = "piper",
    private val modelPath: String = "it_IT-riccardo-x_low.onnx",
    private val outputDir: String = System.getProperty("java.io.tmpdir")
) : TtsManager {

    private var currentProcess: Process? = null

    override suspend fun speak(text: String) {
        withContext(Dispatchers.IO) {
            val sanitized = text.replace("\"", "").replace("'", "")
            val outputFile = File(outputDir, "lolcoach_tts_${System.currentTimeMillis()}.wav")

            try {
                // Piper reads from stdin and writes WAV to file
                val piperProcess = ProcessBuilder(
                    piperPath,
                    "--model", modelPath,
                    "--output_file", outputFile.absolutePath
                )
                    .redirectErrorStream(true)
                    .start()

                piperProcess.outputStream.bufferedWriter().use { writer ->
                    writer.write(sanitized)
                }
                piperProcess.waitFor()

                // Play the generated WAV file
                if (outputFile.exists()) {
                    val os = System.getProperty("os.name").lowercase()
                    val playCommand = when {
                        os.contains("mac") -> listOf("afplay", outputFile.absolutePath)
                        os.contains("win") -> listOf(
                            "powershell", "-Command",
                            "(New-Object Media.SoundPlayer '${outputFile.absolutePath}').PlaySync()"
                        )
                        else -> listOf("aplay", outputFile.absolutePath)
                    }

                    val playProcess = ProcessBuilder(playCommand)
                        .redirectErrorStream(true)
                        .start()
                    currentProcess = playProcess
                    playProcess.waitFor()
                    currentProcess = null

                    outputFile.delete()
                }
            } catch (e: Exception) {
                // Piper not available, silently ignore
                outputFile.delete()
            }
        }
    }

    override fun stop() {
        currentProcess?.destroyForcibly()
        currentProcess = null
    }
}
