package com.autobox.autoboxlauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobox.autoboxlauncher.R
import com.autobox.autoboxlauncher.MainViewModel
import com.autobox.autoboxlauncher.media.BrowseItem
import com.autobox.autoboxlauncher.media.BrowseState
import com.autobox.autoboxlauncher.media.MediaState

@Composable
fun MultimediaScreen(viewModel: MainViewModel) {
    val mediaState by viewModel.mediaState.collectAsState()
    val browseState by viewModel.browseState.collectAsState()

    if (viewModel.selectedMediaApp.value == null) {
        NoMediaAppSelected()
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Left: browse panel
        BrowsePanel(
            state = browseState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onItem = { viewModel.mediaBrowseItem(it) },
            onBack = { viewModel.mediaBrowseBack() },
        )

        // Right: now-playing panel — same weight, volume inside
        NowPlayingPanel(
            mediaState = mediaState,
            volume = viewModel.volume.value,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onVolumeChange = { viewModel.setVolume(it) },
            onPrevious = { viewModel.mediaPrevious() },
            onTogglePlayPause = {
                if (browseState.isFromQueue && !browseState.isConnected) viewModel.mediaStartPlayback()
                else viewModel.mediaTogglePlayPause()
            },
            onNext = { viewModel.mediaNext() },
        )
    }
}

// ---------------------------------------------------------------------------
// Browse panel — left side
// ---------------------------------------------------------------------------

@Composable
private fun BrowsePanel(
    state: BrowseState,
    modifier: Modifier = Modifier,
    onItem: (BrowseItem) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            BrowsePanelHeader(
                title = when {
                    state.isFromQueue -> state.currentTitle ?: stringResource(R.string.multimedia_queue)
                    else -> state.currentTitle ?: stringResource(R.string.multimedia_library)
                },
                canGoBack = state.canGoBack && !state.isFromQueue,
                onBack = onBack,
            )

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Queue mode: waiting for the app to start playing
                    state.isFromQueue && !state.isConnected -> {
                        Text(
                            text = stringResource(R.string.multimedia_queue_waiting),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Browse error (non-queue mode)
                    state.error != null && !state.isFromQueue -> {
                        Text(
                            text = stringResource(R.string.multimedia_load_error),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    state.items.isEmpty() && state.isConnected -> {
                        Text(
                            text = stringResource(R.string.multimedia_empty),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.items, key = { it.mediaId }) { item ->
                                BrowseItemRow(item = item, onClick = { onItem(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowsePanelHeader(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.multimedia_back),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Spacer(Modifier.size(40.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BrowseItemRow(item: BrowseItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Artwork / icon
        BrowseItemArtwork(
            artwork = item.artwork,
            isBrowsable = item.isBrowsable,
            modifier = Modifier.size(44.dp),
        )

        // Title + subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Chevron for browsable items
        if (item.isBrowsable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun BrowseItemArtwork(
    artwork: Bitmap?,
    isBrowsable: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (isBrowsable) RoundedCornerShape(8.dp) else RoundedCornerShape(4.dp)

    if (artwork != null) {
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isBrowsable) Icons.Default.Folder else Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Now-playing panel — right side (minimal, same as before)
// ---------------------------------------------------------------------------

@Composable
private fun NowPlayingPanel(
    mediaState: MediaState,
    volume: Float,
    modifier: Modifier = Modifier,
    onVolumeChange: (Float) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Album art — bounded, won't overflow
            AlbumArtLarge(
                albumArt = mediaState.albumArt,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // Track info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = mediaState.title ?: stringResource(R.string.media_nothing_playing),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (mediaState.artist != null) {
                    Text(
                        text = mediaState.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Transport controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.media_previous),
                        modifier = Modifier.size(26.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (mediaState.isPlaying) stringResource(R.string.media_pause)
                                             else stringResource(R.string.media_play),
                        modifier = Modifier.size(32.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.media_next),
                        modifier = Modifier.size(26.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Volume — inside the tile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Rounded.VolumeDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp),
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    ),
                )
                Icon(
                    Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared / utility composables (kept from original, used by HomeScreen too)
// ---------------------------------------------------------------------------

@Composable
fun MediaCompactContent(
    state: MediaState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Album art
        if (state.albumArt != null) {
            Image(
                bitmap = state.albumArt.asImageBitmap(),
                contentDescription = stringResource(R.string.media_album_art),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.media_music_note),
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Track title + artist
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = state.title ?: stringResource(R.string.media_nothing_playing),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (state.artist != null) {
                Text(
                    text = state.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        CompactTransportControls(
            state = state,
            onPrevious = onPrevious,
            onTogglePlayPause = onTogglePlayPause,
            onNext = onNext,
        )
    }
}

@Composable
private fun AlbumArtLarge(albumArt: Bitmap?, modifier: Modifier = Modifier) {
    val artModifier = modifier.clip(RoundedCornerShape(12.dp))
    if (albumArt != null) {
        Image(
            bitmap = albumArt.asImageBitmap(),
            contentDescription = stringResource(R.string.media_album_art),
            modifier = artModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = artModifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.media_music_note),
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun NoMediaAppSelected() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.multimedia_no_app_selected),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactTransportControls(
    state: MediaState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(52.dp), shape = CircleShape) {
            Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.media_previous),
                modifier = Modifier.size(24.dp))
        }
        FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(68.dp), shape = CircleShape) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) stringResource(R.string.media_pause)
                                     else stringResource(R.string.media_play),
                modifier = Modifier.size(34.dp),
            )
        }
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(52.dp), shape = CircleShape) {
            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.media_next),
                modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun MediaSourceBadge(packageName: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sourceInfo = remember(packageName) { resolveMediaSourceInfo(context, packageName) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sourceInfo.icon?.let { icon ->
                Image(
                    bitmap = icon,
                    contentDescription = sourceInfo.label,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)),
                )
            }
            Text(
                text = sourceInfo.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class MediaSourceInfo(val label: String, val icon: ImageBitmap? = null)

private fun resolveMediaSourceInfo(
    context: android.content.Context,
    packageName: String?,
): MediaSourceInfo {
    if (packageName.isNullOrBlank()) return MediaSourceInfo(context.getString(R.string.media_no_source))
    val pm = context.packageManager
    return runCatching {
        val appInfo = pm.getApplicationInfo(packageName, 0)
        MediaSourceInfo(
            label = pm.getApplicationLabel(appInfo).toString(),
            icon = pm.getApplicationIcon(packageName).toBitmapSafely()?.asImageBitmap(),
        )
    }.getOrElse {
        MediaSourceInfo(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }
}

private fun Drawable.toBitmapSafely(): Bitmap? {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val w = intrinsicWidth.takeIf { it > 0 } ?: 96
    val h = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}
