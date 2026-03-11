package com.autobox.autoboxlauncher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val packageName: String? = null,
    val hasSession: Boolean = false,
)

/**
 * Manages media playback integration with two-tier strategy:
 *
 * Tier 1 — MediaBrowser: full browse tree + controls. Works with apps that
 *   trust our certificate/permission (e.g. Lineage Twelve, local players).
 *
 * Tier 2 — MediaSessionManager fallback: when MediaBrowser is rejected
 *   (e.g. Spotify certificate check), attaches to the app's MediaSession
 *   via MEDIA_CONTENT_CONTROL permission. The browse panel shows the
 *   current playback queue instead of the content library.
 */
class MediaRepository(private val context: Context) {

    companion object {
        private const val TAG = "AutoBox.Media"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)

    // MediaBrowser (Tier 1)
    private var browser: MediaBrowser? = null
    private var connectedComponent: ComponentName? = null

    // Active controller (from either tier)
    private var controller: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null

    // MediaSessionManager fallback (Tier 2)
    private var targetPackage: String? = null
    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            val pkg = targetPackage ?: return@OnActiveSessionsChangedListener
            attachBestSession(controllers, pkg)
        }

    // MediaBrowser browse state
    private var currentNodeId: String? = null
    private val backStack = ArrayDeque<Pair<String, String?>>()

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState: StateFlow<MediaState> = _mediaState

    private val _browseState = MutableStateFlow(BrowseState())
    val browseState: StateFlow<BrowseState> = _browseState

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun connect(serviceComponent: ComponentName) {
        if (browser?.isConnected == true && connectedComponent == serviceComponent) return
        disconnect()
        targetPackage = serviceComponent.packageName
        _browseState.value = BrowseState(isLoading = true)

        val cb = object : MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                val b = browser ?: return
                Log.d(TAG, "MediaBrowser connected to ${serviceComponent.packageName}, root=${b.root}")
                try { attachController(MediaController(context, b.sessionToken)) }
                catch (e: Exception) { Log.w(TAG, "MediaController from token failed", e) }
                browseTo(nodeId = b.root, title = null)
            }

            override fun onConnectionFailed() {
                Log.w(TAG, "MediaBrowser rejected by ${serviceComponent.packageName} — using queue fallback")
                _browseState.value = BrowseState(error = "connection_failed", isFromQueue = true)
                startSessionFallback(serviceComponent.packageName)
            }

            override fun onConnectionSuspended() {
                Log.d(TAG, "MediaBrowser suspended")
                detachController()
                _browseState.value = BrowseState()
            }
        }

        try {
            browser = MediaBrowser(context, serviceComponent, cb, null)
            connectedComponent = serviceComponent
            browser!!.connect()
            Log.d(TAG, "Connecting to ${serviceComponent.packageName}")
        } catch (e: Exception) {
            Log.e(TAG, "connect() threw", e)
            _browseState.value = BrowseState(error = "connect_threw: ${e.message}", isFromQueue = true)
            startSessionFallback(serviceComponent.packageName)
        }
    }

    fun disconnect() {
        stopSessionFallback()
        currentNodeId?.let { browser?.unsubscribe(it) }
        detachController()
        browser?.disconnect()
        browser = null
        connectedComponent = null
        targetPackage = null
        currentNodeId = null
        backStack.clear()
        _browseState.value = BrowseState()
        _mediaState.value = MediaState()
    }

    fun openItem(item: BrowseItem) {
        when {
            item.queueId != null ->
                controller?.transportControls?.skipToQueueItem(item.queueId)
            item.isBrowsable -> {
                backStack.addLast(Pair(currentNodeId ?: return, _browseState.value.currentTitle))
                browseTo(item.mediaId, item.title)
            }
            item.isPlayable ->
                controller?.transportControls?.playFromMediaId(item.mediaId, null)
        }
    }

    fun goBack() {
        val (parentId, title) = backStack.removeLastOrNull() ?: return
        browseTo(parentId, title)
    }

    fun play()     { controller?.transportControls?.play() }
    fun pause()    { controller?.transportControls?.pause() }
    fun next()     { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }

    fun togglePlayPause() {
        if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) pause() else play()
    }

    /**
     * Attempts to start playback for [packageName] without launching any Activity.
     *
     * Tries three approaches in order:
     *  1. If the app already has an active session — call play() directly.
     *  2. Send ACTION_MEDIA_BUTTON broadcast targeted at the package — wakes up
     *     the app's MediaButtonReceiver (same as pressing a headset play button).
     *  3. Fallback: start the MediaBrowserService as a regular service — this
     *     triggers the service's onCreate() which typically creates the MediaSession.
     */
    fun startPlayback(packageName: String) {
        // 1. Existing session
        try {
            val existing = sessionManager.getActiveSessions(null)
                ?.firstOrNull { it.packageName == packageName }
            if (existing != null) {
                existing.transportControls.play()
                return
            }
        } catch (_: SecurityException) {}

        val keyDown = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
        val keyUp = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_UP,   android.view.KeyEvent.KEYCODE_MEDIA_PLAY)

        // 2. Targeted broadcast to the app's MediaButtonReceiver
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                setPackage(packageName)
                putExtra(android.content.Intent.EXTRA_KEY_EVENT, keyDown)
            }
            context.sendBroadcast(intent)
            intent.putExtra(android.content.Intent.EXTRA_KEY_EVENT, keyUp)
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent MEDIA_BUTTON broadcast to $packageName")
            return
        } catch (e: Exception) {
            Log.w(TAG, "MEDIA_BUTTON broadcast failed for $packageName", e)
        }

        // 3. Start the MediaBrowserService directly (no activity, no browse auth needed)
        try {
            val svcIntent = android.content.Intent("android.media.browse.MediaBrowserService").apply {
                setPackage(packageName)
            }
            context.startService(svcIntent)
            Log.d(TAG, "Started MediaBrowserService for $packageName")
        } catch (e: Exception) {
            Log.w(TAG, "startService failed for $packageName", e)
        }

        // 4. Global media key as last resort
        val audioManager = context.getSystemService(android.media.AudioManager::class.java)
        audioManager.dispatchMediaKeyEvent(keyDown)
        audioManager.dispatchMediaKeyEvent(keyUp)
        Log.d(TAG, "Dispatched global MEDIA_PLAY key event")
    }

    // -------------------------------------------------------------------------
    // Tier 2: MediaSessionManager fallback
    // -------------------------------------------------------------------------

    private fun startSessionFallback(packageName: String) {
        stopSessionFallback()
        targetPackage = packageName
        try {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener, null, mainHandler
            )
            attachBestSession(sessionManager.getActiveSessions(null), packageName)
        } catch (e: SecurityException) {
            Log.e(TAG, "MEDIA_CONTENT_CONTROL not granted", e)
        }
    }

    private fun stopSessionFallback() {
        try { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        catch (_: Exception) {}
    }

    private fun attachBestSession(controllers: List<MediaController>?, packageName: String) {
        val best = controllers?.firstOrNull { it.packageName == packageName }
        if (best?.sessionToken == controller?.sessionToken) return
        detachController()
        val c = best ?: run { _mediaState.value = MediaState(); return }
        Log.d(TAG, "Attaching session-manager controller for $packageName")
        attachController(c)
    }

    // -------------------------------------------------------------------------
    // Browse tree (Tier 1 only)
    // -------------------------------------------------------------------------

    private fun browseTo(nodeId: String, title: String?) {
        val b = browser ?: return
        currentNodeId?.let { b.unsubscribe(it) }
        currentNodeId = nodeId

        _browseState.value = _browseState.value.copy(
            isLoading = true,
            currentTitle = title,
            canGoBack = backStack.isNotEmpty(),
            items = emptyList(),
            error = null,
            isFromQueue = false,
        )

        b.subscribe(nodeId, object : MediaBrowser.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: List<MediaBrowser.MediaItem>) {
                if (parentId != currentNodeId) return
                Log.d(TAG, "Loaded ${children.size} items for $parentId")
                _browseState.value = _browseState.value.copy(
                    isConnected = true,
                    isLoading = false,
                    items = children.mapNotNull { it.toItem() },
                )
            }

            override fun onError(parentId: String) {
                if (parentId != currentNodeId) return
                _browseState.value = _browseState.value.copy(isLoading = false, error = "load_error")
            }
        })
    }

    // -------------------------------------------------------------------------
    // Controller lifecycle
    // -------------------------------------------------------------------------

    private fun attachController(newController: MediaController) {
        detachController()
        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = updateMedia(newController)
            override fun onPlaybackStateChanged(state: PlaybackState?) = updateMedia(newController)
            override fun onQueueChanged(queue: List<MediaSession.QueueItem>?) =
                updateQueue(newController, queue)
            override fun onSessionDestroyed() {
                detachController()
                val pkg = targetPackage ?: return
                try { attachBestSession(sessionManager.getActiveSessions(null), pkg) }
                catch (_: SecurityException) {}
            }
        }
        newController.registerCallback(cb, mainHandler)
        controller = newController
        controllerCallback = cb
        updateMedia(newController)
        // Load queue immediately if we're in fallback mode
        if (_browseState.value.isFromQueue) {
            updateQueue(newController, newController.queue)
        }
    }

    private fun detachController() {
        controllerCallback?.let { controller?.unregisterCallback(it) }
        controller = null
        controllerCallback = null
        _mediaState.value = MediaState()
    }

    private fun updateMedia(c: MediaController) {
        val meta = c.metadata
        _mediaState.value = MediaState(
            title       = meta?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist      = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                       ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            albumArt    = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                       ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying   = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = c.packageName,
            hasSession  = true,
        )
    }

    private fun updateQueue(c: MediaController, queue: List<MediaSession.QueueItem>?) {
        if (!_browseState.value.isFromQueue) return
        val activeId = c.playbackState?.activeQueueItemId ?: MediaSession.QueueItem.UNKNOWN_ID
        val items = queue?.map { qi ->
            val desc = qi.description
            BrowseItem(
                mediaId    = desc.mediaId ?: qi.queueId.toString(),
                title      = desc.title?.toString() ?: "—",
                subtitle   = desc.subtitle?.toString(),
                artwork    = desc.iconBitmap,
                isPlayable = true,
                queueId    = qi.queueId,
            )
        } ?: emptyList()

        _browseState.value = _browseState.value.copy(
            isConnected  = true,
            isLoading    = false,
            currentTitle = c.queueTitle?.toString(),
            items        = items,
            error        = if (items.isEmpty()) "queue_empty" else null,
        )
    }

    private fun MediaBrowser.MediaItem.toItem(): BrowseItem? {
        val id    = mediaId ?: return null
        val title = description.title?.toString() ?: return null
        return BrowseItem(
            mediaId     = id,
            title       = title,
            subtitle    = description.subtitle?.toString(),
            artwork     = description.iconBitmap,
            isBrowsable = isBrowsable,
            isPlayable  = isPlayable,
        )
    }
}
