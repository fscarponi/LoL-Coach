package com.lolcoach.app.tts

interface TtsManager {
    suspend fun speak(text: String)
    fun stop()
}
