package com.dynamicisland.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.dynamicisland.overlay.IslandEventBus
import com.dynamicisland.overlay.IslandState
import com.dynamicisland.service.IslandNotificationListener

class MediaCallbackManager(private val context: Context) {

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private var activeController: MediaController? = null
    private var activeCallback: MediaController.Callback? = null

    private val sessionListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            controllers?.firstOrNull()?.let { registerCallbacks(it) }
                ?: clearCallbacks()
        }

    init {
        val component = ComponentName(context, IslandNotificationListener::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, component)
            // Check current sessions
            mediaSessionManager.getActiveSessions(component).firstOrNull()?.let {
                registerCallbacks(it)
            }
        } catch (e: SecurityException) {
            // Notification listener not enabled yet
        }
    }

    private fun registerCallbacks(controller: MediaController) {
        clearCallbacks()
        activeController = controller

        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                emitCurrentState(controller)
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                emitCurrentState(controller)
            }

            override fun onSessionDestroyed() {
                clearCallbacks()
                IslandEventBus.dismiss()
            }
        }

        activeCallback = callback
        controller.registerCallback(callback)
        emitCurrentState(controller)
    }

    private fun emitCurrentState(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState ?: return

        val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

        // Only show if actively playing or just paused
        if (playbackState.state == PlaybackState.STATE_STOPPED ||
            playbackState.state == PlaybackState.STATE_NONE) {
            IslandEventBus.dismiss()
            return
        }

        IslandEventBus.post(
            IslandState.Music(
                title = title,
                artist = artist,
                albumArt = albumArt,
                isPlaying = isPlaying,
                packageName = controller.packageName ?: ""
            )
        )
    }

    private fun clearCallbacks() {
        activeCallback?.let { callback ->
            activeController?.unregisterCallback(callback)
        }
        activeController = null
        activeCallback = null
    }

    fun getActiveController(): MediaController? = activeController

    fun release() {
        clearCallbacks()
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
    }
}
