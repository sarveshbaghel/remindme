package com.taskly.app.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    fun play(filePath: String, onCompletion: () -> Unit = {}) {
        val file = File(filePath)
        if (!file.exists()) return

        if (currentFilePath == filePath && mediaPlayer != null) {
            mediaPlayer?.start()
            _isPlaying.value = true
            startTicker()
            return
        }

        release()

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    stopTicker()
                    onCompletion()
                }
            }

            mediaPlayer = player
            currentFilePath = filePath
            _durationMs.value = player.duration.toLong().coerceAtLeast(0L)
            _currentPositionMs.value = 0L

            player.start()
            _isPlaying.value = true
            startTicker()
        } catch (e: Exception) {
            e.printStackTrace()
            release()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopTicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause(filePath: String, onCompletion: () -> Unit = {}) {
        if (_isPlaying.value) {
            pause()
        } else {
            play(filePath, onCompletion)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    _currentPositionMs.value = it.currentPosition.toLong()
                }
                delay(100)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun release() {
        stopTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            currentFilePath = null
            _isPlaying.value = false
            _currentPositionMs.value = 0L
        }
    }
}

