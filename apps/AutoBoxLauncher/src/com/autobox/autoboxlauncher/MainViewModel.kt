package com.autobox.autoboxlauncher

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToInt
import com.autobox.autoboxlauncher.media.BrowseItem
import com.autobox.autoboxlauncher.media.BrowseState
import com.autobox.autoboxlauncher.media.MediaRepository
import com.autobox.autoboxlauncher.media.MediaState
import com.autobox.autoboxlauncher.signs.CameraCapture
import com.autobox.autoboxlauncher.signs.SignsRepository
import com.autobox.autoboxlauncher.speed.SpeedRepository
import com.tomtom.sdk.init.TomTomSdk
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.navigation.ActiveRouteChangedListener
import com.tomtom.sdk.navigation.GuidanceUpdatedListener
import com.tomtom.sdk.navigation.NavigationOptions
import com.tomtom.sdk.navigation.NavigationState
import com.tomtom.sdk.navigation.NavigationStateChangedListener
import com.tomtom.sdk.navigation.ProgressUpdatedListener
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.guidance.GuidanceAnnouncement
import com.tomtom.sdk.navigation.guidance.InstructionPhase
import com.tomtom.sdk.navigation.guidance.instruction.GuidanceInstruction
import com.tomtom.sdk.navigation.progress.RouteProgress
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.route.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTheme {
    LIGHT, DARK, AUTO
}

enum class UnitSystem {
    METRIC, IMPERIAL
}

data class MediaAppInfo(
    val name: String,
    val packageName: String,
    val serviceClassName: String,  // full class name of the MediaBrowserService
)

