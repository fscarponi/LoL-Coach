package com.lolcoach.bridge.voice

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine

data class VoiceDevice(val name: String, val info: Mixer.Info?)

object AudioCaptureProvider {
    private val format = AudioFormat(16000f, 16, 1, true, false)

    fun listDevices(): List<VoiceDevice> {
        val devices = mutableListOf<VoiceDevice>()
        val mixerInfos = AudioSystem.getMixerInfo()
        val lineInfo = DataLine.Info(TargetDataLine::class.java, format)

        for (info in mixerInfos) {
            val mixer = AudioSystem.getMixer(info)
            if (mixer.isLineSupported(lineInfo)) {
                devices.add(VoiceDevice(info.name, info))
            }
        }
        return devices
    }

    fun createLine(device: VoiceDevice?): TargetDataLine {
        val lineInfo = DataLine.Info(TargetDataLine::class.java, format)
        val mixer = if (device?.info != null) AudioSystem.getMixer(device.info) else null
        
        val line = if (mixer != null) {
            mixer.getLine(lineInfo) as TargetDataLine
        } else {
            AudioSystem.getLine(lineInfo) as TargetDataLine
        }
        
        line.open(format)
        return line
    }
}
