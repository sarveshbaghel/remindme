package com.taskly.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var startTimestamp: Long = 0L

    var isRecording: Boolean = false
        private set

    fun start(outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            startTimestamp = System.currentTimeMillis()
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            false
        }
    }

    fun stop(): Long {
        if (!isRecording) return 0L
        val duration = System.currentTimeMillis() - startTimestamp
        try {
            recorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            releaseRecorder()
        }
        return duration.coerceAtLeast(0L)
    }

    fun cancel(outputFile: File?) {
        try {
            if (isRecording) {
                recorder?.stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            releaseRecorder()
            outputFile?.let {
                if (it.exists()) it.delete()
            }
        }
    }

    private fun releaseRecorder() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
            isRecording = false
        }
    }
}

