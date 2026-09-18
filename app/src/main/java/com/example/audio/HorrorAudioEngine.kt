package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class HorrorAudioEngine {
  private val sampleRate = 22050
  private var audioTrack: AudioTrack? = null
  private var audioJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Default)

  var isMuted: Boolean = false

  // Sound triggers & parameters
  @Volatile var heartbeatBpm: Float = 70f
  @Volatile var heartbeatVolume: Float = 0.5f
  @Volatile var monsterProximity: Float = 0f // 0 (far) to 1 (touching)
  @Volatile var isMonsterChasing: Boolean = false

  // One-shot triggers
  @Volatile private var triggerFootstep = false
  @Volatile private var triggerJumpscare = false
  @Volatile private var triggerDoorUnlock = false
  @Volatile private var triggerClick = false
  @Volatile private var triggerMonsterRoar = false

  fun start() {
    if (audioJob != null) return

    val bufferSize = AudioTrack.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_OUT_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    )

    try {
      audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufferSize * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()

      audioJob = scope.launch {
        renderAudioLoop(bufferSize)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun stop() {
    audioJob?.cancel()
    audioJob = null
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (e: Exception) {
      // ignore
    }
    audioTrack = null
  }

  fun playFootstep() {
    triggerFootstep = true
  }

  fun playJumpscare() {
    triggerJumpscare = true
  }

  fun playDoorUnlock() {
    triggerDoorUnlock = true
  }

  fun playClick() {
    triggerClick = true
  }

  fun playMonsterRoar() {
    triggerMonsterRoar = true
  }

  private fun renderAudioLoop(bufferSize: Int) {
    val chunk = ShortArray(bufferSize)
    var phaseAmbiance = 0.0
    var phaseDrone = 0.0
    var sampleCounter = 0L

    // Heartbeat state
    var heartPhase = 0.0

    // One-shot sample counters
    var footstepSamplesLeft = 0
    var jumpscareSamplesLeft = 0
    var doorSamplesLeft = 0
    var clickSamplesLeft = 0
    var roarSamplesLeft = 0

    while (scope.isActive) {
      val track = audioTrack ?: break

      // Check one-shot triggers
      if (triggerFootstep) {
        triggerFootstep = false
        footstepSamplesLeft = (sampleRate * 0.12).toInt()
      }
      if (triggerJumpscare) {
        triggerJumpscare = false
        jumpscareSamplesLeft = (sampleRate * 1.5).toInt()
      }
      if (triggerDoorUnlock) {
        triggerDoorUnlock = false
        doorSamplesLeft = (sampleRate * 0.4).toInt()
      }
      if (triggerClick) {
        triggerClick = false
        clickSamplesLeft = (sampleRate * 0.05).toInt()
      }
      if (triggerMonsterRoar) {
        triggerMonsterRoar = false
        roarSamplesLeft = (sampleRate * 1.2).toInt()
      }

      for (i in chunk.indices) {
        if (isMuted) {
          chunk[i] = 0
          continue
        }

        var sample = 0.0

        // 1. Dark ambient drone (low sub-bass 55Hz + modulated 82Hz)
        val droneFreq = 55.0 + sin(sampleCounter * 0.0003) * 4.0
        phaseDrone += 2.0 * PI * droneFreq / sampleRate
        if (phaseDrone > 2.0 * PI) phaseDrone -= 2.0 * PI
        sample += sin(phaseDrone) * 0.12

        // Second eerie high harmonic
        phaseAmbiance += 2.0 * PI * 110.0 / sampleRate
        if (phaseAmbiance > 2.0 * PI) phaseAmbiance -= 2.0 * PI
        sample += sin(phaseAmbiance) * 0.04

        // 2. Dynamic Heartbeat (double thud)
        val heartPeriodSamples = (sampleRate * 60.0 / heartbeatBpm.coerceIn(50f, 180f)).toInt()
        val heartPos = (sampleCounter % heartPeriodSamples).toInt()
        val tSec = heartPos.toDouble() / sampleRate

        // First thud at t=0, second thud at t=0.18s
        var heartThud = 0.0
        if (tSec < 0.12) {
          val env = exp(-tSec * 35.0)
          heartThud += sin(2.0 * PI * 58.0 * tSec) * env
        } else if (tSec in 0.18..0.30) {
          val t2 = tSec - 0.18
          val env = exp(-t2 * 40.0)
          heartThud += sin(2.0 * PI * 50.0 * t2) * env * 0.8
        }
        val currentHeartVol = (heartbeatVolume * (0.3 + monsterProximity * 0.7)).coerceIn(0.0, 1.0)
        sample += heartThud * currentHeartVol * 0.55

        // 3. Monster proximity growl / hiss
        if (monsterProximity > 0.15f || isMonsterChasing) {
          val growlFreq = 42.0 + sin(sampleCounter * 0.002) * 15.0
          val growlWave = sin(sampleCounter * (2.0 * PI * growlFreq / sampleRate))
          val noise = (Math.random() - 0.5) * 0.2
          val growlVol = if (isMonsterChasing) 0.35 else (monsterProximity * 0.2).toDouble()
          sample += (growlWave * 0.7 + noise) * growlVol
        }

        // 4. One-shot effects
        if (footstepSamplesLeft > 0) {
          val t = 1.0 - (footstepSamplesLeft.toDouble() / (sampleRate * 0.12))
          val stepNoise = (Math.random() - 0.5) * exp(-t * 12.0) * 0.25
          val stepThud = sin(2.0 * PI * 90.0 * t * 0.1) * exp(-t * 15.0) * 0.2
          sample += stepNoise + stepThud
          footstepSamplesLeft--
        }

        if (jumpscareSamplesLeft > 0) {
          val progress = 1.0 - (jumpscareSamplesLeft.toDouble() / (sampleRate * 1.5))
          // Harsh screech: discordant frequencies + heavy distorted noise
          val screech1 = sin(sampleCounter * 0.35)
          val screech2 = sin(sampleCounter * 0.49)
          val blastNoise = (Math.random() - 0.5) * 0.6
          val decay = exp(-progress * 2.5)
          sample += (screech1 * 0.4 + screech2 * 0.4 + blastNoise) * decay * 0.85
          jumpscareSamplesLeft--
        }

        if (roarSamplesLeft > 0) {
          val progress = 1.0 - (roarSamplesLeft.toDouble() / (sampleRate * 1.2))
          val roarFreq = 80.0 - progress * 30.0 + sin(sampleCounter * 0.01) * 20.0
          val roarWave = sin(sampleCounter * (2.0 * PI * roarFreq / sampleRate))
          val roarNoise = (Math.random() - 0.5) * 0.3
          sample += (roarWave * 0.6 + roarNoise) * exp(-progress * 1.8) * 0.5
          roarSamplesLeft--
        }

        if (doorSamplesLeft > 0) {
          val progress = 1.0 - (doorSamplesLeft.toDouble() / (sampleRate * 0.4))
          val creakFreq = 300.0 + sin(progress * 20.0) * 80.0
          val creak = sin(sampleCounter * (2.0 * PI * creakFreq / sampleRate)) * exp(-progress * 4.0) * 0.3
          sample += creak
          doorSamplesLeft--
        }

        if (clickSamplesLeft > 0) {
          val click = (Math.random() - 0.5) * 0.4
          sample += click
          clickSamplesLeft--
        }

        sampleCounter++
        val clamped = (sample * 32767.0).toInt().coerceIn(-32767, 32767)
        chunk[i] = clamped.toShort()
      }

      track.write(chunk, 0, chunk.size)
    }
  }
}
