package com.lolcoach.bridge.client

import com.lolcoach.bridge.model.LockfileData
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

class LcuWebSocketClient(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val _champSelectEvents = MutableSharedFlow<ChampSelectSession>(replay = 1)
    val champSelectEvents: SharedFlow<ChampSelectSession> = _champSelectEvents.asSharedFlow()

    fun connectFlow(client: HttpClient, lockfileData: LockfileData): Flow<ChampSelectSession> = flow {
        val host = "127.0.0.1"
        val port = lockfileData.port

        client.wss(host = host, port = port, path = "/") {
            // Subscribe to champ select events
            val subscribeMsg = """[5, "OnJsonApiEvent_lol-champ-select_v1_session"]"""
            send(Frame.Text(subscribeMsg))

            while (currentCoroutineContext().isActive) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    parseChampSelectEvent(text)?.let { session ->
                        _champSelectEvents.emit(session)
                        emit(session)
                    }
                }
            }
        }
    }

    private fun parseChampSelectEvent(rawMessage: String): ChampSelectSession? {
        return try {
            val jsonElement = json.parseToJsonElement(rawMessage)
            if (jsonElement is JsonArray && jsonElement.size >= 3) {
                val opCode = jsonElement[0].jsonPrimitive.int
                if (opCode == 8) {
                    val dataStr = jsonElement[2].toString()
                    // The data payload contains an object with "data" field
                    val wrapper = json.parseToJsonElement(dataStr)
                    val data = wrapper.jsonArray.toString()
                    json.decodeFromString<ChampSelectSession>(data)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
