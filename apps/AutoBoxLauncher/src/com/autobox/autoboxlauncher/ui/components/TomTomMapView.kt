package com.autobox.autoboxlauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChanged
import com.tomtom.sdk.annotations.InternalTomTomSdkApi
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.map.display.camera.InitialCameraOptions
import com.tomtom.sdk.map.display.compose.TomTomMap
import com.tomtom.sdk.map.display.compose.model.MapDisplayInfrastructure
import com.tomtom.sdk.map.display.compose.model.MarkerData
import com.tomtom.sdk.map.display.compose.model.PolylineData
import com.tomtom.sdk.map.display.compose.nodes.CurrentLocationMarker
import com.tomtom.sdk.map.display.compose.nodes.Marker
import com.tomtom.sdk.map.display.compose.nodes.Polyline
import com.tomtom.sdk.map.display.compose.properties.CurrentLocationMarkerProperties
import com.tomtom.sdk.map.display.compose.properties.MarkerProperties
import com.tomtom.sdk.map.display.compose.properties.PolylineProperties
import com.tomtom.sdk.map.display.compose.state.MapViewState
import com.tomtom.sdk.map.display.compose.state.rememberMapViewState
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.style.StyleMode

@OptIn(ExperimentalComposeUiApi::class, InternalTomTomSdkApi::class)
@Composable
fun TomTomMapView(
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    isDark: Boolean = false,
    showLocationMarker: Boolean = false,
    followCurrentLocation: Boolean = false,
    use3dLocationMarker: Boolean = false,
    trackingZoom: Double = 16.5,
    trackingTilt: Double = 0.0,
    destinationGeoPoint: GeoPoint? = null,
    routeGeometry: List<GeoPoint> = emptyList(),
    onMapViewState: ((MapViewState) -> Unit)? = null,
    onUserPan: (() -> Unit)? = null,
) {
    // Guard against "MapView is already created": the TomTom SDK allows only one MapView at a
    // time. NavHost removes the old screen from composition in the same applyChanges pass that
    // adds the new one, but onRemembered fires before onDispose. We defer TomTomMap creation
    // to the next coroutine dispatch (after applyChanges returns), by which point onDispose of
    // the previous TomTomMapView has already run and the old MapView is destroyed.
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }
    if (!ready) return

    val clippedModifier = modifier.clipToBounds()

    val effectiveModifier = when {
        !isInteractive -> clippedModifier.pointerInteropFilter { true }
        onUserPan != null -> clippedModifier.pointerInput(onUserPan) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.positionChanged() }) {
                        onUserPan()
                        return@awaitEachGesture
                    }
                    if (event.changes.none { it.pressed }) break
                }
            }
        }
        else -> clippedModifier
    }

    val initialCameraOptions = remember {
        InitialCameraOptions.LocationBased(
            position = GeoPoint(52.2297, 21.0122),
            zoom = 13.0
        )
    }

    val mapViewState = rememberMapViewState(
        initialCameraOptions = initialCameraOptions,
        renderToTextureView = true,
    )

    LaunchedEffect(isDark) {
        mapViewState.styleState.styleMode = if (isDark) StyleMode.DARK else StyleMode.MAIN
    }

    var currentLocation by remember(showLocationMarker) {
        mutableStateOf<GeoLocation?>(TomTomSdk.locationProvider.lastKnownLocation)
    }

    DisposableEffect(showLocationMarker) {
        if (!showLocationMarker) {
            onDispose { }
        } else {
            runCatching { TomTomSdk.locationProvider.enable() }
            val listener = OnLocationUpdateListener { location ->
                currentLocation = location
            }
            TomTomSdk.locationProvider.addOnLocationUpdateListener(listener)
            onDispose {
                TomTomSdk.locationProvider.removeOnLocationUpdateListener(listener)
            }
        }
    }

    val infrastructure = remember(showLocationMarker) {
        val base = MapDisplayInfrastructure(sdkContext = TomTomSdk.sdkContext)
        if (!showLocationMarker) {
            return@remember base
        }

        val locInfra = base.locationInfrastructure.copy {
            locationProvider = TomTomSdk.locationProvider
        }

        base.copy {
            locationInfrastructure = locInfra
        }
    }

    LaunchedEffect(currentLocation, trackingZoom, trackingTilt) {
        val location = currentLocation ?: return@LaunchedEffect
        if (!followCurrentLocation) {
            return@LaunchedEffect
        }

        mapViewState.cameraState.moveCamera(
            com.tomtom.sdk.map.display.camera.CameraOptions(
                position = location.position,
                zoom = trackingZoom,
                tilt = trackingTilt,
                rotation = location.course?.inDegrees(),
                fieldOfView = null,
            )
        )
    }

    val currentLocationMarkerProperties = remember(use3dLocationMarker) {
        CurrentLocationMarkerProperties()
    }

    val destinationPinImage = remember {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = android.graphics.Color.RED
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 5f, paint)
        ImageFactory.fromBitmap(bitmap)
    }

    val carMarkerImage = remember {
        val width = 84
        val height = 132
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(56, 0, 0, 0)
        }
        canvas.drawOval(18f, 96f, 66f, 122f, shadowPaint)

        val bodyPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(28, 140, 255)
        }
        val roofPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(205, 235, 255)
        }
        val accentPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(14, 33, 61)
        }
        val wheelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
        }
        val lightPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(255, 244, 173)
        }

        canvas.drawRoundRect(18f, 12f, 66f, 108f, 24f, 24f, bodyPaint)
        canvas.drawRoundRect(24f, 28f, 60f, 78f, 16f, 16f, roofPaint)
        canvas.drawRoundRect(28f, 82f, 56f, 98f, 10f, 10f, accentPaint)

        canvas.drawRoundRect(12f, 28f, 20f, 54f, 6f, 6f, wheelPaint)
        canvas.drawRoundRect(64f, 28f, 72f, 54f, 6f, 6f, wheelPaint)
        canvas.drawRoundRect(12f, 68f, 20f, 94f, 6f, 6f, wheelPaint)
        canvas.drawRoundRect(64f, 68f, 72f, 94f, 6f, 6f, wheelPaint)

        canvas.drawRoundRect(28f, 16f, 40f, 22f, 3f, 3f, lightPaint)
        canvas.drawRoundRect(44f, 16f, 56f, 22f, 3f, 3f, lightPaint)

        ImageFactory.fromBitmap(bitmap)
    }

    val carMarkerProperties = remember(carMarkerImage) {
        MarkerProperties(pinImage = carMarkerImage).copy {
            placementAnchor = Offset(0.5f, 0.5f)
        }
    }

    onMapViewState?.invoke(mapViewState)

    TomTomMap(
        infrastructure = infrastructure,
        state = mapViewState,
        modifier = effectiveModifier,
        content = {
            if (showLocationMarker) {
                if (use3dLocationMarker) {
                    currentLocation?.let { location ->
                        Marker(
                            data = MarkerData(geoPoint = location.position),
                            properties = carMarkerProperties,
                        )
                    }
                } else {
                    CurrentLocationMarker(properties = currentLocationMarkerProperties)
                }
            }
            destinationGeoPoint?.let { geoPoint ->
                Marker(
                    data = MarkerData(geoPoint = geoPoint),
                    properties = MarkerProperties(pinImage = destinationPinImage),
                )
            }
            if (routeGeometry.size > 1) {
                Polyline(
                    data = PolylineData(geoPoints = routeGeometry),
                    properties = PolylineProperties(),
                )
            }
        }
    )
}
