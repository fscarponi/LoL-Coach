package com.lolcoach.bridge.model

data class LockfileData(
    val processName: String,
    val pid: Int,
    val port: Int,
    val token: String,
    val protocol: String
)
