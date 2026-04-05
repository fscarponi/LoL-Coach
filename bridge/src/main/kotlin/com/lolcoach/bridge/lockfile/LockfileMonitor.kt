package com.lolcoach.bridge.lockfile

import com.lolcoach.bridge.model.LockfileData
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class LockfileMonitor(
    private val lockfilePath: String? = null,
    private val pollIntervalMs: Long = 2000L
) {
    private val _lockfileData = MutableStateFlow<LockfileData?>(null)
    val lockfileData: StateFlow<LockfileData?> = _lockfileData.asStateFlow()

    fun monitorFlow(): Flow<LockfileData?> = flow {
        val path = lockfilePath ?: LockfileReader.findLockfilePath()
        while (currentCoroutineContext().isActive) {
            val data = path?.let { LockfileReader.readFromFile(it) }
            if (data != _lockfileData.value) {
                _lockfileData.value = data
                emit(data)
            }
            delay(pollIntervalMs)
        }
    }
}
