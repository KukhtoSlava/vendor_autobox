package com.autobox.autoboxlauncher.media

import android.graphics.Bitmap

data class BrowseItem(
    val mediaId: String,
    val title: String,
    val subtitle: String? = null,
    val artwork: Bitmap? = null,
    val isBrowsable: Boolean = false,
    val isPlayable: Boolean = false,
    /** Non-null when this item represents a MediaSession queue entry. */
    val queueId: Long? = null,
)

data class BrowseState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val currentTitle: String? = null,
    val canGoBack: Boolean = false,
    val items: List<BrowseItem> = emptyList(),
    val error: String? = null,
    /** True when [items] are queue entries (not a browse tree). */
    val isFromQueue: Boolean = false,
)
