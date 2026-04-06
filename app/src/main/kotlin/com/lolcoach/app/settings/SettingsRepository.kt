package com.lolcoach.app.settings

import com.lolcoach.app.logging.AppLogger
import com.lolcoach.app.ui.dashboard.AppSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PersistedSettings(
    val overlayEnabled: Boolean = true,
    val ttsEnabled: Boolean = true,
    val llmEnabled: Boolean = true,
    val llmBaseUrl: String = "http://localhost:11434/v1",
    val llmModel: String = "",
    val llmTemperature: Double = 0.7,
    val pollingIntervalMs: Long = 1000,
    val voiceEnabled: Boolean = false,
    val wakeWord: String = "hey coach"
)

class SettingsRepository(
    settingsFilePath: String? = null
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val settingsFile: File by lazy {
        if (settingsFilePath != null) {
            File(settingsFilePath)
        } else {
            val configDir = File(System.getProperty("user.home"), ".lolcoach")
            configDir.mkdirs()
            File(configDir, "settings.json")
        }
    }

    fun load(): PersistedSettings {
        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                json.decodeFromString<PersistedSettings>(content).also {
                    AppLogger.info("Settings", "Settings loaded from ${settingsFile.absolutePath}")
                }
            } else {
                AppLogger.info("Settings", "No settings file found, using defaults")
                PersistedSettings()
            }
        } catch (e: Exception) {
            AppLogger.warn("Settings", "Failed to load settings: ${e.message}")
            PersistedSettings()
        }
    }

    fun save(settings: PersistedSettings) {
        try {
            val content = json.encodeToString(PersistedSettings.serializer(), settings)
            settingsFile.writeText(content)
            AppLogger.debug("Settings", "Settings saved to ${settingsFile.absolutePath}")
        } catch (e: Exception) {
            AppLogger.error("Settings", "Failed to save settings: ${e.message}")
        }
    }

    fun toAppSettings(persisted: PersistedSettings) = AppSettings(
        overlayEnabled = persisted.overlayEnabled,
        ttsEnabled = persisted.ttsEnabled,
        llmEnabled = persisted.llmEnabled,
        llmBaseUrl = persisted.llmBaseUrl,
        llmModel = persisted.llmModel,
        llmTemperature = persisted.llmTemperature,
        pollingIntervalMs = persisted.pollingIntervalMs
    )

    fun fromAppSettings(
        appSettings: AppSettings,
        voiceEnabled: Boolean,
        wakeWord: String
    ) = PersistedSettings(
        overlayEnabled = appSettings.overlayEnabled,
        ttsEnabled = appSettings.ttsEnabled,
        llmEnabled = appSettings.llmEnabled,
        llmBaseUrl = appSettings.llmBaseUrl,
        llmModel = appSettings.llmModel,
        llmTemperature = appSettings.llmTemperature,
        pollingIntervalMs = appSettings.pollingIntervalMs,
        voiceEnabled = voiceEnabled,
        wakeWord = wakeWord
    )
}
