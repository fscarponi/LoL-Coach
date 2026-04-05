package com.lolcoach.bridge

import com.lolcoach.bridge.client.KtorClientFactory
import com.lolcoach.bridge.client.LcuWebSocketClient
import com.lolcoach.bridge.client.LiveClientPoller
import com.lolcoach.bridge.lockfile.LockfileMonitor
import com.lolcoach.bridge.model.LockfileData
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import com.lolcoach.bridge.client.retryWithBackoff
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class BridgeFacade(
    private val scope: CoroutineScope,
    lockfilePath: String? = null
) {
    private val lockfileMonitor = LockfileMonitor(lockfilePath)
    private val lcuWebSocketClient = LcuWebSocketClient()
    private val liveClientPoller = LiveClientPoller()

    private val _lockfileData = MutableStateFlow<LockfileData?>(null)
    val lockfileData: StateFlow<LockfileData?> = _lockfileData.asStateFlow()

    private val _champSelectEvents = MutableSharedFlow<ChampSelectSession>(replay = 1)
    val champSelectEvents: SharedFlow<ChampSelectSession> = _champSelectEvents.asSharedFlow()

    private val _gameSnapshots = MutableSharedFlow<GameSnapshot>(replay = 1)
    val gameSnapshots: SharedFlow<GameSnapshot> = _gameSnapshots.asSharedFlow()

    private var lcuClient: HttpClient? = null
    private var liveClient: HttpClient? = null
    private var lcuJob: Job? = null
    private var pollerJob: Job? = null

    fun start() {
        // Monitor lockfile
        scope.launch {
            lockfileMonitor.monitorFlow().collect { data ->
                _lockfileData.value = data
                if (data != null) {
                    onClientConnected(data)
                } else {
                    onClientDisconnected()
                }
            }
        }

        // Start live client data polling
        liveClient = KtorClientFactory.createLiveClientDataClient()
        pollerJob = scope.launch {
            liveClientPoller.pollFlow(liveClient!!).filterNotNull().collect { snapshot ->
                _gameSnapshots.emit(snapshot)
            }
        }
    }

    private fun onClientConnected(lockfileData: LockfileData) {
        lcuClient?.close()
        lcuJob?.cancel()

        lcuClient = KtorClientFactory.createLcuClient(lockfileData)
        lcuJob = scope.launch {
            retryWithBackoff(maxRetries = Int.MAX_VALUE, initialDelayMs = 2000, maxDelayMs = 15000) {
                lcuWebSocketClient.connectFlow(lcuClient!!, lockfileData).collect { session ->
                    _champSelectEvents.emit(session)
                }
            }
        }
    }

    private fun onClientDisconnected() {
        lcuJob?.cancel()
        lcuClient?.close()
        lcuJob = null
        lcuClient = null
    }

    fun stop() {
        lcuJob?.cancel()
        pollerJob?.cancel()
        lcuClient?.close()
        liveClient?.close()
    }
}
