package com.lolcoach.app.i18n

object Strings {
    const val Connection = "Connection"
    const val Lockfile = "Lockfile"
    const val LiveClient = "Live Client"
    const val NotFound = "Not found"
    const val NotActive = "Not active"
    const val Connected = "Connected"
    
    fun lastSnapshot(seconds: Long) = "Last snapshot ${seconds}s ago"
    
    const val Idle = "IDLE — Disconnected"
    const val ChampSelect = "CHAMPION SELECT"
    const val Loading = "LOADING"
    const val InGame = "IN GAME"
    const val PostGame = "POST GAME"
    
    const val DisconnectedStatus = "Disconnected"
    const val LoadingStatus = "Loading..."
    const val InGameStatus = "In Game"
    const val PostGameStatus = "Post Game"
    
    // Voice
    const val VoiceCoaching = "Voice Coaching"
    const val EhyCoach = "Ehy Coach!"
    const val Microphone = "Microphone"
    const val NoDevices = "No microphones found"
    const val Refresh = "Refresh"
    const val Listening = "Listening..."
    const val Thinking = "Thinking..."
    
    const val VoskModelMissing = "Vosk model missing"
    const val DownloadModel = "Download Model (~40MB)"
    const val ModelDownloading = "Downloading model..."
    const val ModelExtracting = "Extracting..."
    const val ModelReady = "Model ready"
    const val ModelError = "Error downloading model"
    
    const val VoiceSetupGuide = "Setup Guide"
    const val VoiceSetupInstruction1 = "1. Enable Voice Coaching above"
    const val VoiceSetupInstruction2 = "2. Download the Vosk model if not present"
    const val VoiceSetupInstruction3 = "3. Select your preferred microphone"
    const val VoiceSetupInstruction4 = "4. Say 'Ehy Coach' followed by your question"
}
