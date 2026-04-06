package com.lolcoach.bridge.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import javax.sound.sampled.TargetDataLine

class WakeWordDetector(
    private val scope: CoroutineScope,
    private val modelPath: String,
    private val wakeWord: String = "hey coach"
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var job: Job? = null
    private var line: TargetDataLine? = null

    private val _onWakeWordDetected = MutableSharedFlow<Unit>()
    val onWakeWordDetected = _onWakeWordDetected.asSharedFlow()

    fun start(device: VoiceDevice? = null) {
        stop()
        
        if (!File(modelPath).exists()) {
            println("[ERROR] Vosk model not found at $modelPath")
            return
        }

        job = scope.launch(Dispatchers.IO) {
            try {
                if (model == null) {
                    model = Model(modelPath)
                }
                
                // Configurazione recognizer per la wake word specifica
                // Vosk accetta una lista di parole per migliorare l'accuratezza
                val grammar = "[\"$wakeWord\", \"[unk]\"]"
                recognizer = Recognizer(model!!, 16000f, grammar)
                
                line = AudioCaptureProvider.createLine(device)
                line?.start()

                val buffer = ByteArray(4096)
                while (line?.isOpen == true) {
                    val read = line?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        if (recognizer?.acceptWaveForm(buffer, read) == true) {
                            val result = recognizer?.result
                            if (result?.contains(wakeWord, ignoreCase = true) == true) {
                                _onWakeWordDetected.emit(Unit)
                            }
                        } else {
                            val partial = recognizer?.partialResult
                            if (partial?.contains(wakeWord, ignoreCase = true) == true) {
                                _onWakeWordDetected.emit(Unit)
                                recognizer?.reset() // Reset per evitare trigger multipli
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[ERROR] WakeWordDetector: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        job?.cancel()
        line?.stop()
        line?.close()
        line = null
        recognizer?.close()
        recognizer = null
    }
}
