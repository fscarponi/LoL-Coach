package com.lolcoach.app.tts

import com.lolcoach.brain.event.GameEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class TtsEventListener(
    private val scope: CoroutineScope,
    private val ttsManager: TtsManager,
    private val gameEvents: SharedFlow<GameEvent>
) {
    private val speechQueue = Channel<String>(capacity = Channel.BUFFERED)

    fun start() {
        // Collect events and enqueue speech
        scope.launch {
            gameEvents.collect { event ->
                speechQueue.send(event.message)
            }
        }

        // Process speech queue sequentially to avoid overlapping
        scope.launch {
            for (text in speechQueue) {
                ttsManager.speak(text)
            }
        }
    }

    fun stop() {
        speechQueue.close()
        ttsManager.stop()
    }
}
