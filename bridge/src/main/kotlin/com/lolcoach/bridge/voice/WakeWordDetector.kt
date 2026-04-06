package com.lolcoach.bridge.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import javax.sound.sampled.TargetDataLine

class WakeWordDetector(
    private val scope: CoroutineScope,
    private val modelPath: String,
    private var wakeWord: String = "hey coach"
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var queryRecognizer: Recognizer? = null
    private var job: Job? = null
    private var line: TargetDataLine? = null

    private val _onWakeWordDetected = MutableSharedFlow<Unit>()
    val onWakeWordDetected = _onWakeWordDetected.asSharedFlow()

    private val _isListeningForQuery = MutableStateFlow(false)
    val isListeningForQuery = _isListeningForQuery.asStateFlow()

    private val _onQueryTranscribed = MutableSharedFlow<String>()
    val onQueryTranscribed = _onQueryTranscribed.asSharedFlow()

    fun setWakeWord(newWakeWord: String) {
        this.wakeWord = newWakeWord.lowercase()
        // If already running, we need to restart to update the grammar
        if (job?.isActive == true) {
            start(null) // Restarts with current device
        }
    }

    private var currentDevice: VoiceDevice? = null

    fun start(device: VoiceDevice? = null) {
        if (device != null) currentDevice = device
        stop()
        
        // Find the actual model directory (Vosk expects a folder containing am, conf, ivector etc)
        val actualModelPath = findModelPath(modelPath)
        if (actualModelPath == null) {
            println("[ERROR] Vosk model not found at $modelPath")
            return
        }

        job = scope.launch(Dispatchers.IO) {
            try {
                if (model == null) {
                    model = Model(actualModelPath)
                }
                
                // Configurazione recognizer per la wake word specifica
                val grammar = "[\"$wakeWord\", \"[unk]\"]"
                recognizer = Recognizer(model!!, 16000f, grammar)
                
                // Recognizer generico per la query (senza grammatica restrittiva)
                queryRecognizer = Recognizer(model!!, 16000f)
                
                line = AudioCaptureProvider.createLine(currentDevice)
                line?.start()

                val buffer = ByteArray(4096)
                while (line?.isOpen == true) {
                    val read = line?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        if (!_isListeningForQuery.value) {
                            // Modalità WAKE WORD
                            if (recognizer?.acceptWaveForm(buffer, read) == true) {
                                val result = recognizer?.result
                                if (result?.contains(wakeWord, ignoreCase = true) == true) {
                                    handleWakeWordDetected()
                                }
                            } else {
                                val partial = recognizer?.partialResult
                                if (partial?.contains(wakeWord, ignoreCase = true) == true) {
                                    handleWakeWordDetected()
                                    recognizer?.reset()
                                }
                            }
                        } else {
                            // Modalità QUERY (STT)
                            if (queryRecognizer?.acceptWaveForm(buffer, read) == true) {
                                val result = queryRecognizer?.result
                                // Esempio risultato: { "text" : "what should i do" }
                                val text = extractText(result)
                                if (text.isNotBlank()) {
                                    _onQueryTranscribed.emit(text)
                                    _isListeningForQuery.value = false
                                    queryRecognizer?.reset()
                                }
                            }
                            // Opzionale: gestire timeout o silenzio per chiudere la query
                        }
                    }
                }
            } catch (e: Exception) {
                println("[ERROR] WakeWordDetector: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleWakeWordDetected() {
        _onWakeWordDetected.emit(Unit)
        _isListeningForQuery.value = true
        queryRecognizer?.reset()
    }

    private fun extractText(json: String?): String {
        if (json == null) return ""
        // Parsing molto grezzo per evitare dipendenze extra in bridge se possibile, 
        // ma dato che abbiamo kotlinx.serialization in altri moduli potremmo usarlo.
        // Per ora facciamo un semplice split.
        return json.substringAfter("\"text\" : \"").substringBefore("\"")
    }

    fun stop() {
        job?.cancel()
        line?.stop()
        line?.close()
        line = null
        recognizer?.close()
        recognizer = null
        queryRecognizer?.close()
        queryRecognizer = null
        _isListeningForQuery.value = false
    }

    private fun findModelPath(path: String): String? {
        val root = File(path)
        if (!root.exists()) return null
        
        // If it's a valid model dir directly (contains 'am' or 'conf')
        if (File(root, "am").exists() || File(root, "conf").exists()) {
            return root.absolutePath
        }
        
        // Check subdirectories (Vosk zip often extracts into a subfolder)
        root.listFiles()?.filter { it.isDirectory }?.forEach { sub ->
            if (File(sub, "am").exists() || File(sub, "conf").exists()) {
                return sub.absolutePath
            }
        }
        
        return null
    }
}