data class NavigationUiState(
    val navigationState: NavigationState = NavigationState.Idle,
    val plannedRoute: Route? = null,
    val plannedRouteOptions: RoutePlanningOptions? = null,
    val destinationGeoPoint: GeoPoint? = null,
    val routeProgress: RouteProgress? = null,
    val guidanceMessage: String? = null,
    val navigationMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _theme = mutableStateOf(AppTheme.AUTO)
    val theme: State<AppTheme> = _theme

    private val _unitSystem = mutableStateOf(UnitSystem.METRIC)
    val unitSystem: State<UnitSystem> = _unitSystem

    private val _selectedMediaApp = mutableStateOf<MediaAppInfo?>(null)
    val selectedMediaApp: State<MediaAppInfo?> = _selectedMediaApp
    // Convenience accessor used by legacy call sites.
    val selectedMediaPackage: State<String?> get() = mutableStateOf(_selectedMediaApp.value?.packageName)

    private val _installedMediaApps = mutableStateOf<List<MediaAppInfo>>(emptyList())
    val installedMediaApps: State<List<MediaAppInfo>> = _installedMediaApps

    private val _brightness = mutableStateOf(0.8f)
    val brightness: State<Float> = _brightness

    private val audioManager =
        application.getSystemService(AudioManager::class.java)
    private val _volume = mutableStateOf(run {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max > 0) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        else 0.5f
    })
    val volume: State<Float> = _volume

    private val _signRecognitionEnabled = mutableStateOf(true)
    val signRecognitionEnabled: State<Boolean> = _signRecognitionEnabled

    private val appContext = application.applicationContext
    private val mediaRepository = MediaRepository(application)
    private val speedRepository = SpeedRepository(application)
    private val signsRepository = SignsRepository(appContext, viewModelScope)
    private val cameraCapture = CameraCapture(application.applicationContext) { bitmap ->
        signsRepository.recognizeSigns(bitmap)
    }
    private val navigation = TomTomSdk.navigation

    private val _navigationUiState = mutableStateOf(
        NavigationUiState(navigationState = navigation.navigationState)
    )
    val navigationUiState: State<NavigationUiState> = _navigationUiState

    private val progressListener = ProgressUpdatedListener { progress ->
        _navigationUiState.value = _navigationUiState.value.copy(routeProgress = progress)
    }

    private val guidanceListener = object : GuidanceUpdatedListener {
        override fun onInstructionsChanged(instructions: List<GuidanceInstruction>) = Unit

        override fun onAnnouncementGenerated(
            announcement: GuidanceAnnouncement,
            shouldPlay: Boolean,
        ) {
            _navigationUiState.value = _navigationUiState.value.copy(
                guidanceMessage = announcement.plainTextMessage
            )
        }

        override fun onDistanceToNextInstructionChanged(
            distance: com.tomtom.quantity.Distance,
            instructions: List<GuidanceInstruction>,
            currentPhase: InstructionPhase,
        ) {
            if (_navigationUiState.value.guidanceMessage.isNullOrBlank()) {
                _navigationUiState.value = _navigationUiState.value.copy(
                    guidanceMessage = instructions.firstOrNull()?.javaClass?.simpleName
                        ?.removeSuffix("GuidanceInstruction")
                        ?.replace(Regex("([a-z])([A-Z])"), "$1 $2")
                )
            }
        }
    }

    private val navigationStateListener = NavigationStateChangedListener { state ->
        _navigationUiState.value = _navigationUiState.value.copy(
            navigationState = state,
            routeProgress = if (state == NavigationState.Idle) null else _navigationUiState.value.routeProgress,
            guidanceMessage = if (state == NavigationState.Idle) null else _navigationUiState.value.guidanceMessage,
        )
    }

    private val activeRouteListener = ActiveRouteChangedListener { route ->
        _navigationUiState.value = _navigationUiState.value.copy(plannedRoute = route)
    }

    val mediaState: StateFlow<MediaState> = mediaRepository.mediaState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaState())

    val browseState: StateFlow<BrowseState> = mediaRepository.browseState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseState())

    val speedKmh: StateFlow<Int> = speedRepository.speedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 84)

    private val speedLimitSignName: StateFlow<String> = signsRepository.speedLimitSign

    private val otherSignName: StateFlow<String> = signsRepository.otherSign

    val speedLimitSignResId: StateFlow<Int> = speedLimitSignName
        .map { signNameToResId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val currentSpeedLimit: StateFlow<Int?> = speedLimitSignName
        .map { parseSpeedLimitValue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val otherSignResId: StateFlow<Int> = otherSignName
        .map { signNameToResId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun signNameToResId(name: String): Int =
        if (name.isEmpty()) 0
        else appContext.resources.getIdentifier(name, "drawable", appContext.packageName)

    private fun parseSpeedLimitValue(signName: String): Int? =
        Regex("^p1_speed_limit_(\\d+)$").find(signName)?.groupValues?.get(1)?.toIntOrNull()

    init {
        loadInstalledMediaApps()
        viewModelScope.launch {
            val prefs = application.settingsDataStore.data.first()
            _theme.value = when (prefs[PreferencesKeys.THEME]) {
                AppTheme.LIGHT.name -> AppTheme.LIGHT
                AppTheme.DARK.name -> AppTheme.DARK
                else -> AppTheme.AUTO
            }
            _unitSystem.value = when (prefs[PreferencesKeys.UNIT_SYSTEM]) {
                UnitSystem.IMPERIAL.name -> UnitSystem.IMPERIAL
                else -> UnitSystem.METRIC
            }
            _signRecognitionEnabled.value = prefs[PreferencesKeys.SIGN_RECOGNITION_ENABLED] ?: true
            if (_signRecognitionEnabled.value) cameraCapture.start()

            prefs[PreferencesKeys.BRIGHTNESS]?.let { saved ->
                _brightness.value = saved
                applyBrightness(saved)
            }
            prefs[PreferencesKeys.MEDIA_APP_PACKAGE]?.let { pkg ->
                val app = _installedMediaApps.value.firstOrNull { it.packageName == pkg }
                if (app != null) setMediaApp(app)
            }
        }
        navigation.addProgressUpdatedListener(progressListener)
        navigation.addGuidanceUpdatedListener(guidanceListener)
        navigation.addNavigationStateChangedListener(navigationStateListener)
        navigation.addActiveRouteChangedListener(activeRouteListener)
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[PreferencesKeys.THEME] = theme.name
            }
        }
    }

    fun setSignRecognitionEnabled(enabled: Boolean) {
        _signRecognitionEnabled.value = enabled
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[PreferencesKeys.SIGN_RECOGNITION_ENABLED] = enabled
            }
        }
        if (enabled) cameraCapture.start() else cameraCapture.stop()
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        _unitSystem.value = unitSystem
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[PreferencesKeys.UNIT_SYSTEM] = unitSystem.name
            }
        }
    }

    fun setMediaApp(app: MediaAppInfo?) {
        _selectedMediaApp.value = app
        if (app != null) {
            val component = ComponentName(app.packageName, app.serviceClassName)
            mediaRepository.connect(component)
        } else {
            mediaRepository.disconnect()
        }
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                if (app != null) prefs[PreferencesKeys.MEDIA_APP_PACKAGE] = app.packageName
                else prefs.remove(PreferencesKeys.MEDIA_APP_PACKAGE)
            }
        }
    }

    // Legacy helper used by MainActivity.detectMediaApp()
    fun setMediaPackage(packageName: String?) {
        val app = _installedMediaApps.value.firstOrNull { it.packageName == packageName }
        setMediaApp(app)
    }

    fun setBrightness(value: Float) {
        _brightness.value = value
        applyBrightness(value)
        viewModelScope.launch {
            getApplication<Application>().settingsDataStore.edit { prefs ->
                prefs[PreferencesKeys.BRIGHTNESS] = value
            }
        }
    }

    private fun applyBrightness(value: Float) {
        val resolver = getApplication<Application>().contentResolver
        try {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            Settings.System.putFloat(
                resolver,
                "screen_brightness_float",
                value.coerceIn(0f, 1f),
            )
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (value * 255).coerceIn(0f, 255f).toInt(),
            )
            Log.d("AutoBox.Settings", "Brightness applied via Settings.System: $value")
        } catch (e: SecurityException) {
            Log.w("AutoBox.Settings", "WRITE_SETTINGS denied — window-level still applies", e)
        }
    }

    fun setVolume(value: Float) {
        _volume.value = value
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).roundToInt(), 0)
    }

    fun loadInstalledMediaApps() {
        val pm = getApplication<Application>().packageManager
        val intent = Intent("android.media.browse.MediaBrowserService")
        val services = pm.queryIntentServices(intent, 0)
        _installedMediaApps.value = services
            .filter { info ->
                // Exclude Bluetooth media sources (they appear as MediaBrowserService
                // but are not standalone music apps the user can interact with).
                val pkg = info.serviceInfo.packageName
                !pkg.contains("bluetooth", ignoreCase = true)
            }
            .map { info ->
                MediaAppInfo(
                    name = info.loadLabel(pm).toString(),
                    packageName = info.serviceInfo.packageName,
                    serviceClassName = info.serviceInfo.name,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name }
    }

    fun attachMedia() {
        // Reconnect if we have a selected app but lost the connection.
        val app = _selectedMediaApp.value ?: return
        if (mediaRepository.browseState.value.isConnected) return
        mediaRepository.connect(ComponentName(app.packageName, app.serviceClassName))
    }
    fun detachMedia() { /* keep browser alive across activity pauses */ }

    fun mediaPlay()              = mediaRepository.play()
    fun mediaPause()             = mediaRepository.pause()
    fun mediaNext()              = mediaRepository.next()
    fun mediaPrevious()          = mediaRepository.previous()
    fun mediaTogglePlayPause()   = mediaRepository.togglePlayPause()
    fun mediaBrowseItem(item: BrowseItem) = mediaRepository.openItem(item)
    fun mediaBrowseBack()        = mediaRepository.goBack()
    fun mediaStartPlayback()     = _selectedMediaApp.value?.packageName?.let {
        mediaRepository.startPlayback(it)
    }

    fun setPlannedRoute(
        route: Route,
        routeOptions: RoutePlanningOptions,
        destinationGeoPoint: GeoPoint,
    ) {
        _navigationUiState.value = _navigationUiState.value.copy(
            plannedRoute = route,
            plannedRouteOptions = routeOptions,
            destinationGeoPoint = destinationGeoPoint,
            navigationMessage = null,
        )
    }

    fun setNavigationMessage(message: String?) {
        _navigationUiState.value = _navigationUiState.value.copy(navigationMessage = message)
    }

    fun clearNavigationUi() {
        _navigationUiState.value = _navigationUiState.value.copy(
            plannedRoute = null,
            plannedRouteOptions = null,
            destinationGeoPoint = null,
            routeProgress = null,
            guidanceMessage = null,
            navigationMessage = null,
        )
    }

    fun startNavigation() {
        val route = _navigationUiState.value.plannedRoute ?: return
        val routeOptions = _navigationUiState.value.plannedRouteOptions ?: return

        try {
            if (navigation.navigationState != NavigationState.Idle) {
                navigation.stop()
            }
            navigation.start(
                NavigationOptions(
                    activeRoutePlan = RoutePlan(
                        route = route,
                        routePlanningOptions = routeOptions,
                    )
                )
            )
            setNavigationMessage(null)
        } catch (e: Exception) {
            setNavigationMessage(e.message ?: "Navigation failed to start")
        }
    }

    fun stopNavigation() {
        runCatching { navigation.stop() }
        _navigationUiState.value = _navigationUiState.value.copy(
            routeProgress = null,
            guidanceMessage = null,
            navigationMessage = null,
        )
    }

    override fun onCleared() {
        super.onCleared()
        cameraCapture.release()
        signsRepository.close()
        mediaRepository.disconnect()
        navigation.removeProgressUpdatedListener(progressListener)
        navigation.removeGuidanceUpdatedListener(guidanceListener)
        navigation.removeNavigationStateChangedListener(navigationStateListener)
        navigation.removeActiveRouteChangedListener(activeRouteListener)
    }
}
