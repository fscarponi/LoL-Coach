package com.lolcoach.bridge.client

import com.lolcoach.bridge.model.liveclient.GameSnapshot
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class LiveClientPoller(
    private val pollIntervalMs: Long = 1000L,
    private val baseUrl: String = "https://127.0.0.1:2999",
    private val json: Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
) {
    private val _snapshots = MutableSharedFlow<GameSnapshot>(replay = 1)
    val snapshots: SharedFlow<GameSnapshot> = _snapshots.asSharedFlow()

    fun pollFlow(client: HttpClient): Flow<GameSnapshot?> = flow {
        while (currentCoroutineContext().isActive) {
            val snapshot = fetchSnapshot(client)
            if (snapshot != null) {
                _snapshots.emit(snapshot)
            }
            emit(snapshot)
            delay(pollIntervalMs)
        }
    }

    private suspend fun fetchSnapshot(client: HttpClient): GameSnapshot? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/liveclientdata/allgamedata")
            val body = response.bodyAsText()
            json.decodeFromString<GameSnapshot>(body)
        } catch (e: Exception) {
            null
        }
    }
}
