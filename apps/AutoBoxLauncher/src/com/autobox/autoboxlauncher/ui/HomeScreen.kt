package com.autobox.autoboxlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobox.autoboxlauncher.R
import com.autobox.autoboxlauncher.MainViewModel
import com.autobox.autoboxlauncher.UnitSystem
import com.autobox.autoboxlauncher.media.MediaState
import com.autobox.autoboxlauncher.ui.components.TomTomMapView
import com.tomtom.sdk.navigation.NavigationState
import kotlin.time.Duration

@Composable
fun HomeScreen(viewModel: MainViewModel, isDark: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DrivingInfo(modifier = Modifier.weight(1f), viewModel = viewModel)
        MapWidget(modifier = Modifier.weight(1.2f), isDark = isDark, viewModel = viewModel)
        MultimediaWidget(modifier = Modifier.weight(1f), viewModel = viewModel)
    }
}


@Composable
fun DrivingInfo(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val speedLimitSignResId by viewModel.speedLimitSignResId.collectAsState()
    val otherSignResId by viewModel.otherSignResId.collectAsState()
    val currentSpeedLimit by viewModel.currentSpeedLimit.collectAsState()
    val signRecognitionEnabled = viewModel.signRecognitionEnabled.value

    val speedKmh by viewModel.speedKmh.collectAsState()
    val isImperial = viewModel.unitSystem.value == UnitSystem.IMPERIAL
    val displaySpeed = if (isImperial) (speedKmh * 0.621371).toInt() else speedKmh
    val speedUnit = if (isImperial) "mph" else "km/h"

    val isOverLimit = signRecognitionEnabled && currentSpeedLimit != null && speedKmh > currentSpeedLimit!!
    val speedColor = if (isOverLimit) Color.Red else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        if (signRecognitionEnabled && speedLimitSignResId != 0) {
            RoadSign(signResId = speedLimitSignResId, size = 72.dp, modifier = Modifier.align(Alignment.TopStart))
        }

        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = (-24).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = displaySpeed.toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = speedColor,
                lineHeight = 80.sp
            )
            Text(
                text = speedUnit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                letterSpacing = 2.sp
            )
        }

        if (signRecognitionEnabled && otherSignResId != 0) {
            RoadSign(signResId = otherSignResId, size = 90.dp, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun RoadSign(
    signResId: Int,
    size: androidx.compose.ui.unit.Dp = 90.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = signResId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun MapWidget(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    viewModel: MainViewModel,
) {
    val navigationUiState = viewModel.navigationUiState.value
    val isActiveGuidance = navigationUiState.navigationState == NavigationState.ActiveGuidance

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
    ) {
        TomTomMapView(
            modifier = Modifier.fillMaxSize(),
            isInteractive = false,
            isDark = isDark,
            showLocationMarker = true,
            followCurrentLocation = true,
            use3dLocationMarker = true,
            trackingZoom = 17.0,
            trackingTilt = 55.0,
            destinationGeoPoint = navigationUiState.destinationGeoPoint,
            routeGeometry = navigationUiState.plannedRoute?.geometry.orEmpty(),
        )

        if (isActiveGuidance) {
            ElevatedCard(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = navigationUiState.guidanceMessage?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.nav_status_active),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        navigationUiState.routeProgress?.remainingDistance?.toString()?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                        navigationUiState.routeProgress?.remainingTime?.let {
                            Text(
                                text = stringResource(R.string.nav_eta, formatDuration(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    viewModel.stopNavigation()
                    viewModel.clearNavigationUi()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.nav_stop_description))
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

@Composable
fun MultimediaWidget(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val mediaState by viewModel.mediaState.collectAsState()
    val browseState by viewModel.browseState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (viewModel.selectedMediaPackage.value == null) {
            Text(
                text = stringResource(R.string.multimedia_no_app_selected),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        } else {
            MediaCompactContent(
                state = mediaState,
                onPrevious = { viewModel.mediaPrevious() },
                onTogglePlayPause = {
                    if (browseState.isFromQueue && !browseState.isConnected) viewModel.mediaStartPlayback()
                    else viewModel.mediaTogglePlayPause()
                },
                onNext = { viewModel.mediaNext() }
            )
        }
    }
}
