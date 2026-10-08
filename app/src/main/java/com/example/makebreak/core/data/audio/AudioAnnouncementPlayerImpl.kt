package com.example.makebreak.core.data.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import com.example.makebreak.core.domain.service.AudioAnnouncementPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class AudioAnnouncementPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioAnnouncementPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var lastStartFile: String? = null
    private var lastFinishedFile: String? = null
    private var playbackWakeLock: PowerManager.WakeLock? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    /**
     * Plays a random break start voice announcement without immediate repetition.
     * Invoked by: TimerEngine, SettingsViewModel.
     */
    override fun playBreakStart(volume: Float) {
        playRandomAsset("audio/BreakStart", volume, isStart = true)
    }

    /**
     * Plays a random break finished voice announcement without immediate repetition.
     * Invoked by: TimerEngine, SettingsViewModel.
     */
    override fun playBreakFinished(volume: Float) {
        playRandomAsset("audio/BreakFinished", volume, isStart = false)
    }

    /**
     * Stops currently playing voice announcement.
     * Invoked by: TimerEngine, SettingsViewModel.
     */
    override fun stop() {
        releasePlayer()
    }

    private fun playRandomAsset(folderPath: String, volume: Float, isStart: Boolean) {
        val files = listFiles(folderPath)
        if (files.isEmpty()) {
            return
        }

        val selectedFile = selectNextFile(files, isStart)
        updateLastPlayed(selectedFile, isStart)
        playFile("$folderPath/$selectedFile", volume)
    }

    private fun selectNextFile(files: List<String>, isStart: Boolean): String {
        val lastFile = if (isStart) lastStartFile else lastFinishedFile
        if (files.size <= 1) {
            return files.first()
        }
        val availableFiles = files.filter { it != lastFile }
        val index = Random.nextInt(availableFiles.size)
        return availableFiles[index]
    }

    private fun updateLastPlayed(fileName: String, isStart: Boolean) {
        if (isStart) {
            lastStartFile = fileName
        } else {
            lastFinishedFile = fileName
        }
    }

    private fun listFiles(folderPath: String): List<String> {
        return try {
            context.assets.list(folderPath)?.filter { it.endsWith(".mp3") } ?: emptyList()
        } catch (exception: IOException) {
            Log.e(TAG, "Failed to list assets in $folderPath", exception)
            emptyList()
        }
    }

    private fun playFile(assetPath: String, volume: Float) {
        releasePlayer()
        acquirePlaybackWakeLock()
        try {
            val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
            val player = MediaPlayer()
            player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            player.setAudioAttributes(audioAttributes)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.setVolume(volume, volume)
            player.setOnCompletionListener {
                releasePlayer()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                releasePlayer()
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to play audio asset $assetPath", exception)
            releasePlayer()
        }
    }

    private fun acquirePlaybackWakeLock() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        playbackWakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            PLAYBACK_WAKE_LOCK_TAG
        )?.apply {
            setReferenceCounted(false)
            acquire(PLAYBACK_WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releasePlaybackWakeLock() {
        if (playbackWakeLock?.isHeld == true) {
            playbackWakeLock?.release()
        }
        playbackWakeLock = null
    }

    private fun releasePlayer() {
        releasePlaybackWakeLock()
        try {
            mediaPlayer?.stop()
        } catch (exception: IllegalStateException) {
            Log.w(TAG, "MediaPlayer stop failed in illegal state", exception)
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "AudioAnnouncementPlayer"
        private const val PLAYBACK_WAKE_LOCK_TAG = "MakeBreak:AudioPlaybackWakeLock"
        private const val PLAYBACK_WAKE_LOCK_TIMEOUT_MS = 15_000L
    }
}
