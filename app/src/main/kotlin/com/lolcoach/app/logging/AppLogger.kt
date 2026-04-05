package com.lolcoach.app.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LogLevel(val label: String, val emoji: String) {
    DEBUG("DEBUG", "🔍"),
    INFO("INFO", "ℹ️"),
    WARN("WARN", "⚠️"),
    ERROR("ERROR", "❌"),
    EVENT("EVENT", "🎯")
}

data class LogEntry(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val level: LogLevel,
    val source: String,
    val message: String
) {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun formatted(): String =
        "[${timestamp.format(formatter)}] ${level.emoji} [${level.label}] [$source] $message"
}

object AppLogger {
    private const val MAX_LOG_ENTRIES = 500

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(level: LogLevel, source: String, message: String) {
        val entry = LogEntry(level = level, source = source, message = message)
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > MAX_LOG_ENTRIES) {
            current.removeAt(current.lastIndex)
        }
        _logs.value = current
    }

    fun debug(source: String, message: String) = log(LogLevel.DEBUG, source, message)
    fun info(source: String, message: String) = log(LogLevel.INFO, source, message)
    fun warn(source: String, message: String) = log(LogLevel.WARN, source, message)
    fun error(source: String, message: String) = log(LogLevel.ERROR, source, message)
    fun event(source: String, message: String) = log(LogLevel.EVENT, source, message)

    fun clear() {
        _logs.value = emptyList()
    }
}
