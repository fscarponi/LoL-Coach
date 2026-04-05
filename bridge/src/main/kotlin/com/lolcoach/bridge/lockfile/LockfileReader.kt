package com.lolcoach.bridge.lockfile

import com.lolcoach.bridge.model.LockfileData
import java.io.File

object LockfileReader {

    fun parse(content: String): LockfileData {
        val parts = content.trim().split(":")
        require(parts.size == 5) {
            "Invalid lockfile format. Expected 5 parts separated by ':', got ${parts.size}: $content"
        }
        return LockfileData(
            processName = parts[0],
            pid = parts[1].toInt(),
            port = parts[2].toInt(),
            token = parts[3],
            protocol = parts[4]
        )
    }

    fun readFromFile(lockfilePath: String): LockfileData? {
        val file = File(lockfilePath)
        if (!file.exists() || !file.canRead()) return null
        return try {
            parse(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun findLockfilePath(): String? {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> findWindowsLockfile()
            os.contains("mac") -> findMacLockfile()
            else -> null
        }
    }

    private fun findWindowsLockfile(): String? {
        val commonPaths = listOf(
            "C:\\Riot Games\\League of Legends\\lockfile",
            "D:\\Riot Games\\League of Legends\\lockfile"
        )
        return commonPaths.firstOrNull { File(it).exists() }
    }

    private fun findMacLockfile(): String? {
        val path = "/Applications/League of Legends.app/Contents/LoL/lockfile"
        return if (File(path).exists()) path else null
    }
}
