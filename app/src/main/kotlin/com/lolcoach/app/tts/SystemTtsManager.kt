package com.lolcoach.app.tts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemTtsManager : TtsManager {

    private var currentProcess: Process? = null

    override suspend fun speak(text: String) {
        withContext(Dispatchers.IO) {
            val sanitized = text.replace("\"", "").replace("'", "")
            val os = System.getProperty("os.name").lowercase()

            val command = when {
                os.contains("mac") -> listOf("say", "-v", "Alice", sanitized)
                os.contains("win") -> listOf(
                    "powershell", "-Command",
                    "Add-Type -AssemblyName System.Speech; " +
                        "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('$sanitized')"
                )
                else -> listOf("espeak", "-v", "it", sanitized)
            }

            try {
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                currentProcess = process
                process.waitFor()
                currentProcess = null
            } catch (e: Exception) {
                // TTS not available, silently ignore
            }
        }
    }

    override fun stop() {
        currentProcess?.destroyForcibly()
        currentProcess = null
    }
}
