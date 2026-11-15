package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.AudioTrackItem

class MusicPlayerManager(
    private val context: Context,
    private val onTrackChanged: (index: Int, isPlaying: Boolean) -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var tracks: List<AudioTrackItem> = emptyList()
    private var currentIndex: Int = -1
    private var volume: Float = 0.8f
    private var isPlaying: Boolean = false

    fun setPlaylist(newTracks: List<AudioTrackItem>) {
        tracks = newTracks
        if (tracks.isEmpty()) {
            stopAndRelease()
            currentIndex = -1
            onTrackChanged(-1, false)
        } else if (currentIndex >= tracks.size) {
            currentIndex = 0
            onTrackChanged(currentIndex, isPlaying)
        }
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (_: Exception) {
        }
    }

    fun resolveTrackTitle(uri: Uri): String {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            var name: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(idx)
                }
            }
            (name ?: uri.lastPathSegment ?: "Local Song")
                .substringBeforeLast('.')
                .ifBlank { "Local Song" }
        } catch (_: Exception) {
            uri.lastPathSegment?.substringBeforeLast('.') ?: "Local Song"
        }
    }

    fun playTrack(index: Int) {
        if (index !in tracks.indices) return
        val item = tracks[index]
        stopAndRelease()
        try {
            val uri = Uri.parse(item.uriString)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                setVolume(volume, volume)
                setOnPreparedListener { player ->
                    player.start()
                    this@MusicPlayerManager.isPlaying = true
                    this@MusicPlayerManager.currentIndex = index
                    onTrackChanged(index, true)
                }
                setOnCompletionListener {
                    next()
                }
                setOnErrorListener { _, _, _ ->
                    this@MusicPlayerManager.isPlaying = false
                    onTrackChanged(index, false)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
            currentIndex = index
        } catch (_: Exception) {
            isPlaying = false
            onTrackChanged(index, false)
        }
    }

    fun togglePlayPause() {
        if (tracks.isEmpty()) return
        val mp = mediaPlayer
        if (mp == null) {
            playTrack(if (currentIndex in tracks.indices) currentIndex else 0)
            return
        }
        try {
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
            onTrackChanged(currentIndex, isPlaying)
        } catch (_: Exception) {
            playTrack(0)
        }
    }

    fun next() {
        if (tracks.isEmpty()) return
        val nextIdx = (currentIndex + 1) % tracks.size
        playTrack(nextIdx)
    }

    fun previous() {
        if (tracks.isEmpty()) return
        val prevIdx = if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
        playTrack(prevIdx)
    }

    fun stopAndRelease() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isPlaying = false
    }
}
