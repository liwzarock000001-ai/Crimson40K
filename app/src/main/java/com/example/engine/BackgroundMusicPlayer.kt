package com.example.engine

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

object BackgroundMusicPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume: Float = 0.8f
    private var isPlayingEnabled: Boolean = true

    fun init(context: Context) {
        if (mediaPlayer != null) return
        try {
            val afd = context.applicationContext.assets.openFd("bgm_theme.mp3")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(currentVolume, currentVolume)
                prepare()
                if (isPlayingEnabled) {
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("BGM", "Failed to initialize background music", e)
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }

    fun setEnabled(enabled: Boolean) {
        isPlayingEnabled = enabled
        mediaPlayer?.let { mp ->
            if (enabled) {
                if (!mp.isPlaying) {
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        Log.e("BGM", "Failed to start BGM", e)
                    }
                }
            } else {
                if (mp.isPlaying) {
                    mp.pause()
                }
            }
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("BGM", "Failed to release BGM", e)
        }
    }
}
