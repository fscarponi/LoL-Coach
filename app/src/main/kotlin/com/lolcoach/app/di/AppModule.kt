package com.lolcoach.app.di

import com.lolcoach.app.settings.SettingsRepository
import com.lolcoach.app.tts.SystemTtsManager
import com.lolcoach.app.tts.TtsManager
import com.lolcoach.brain.event.EventProcessor
import com.lolcoach.brain.llm.LlmCoachService
import com.lolcoach.brain.llm.LlmConfig
import com.lolcoach.brain.state.GameStateMachine
import com.lolcoach.brain.strategy.StrategyEngine
import com.lolcoach.bridge.BridgeFacade
import com.lolcoach.bridge.voice.VoskModelDownloader
import com.lolcoach.bridge.voice.WakeWordDetector
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { SettingsRepository() }
    single { GameStateMachine() }
    single { StrategyEngine() }
    single { LlmConfig() }

    single(named("modelPath")) {
        System.getenv("VOSK_MODEL_PATH") ?: "models/vosk-model"
    }
    single { VoskModelDownloader(get(named("modelPath"))) }

    factory<TtsManager> { SystemTtsManager() }
}

fun bridgeModule(scope: kotlinx.coroutines.CoroutineScope) = module {
    single { BridgeFacade(scope) }
    single { WakeWordDetector(scope, get(named("modelPath"))) }
    single { EventProcessor(scope, get<GameStateMachine>(), get<StrategyEngine>().allStrategies()) }
    single {
        val config = get<LlmConfig>()
        if (config.enabled) LlmCoachService(scope, config.createProvider()) else null
    }
}
