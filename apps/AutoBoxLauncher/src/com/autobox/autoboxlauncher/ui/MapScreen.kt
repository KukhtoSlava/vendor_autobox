package com.autobox.autoboxlauncher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.autobox.autoboxlauncher.BuildConfig
import com.autobox.autoboxlauncher.R
import com.autobox.autoboxlauncher.MainViewModel
import com.autobox.autoboxlauncher.ui.components.TomTomMapView
import com.tomtom.sdk.annotations.InternalTomTomSdkApi
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.compose.state.MapViewState
import com.tomtom.sdk.navigation.NavigationState
import com.tomtom.sdk.navigation.progress.RouteProgress
import com.tomtom.sdk.search.Search
import com.tomtom.sdk.search.SearchOptions
import com.tomtom.sdk.search.model.SearchResultType
import com.tomtom.sdk.search.model.result.SearchResult
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.ItineraryPoint
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.GuidanceOptions
import com.tomtom.sdk.routing.route.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val DefaultMapCenter = GeoPoint(52.2297, 21.0122)

private data class DestinationSuggestion(
    val primaryText: String,
    val secondaryText: String = "",
    val coordinate: GeoPoint? = null,
)

@OptIn(InternalTomTomSdkApi::class)
@Composable
fun MapScreen(viewModel: MainViewModel, isDark: Boolean = false) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val routePlanner = remember {
        com.tomtom.sdk.routing.RoutePlanner(context.applicationContext, BuildConfig.TOMTOM_API_KEY)
    }
    val search = remember {
        Search(context.applicationContext, BuildConfig.TOMTOM_API_KEY)
    }

    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var mapState by remember { mutableStateOf<MapViewState?>(null) }
    var isFollowingVehicle by remember { mutableStateOf(true) }
    LaunchedEffect(mapState) {
        val state = mapState ?: return@LaunchedEffect
        val location = TomTomSdk.locationProvider.lastKnownLocation ?: return@LaunchedEffect
        state.cameraState.moveCamera(
            CameraOptions(
                position = location.position,
                zoom = 17.0,
                tilt = 55.0,
                rotation = location.course?.inDegrees(),
            )
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<DestinationSuggestion>>(emptyList()) }
    var suppressNextSearch by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var isPlanningRoute by remember { mutableStateOf(false) }
    val navigationUiState = viewModel.navigationUiState.value
    val plannedRoute = navigationUiState.plannedRoute
    val destinationGeoPoint = navigationUiState.destinationGeoPoint
    val routeProgress = navigationUiState.routeProgress
    val guidanceMessage = navigationUiState.guidanceMessage
    val navigationMessage = navigationUiState.navigationMessage
    val navigationState = navigationUiState.navigationState

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocation) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(searchQuery, hasLocation) {
        if (suppressNextSearch) {
            suppressNextSearch = false
            return@LaunchedEffect
        }
        val query = searchQuery.trim()
        if (query.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        delay(250)
        val geoBias = currentOrigin(mapState)

        val result = withContext(Dispatchers.IO) {
            search.search(
                SearchOptions(
                    query = query,
                    geoBias = geoBias,
                    limit = 5,
                    locale = Locale.getDefault(),
                    resultTypes = setOf(
                        SearchResultType.Address,
                        SearchResultType.Street,
                        SearchResultType.CrossStreet,
                    ),
                )
            )
        }

        suggestions = if (result.isSuccess()) {
            result.value().results
                .mapNotNull { it.toSuggestion() }
                .distinctBy { Triple(it.primaryText, it.secondaryText, it.coordinate) }
        } else {
            emptyList()
        }
    }

    DisposableEffect(routePlanner, search) {
        onDispose {
            routePlanner.close()
            search.close()
        }
    }

    fun clearRoute() {
        suggestions = emptyList()
        if (navigationState != NavigationState.Idle) {
            viewModel.stopNavigation()
        }
        viewModel.clearNavigationUi()
    }

    fun planRouteToDestination(destination: GeoPoint) {
        val routeOptions = RoutePlanningOptions(
            itinerary = Itinerary(
                origin = ItineraryPoint(currentOrigin(mapState)),
                destination = ItineraryPoint(destination),
                waypoints = emptyList(),
            ),
            guidanceOptions = GuidanceOptions(language = Locale.getDefault()),
        )

        isPlanningRoute = true
        viewModel.setNavigationMessage(null)
        coroutineScope.launch(Dispatchers.IO) {
            val result = routePlanner.planRoute(routeOptions)
            withContext(Dispatchers.Main) {
                isPlanningRoute = false
                if (result.isSuccess()) {
                    val route = result.value().routes.firstOrNull()
                    if (route != null) {
                        viewModel.setPlannedRoute(
                            route = route,
                            routeOptions = routeOptions,
                            destinationGeoPoint = destination,
                        )
                        mapState?.cameraState?.moveCamera(
                            CameraOptions(position = destination, zoom = 14.0)
                        )
                    } else {
                        viewModel.setNavigationMessage(context.getString(R.string.nav_error_route_not_returned))
                    }
                } else {
                    viewModel.setNavigationMessage(result.failure().toString())
                }
            }
        }
    }

    fun resolveDestinationQuery(query: String) {
        if (query.isBlank()) {
            return
        }

        isSearching = true
        viewModel.setNavigationMessage(null)
        val geoBias = currentOrigin(mapState)
        coroutineScope.launch(Dispatchers.IO) {
            val result = search.search(
                SearchOptions(
                    query = query,
                    geoBias = geoBias,
                    limit = 5,
                    locale = Locale.getDefault(),
                    resultTypes = setOf(
                        SearchResultType.Address,
                        SearchResultType.Street,
                        SearchResultType.CrossStreet,
                        SearchResultType.Poi,
                    ),
                )
            )
            withContext(Dispatchers.Main) {
                isSearching = false
                if (result.isSuccess()) {
                    val place = result.value().results.firstOrNull()?.place
                    if (place == null) {
                        viewModel.setNavigationMessage(context.getString(R.string.nav_error_destination_not_found))
                        return@withContext
                    }

                    val geoPoint = place.coordinate
                    suggestions = emptyList()
                    planRouteToDestination(geoPoint)
                } else {
                    viewModel.setNavigationMessage(result.failure().toString())
                }
            }
        }
    }

    fun performSearch() {
        suggestions.firstOrNull()?.coordinate?.let { coordinate ->
            suggestions = emptyList()
            planRouteToDestination(coordinate)
            return
        }
        resolveDestinationQuery(searchQuery.trim())
    }

    fun startNavigation() {
        viewModel.startNavigation()
        suggestions = emptyList()
        isFollowingVehicle = true
        TomTomSdk.locationProvider.lastKnownLocation?.let { location ->
            mapState?.cameraState?.moveCamera(
                CameraOptions(
                    position = location.position,
                    zoom = 17.0,
                    tilt = 55.0,
                    rotation = location.course?.inDegrees(),
                )
            )
        }
    }

    val isNavigationUiFocused = navigationState == NavigationState.ActiveGuidance

    Box(modifier = Modifier.fillMaxSize()) {
        TomTomMapView(
            modifier = Modifier.fillMaxSize(),
            isInteractive = true,
            isDark = isDark,
            showLocationMarker = hasLocation,
            followCurrentLocation = isNavigationUiFocused && isFollowingVehicle,
            use3dLocationMarker = true,
            trackingZoom = 17.0,
            trackingTilt = if (isNavigationUiFocused) 55.0 else 0.0,
            destinationGeoPoint = destinationGeoPoint,
            routeGeometry = plannedRoute?.geometry.orEmpty(),
            onMapViewState = { state -> mapState = state },
            onUserPan = { if (isNavigationUiFocused) isFollowingVehicle = false },
        )

        if (!isNavigationUiFocused) {
            SearchPanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, end = 72.dp),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                isSearching = isSearching || isPlanningRoute,
                suggestions = suggestions,
                onSearch = { performSearch() },
                onSuggestionClick = { suggestion ->
                    keyboardController?.hide()
                    suppressNextSearch = true
                    searchQuery = suggestion.primaryText
                    suggestions = emptyList()
                    suggestion.coordinate?.let(::planRouteToDestination) ?: resolveDestinationQuery(suggestion.primaryText)
                },
                onClear = {
                    searchQuery = ""
                    clearRoute()
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MapControlButton(icon = Icons.Default.Add, description = stringResource(R.string.map_zoom_in)) {
                mapState?.cameraState?.let { camera ->
                    val zoom = ((camera.data?.position?.zoom ?: 15.0) + 1.0).coerceAtMost(20.0)
                    camera.moveCamera(CameraOptions(zoom = zoom))
                }
            }
            MapControlButton(icon = Icons.Default.Remove, description = stringResource(R.string.map_zoom_out)) {
                mapState?.cameraState?.let { camera ->
                    val zoom = ((camera.data?.position?.zoom ?: 15.0) - 1.0).coerceAtLeast(1.0)
                    camera.moveCamera(CameraOptions(zoom = zoom))
                }
            }
            if (hasLocation) {
                HorizontalDivider(modifier = Modifier.width(40.dp))
                MapControlButton(icon = Icons.Default.MyLocation, description = stringResource(R.string.map_my_location)) {
                    val state = mapState ?: return@MapControlButton
                    val location = state.locationState.getCurrentLocation() ?: return@MapControlButton
                    coroutineScope.launch {
                        state.cameraState.animateCamera(
                            CameraOptions(
                                position = location.position,
                                zoom = if (isNavigationUiFocused) 17.0 else 15.0,
                                tilt = if (isNavigationUiFocused) 55.0 else 0.0,
                                rotation = if (isNavigationUiFocused) location.course?.inDegrees() else null,
                            ),
                            800.milliseconds,
                        )
                        isFollowingVehicle = true
                    }
                }
            }
        }

        if (isNavigationUiFocused) {
            CompactNavigationBanner(
                guidanceMessage = guidanceMessage,
                progress = routeProgress,
                route = plannedRoute,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, end = 84.dp)
            )

            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.stopNavigation()
                },
                icon = { Icon(Icons.Default.Stop, contentDescription = null) },
                text = { Text(stringResource(R.string.nav_stop)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp)
            )
        } else if (
            plannedRoute != null ||
            navigationState != NavigationState.Idle ||
            navigationMessage != null
        ) {
            NavigationPanel(
                route = plannedRoute,
                progress = routeProgress,
                guidanceMessage = guidanceMessage,
                navigationState = navigationState,
                message = navigationMessage,
                onStartNavigation = { startNavigation() },
                onStopNavigation = {
                    viewModel.stopNavigation()
                },
                onClearRoute = {
                    clearRoute()
                    searchQuery = ""
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun CompactNavigationBanner(
    guidanceMessage: String?,
    progress: RouteProgress?,
    route: Route?,
    modifier: Modifier = Modifier,
) {
    val distanceText = progress?.remainingDistance?.toString() ?: route?.summary?.length?.toString()
    val timeText = progress?.remainingTime?.let(::formatDuration) ?: route?.summary?.travelTime?.let(::formatDuration)

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = guidanceMessage?.takeIf { it.isNotBlank() } ?: stringResource(R.string.nav_status_active),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                distanceText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
                timeText?.let {
                    Text(
                        text = stringResource(R.string.nav_eta, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

private fun currentOrigin(mapState: MapViewState?): GeoPoint {
    return mapState?.locationState?.getCurrentLocation()?.position
        ?: TomTomSdk.locationProvider.lastKnownLocation?.position
        ?: mapState?.cameraState?.data?.position?.position
        ?: DefaultMapCenter
}

@Composable
private fun NavigationPanel(
    route: Route?,
    progress: RouteProgress?,
    guidanceMessage: String?,
    navigationState: NavigationState,
    message: String?,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onClearRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when (navigationState) {
                    NavigationState.ActiveGuidance -> stringResource(R.string.nav_status_active)
                    NavigationState.FreeDriving -> stringResource(R.string.nav_status_free_driving)
                    NavigationState.Idle -> stringResource(R.string.nav_status_route_ready)
                    else -> navigationState.toString()
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            guidanceMessage?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            val distanceText = progress?.remainingDistance?.toString() ?: route?.summary?.length?.toString()
            val timeText = progress?.remainingTime?.let(::formatDuration) ?: route?.summary?.travelTime?.let(::formatDuration)
            if (distanceText != null || timeText != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    distanceText?.let {
                        Text(
                            text = stringResource(R.string.nav_distance, it),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    timeText?.let {
                        Text(
                            text = stringResource(R.string.nav_eta, it),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (navigationState == NavigationState.ActiveGuidance) {
                    OutlinedButton(onClick = onStopNavigation) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.nav_stop))
                    }
                } else {
                    Button(
                        onClick = onStartNavigation,
                        enabled = route != null
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.nav_start))
                    }
                }

                OutlinedButton(onClick = onClearRoute) {
                    Text(stringResource(R.string.nav_clear))
                }
            }
        }
    }
}

@Composable
private fun SearchPanel(
    suggestions: List<DestinationSuggestion>,
    onSuggestionClick: (DestinationSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MapSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            isSearching = isSearching,
            onSearch = onSearch,
            onClear = onClear,
        )

        if (suggestions.isNotEmpty()) {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    suggestions.forEachIndexed { index, suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            onClick = { onSuggestionClick(suggestion) }
                        )
                        if (index != suggestions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: DestinationSuggestion,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.primaryText,
                style = MaterialTheme.typography.bodyLarge
            )
            if (suggestion.secondaryText.isNotBlank()) {
                Text(
                    text = suggestion.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    onSearch: () -> Unit,
    onClear: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSearching) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.map_search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    onSearch()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.map_clear_search),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
    ) {
        Icon(imageVector = icon, contentDescription = description)
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

private fun SearchResult.toSuggestion(): DestinationSuggestion? {
    val primary = place.address?.freeformAddress
        ?.takeIf { it.isNotBlank() }
        ?: place.name.takeIf { it.isNotBlank() }
        ?: return null

    val secondary = listOfNotNull(
        place.name.takeIf { it.isNotBlank() && it != primary },
        place.address?.municipality?.takeIf { it.isNotBlank() },
        place.address?.countrySubdivision?.takeIf { it.isNotBlank() },
    ).distinct().joinToString(", ")

    if (primary.isBlank()) {
        return null
    }

    return DestinationSuggestion(
        primaryText = primary,
        secondaryText = secondary,
        coordinate = place.coordinate,
    )
}
