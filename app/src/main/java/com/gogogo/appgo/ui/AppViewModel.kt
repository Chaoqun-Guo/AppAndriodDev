package com.gogogo.appgo.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gogogo.appgo.data.WorkoutRepository
import com.gogogo.appgo.model.BackupCloudProvider
import com.gogogo.appgo.model.BackupConfig
import com.gogogo.appgo.model.BackupStrategy
import com.gogogo.appgo.model.BackupStrategyTemplate
import com.gogogo.appgo.model.CloudBackupConfig
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.MarkerPoint
import com.gogogo.appgo.model.AppSettings
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.RecordingMetric
import com.gogogo.appgo.model.ServiceIntegrationConfig
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.TrackPoint
import com.gogogo.appgo.model.UserProfile
import com.gogogo.appgo.model.WorkoutRecord
import com.gogogo.appgo.model.WorkoutSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

data class AppUiState(
    val selectedType: ExerciseType = ExerciseType.HIKING,
    val recordingStatus: RecordingStatus = RecordingStatus.IDLE,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val averagePaceSecondsPerKm: Int = 0,
    val currentAltitude: Double = 15.0,
    val totalElevationGain: Double = 0.0,
    val currentHeartRate: Int? = null,
    val currentSpeedMps: Double = 0.0,
    val lowSpeedSeconds: Int = 0,
    val movementDetected: Boolean = false,
    val gyroRadPerSec: Float = 0f,
    val hasLocationFix: Boolean = false,
    val locationAccuracyMeters: Float? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val markerPoints: List<MarkerPoint> = emptyList(),
    val history: List<WorkoutSummary> = emptyList(),
    val filteredHistory: List<WorkoutSummary> = emptyList(),
    val selectedWorkoutId: Long? = null,
    val selectedWorkoutTrack: List<TrackPoint> = emptyList(),
    val lastWorkout: WorkoutSummary? = null,
    val weeklyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val monthlyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val yearlyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val weekAnchorDate: LocalDate = LocalDate.now(),
    val monthAnchor: YearMonth = YearMonth.now(),
    val yearAnchor: Int = LocalDate.now().year,
    val historyFilterDate: LocalDate = LocalDate.now(),
    val summaryDialogVisible: Boolean = false,
    val pendingRecord: WorkoutRecord? = null,
    val profile: UserProfile = UserProfile(),
    val bluetoothConnected: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val backupInProgress: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("appgo_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var startTimeMillis: Long = 0
    private var tickerJob: Job? = null
    private var latestLocation: Location? = null
    private var latestGyroMagnitude = 0f
    private var lastLocationMillis = 0L
    private var lastGyroMillis = 0L
    private var fallbackLat = 31.2304
    private var fallbackLon = 121.4737

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latestLocation = location
            lastLocationMillis = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    hasLocationFix = true,
                    locationAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                )
            }
        }

        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
            val x = event.values.getOrNull(0) ?: 0f
            val y = event.values.getOrNull(1) ?: 0f
            val z = event.values.getOrNull(2) ?: 0f
            latestGyroMagnitude = sqrt(x * x + y * y + z * z)
            lastGyroMillis = System.currentTimeMillis()
            _uiState.update { it.copy(gyroRadPerSec = latestGyroMagnitude) }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        loadSettings()
        refreshFromDatabase()
    }

    fun selectExerciseType(type: ExerciseType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun startWorkout() {
        if (_uiState.value.recordingStatus == RecordingStatus.RECORDING) return

        startTimeMillis = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                recordingStatus = RecordingStatus.RECORDING,
                elapsedSeconds = 0,
                distanceMeters = 0.0,
                averagePaceSecondsPerKm = 0,
                currentAltitude = 16.0,
                totalElevationGain = 0.0,
                currentHeartRate = 96,
                currentSpeedMps = 0.0,
                lowSpeedSeconds = 0,
                movementDetected = false,
                trackPoints = emptyList(),
                markerPoints = emptyList(),
                summaryDialogVisible = false,
                pendingRecord = null,
            )
        }
        startInputs()
        startTicker()
    }

    fun togglePause() {
        _uiState.update { state ->
            when (state.recordingStatus) {
                RecordingStatus.RECORDING -> state.copy(recordingStatus = RecordingStatus.PAUSED)
                RecordingStatus.PAUSED, RecordingStatus.AUTO_PAUSED -> state.copy(
                    recordingStatus = RecordingStatus.RECORDING,
                    lowSpeedSeconds = 0,
                )
                else -> state
            }
        }
    }

    fun endWorkout() {
        stopTickerAndInputs()
        val now = System.currentTimeMillis()
        val state = _uiState.value
        if (state.elapsedSeconds < 3 || state.trackPoints.size < 2) {
            _uiState.update {
                it.copy(
                    recordingStatus = RecordingStatus.IDLE,
                    summaryDialogVisible = false,
                    pendingRecord = null,
                )
            }
            return
        }

        val averageHeartRate = state.trackPoints.mapNotNull { it.heartRate }.average()
            .takeIf { !it.isNaN() }
            ?.roundToInt()

        val pending = WorkoutRecord(
            type = state.selectedType,
            startTimeMillis = startTimeMillis,
            endTimeMillis = now,
            totalDistanceMeters = state.distanceMeters,
            totalElevationGainMeters = state.totalElevationGain,
            averageHeartRate = averageHeartRate,
            trackFilePath = "track_${now}.json",
        )

        _uiState.update {
            it.copy(
                recordingStatus = RecordingStatus.FINISHED,
                summaryDialogVisible = true,
                pendingRecord = pending,
            )
        }
    }

    fun saveWorkout() {
        val state = _uiState.value
        val record = state.pendingRecord ?: return
        repository.saveWorkout(record, state.trackPoints, state.markerPoints)
        _uiState.update {
            it.copy(
                recordingStatus = RecordingStatus.IDLE,
                summaryDialogVisible = false,
                pendingRecord = null,
            )
        }
        refreshFromDatabase()
    }

    fun discardWorkout() {
        stopTickerAndInputs()
        _uiState.update {
            it.copy(
                recordingStatus = RecordingStatus.IDLE,
                summaryDialogVisible = false,
                pendingRecord = null,
                trackPoints = emptyList(),
                markerPoints = emptyList(),
            )
        }
    }

    fun addMarker(type: String = "营地", note: String = "手动标记") {
        val state = _uiState.value
        val lastPoint = state.trackPoints.lastOrNull() ?: return
        val marker = MarkerPoint(
            workoutId = 0,
            type = type,
            latitude = lastPoint.latitude,
            longitude = lastPoint.longitude,
            photoPath = null,
            note = note,
        )
        _uiState.update { it.copy(markerPoints = it.markerPoints + marker) }
    }

    fun openWorkoutDetail(workoutId: Long) {
        _uiState.update {
            it.copy(
                selectedWorkoutId = workoutId,
                selectedWorkoutTrack = repository.loadTrackPoints(workoutId),
            )
        }
    }

    fun shiftWeek(step: Long) {
        _uiState.update { it.copy(weekAnchorDate = it.weekAnchorDate.plusDays(7 * step)) }
        refreshDerivedStatsAndFilters()
    }

    fun setWeekAnchorDate(date: LocalDate) {
        _uiState.update { it.copy(weekAnchorDate = date) }
        refreshDerivedStatsAndFilters()
    }

    fun shiftMonth(step: Long) {
        _uiState.update { it.copy(monthAnchor = it.monthAnchor.plusMonths(step)) }
        refreshDerivedStatsAndFilters()
    }

    fun setMonthAnchor(month: YearMonth) {
        _uiState.update { it.copy(monthAnchor = month) }
        refreshDerivedStatsAndFilters()
    }

    fun shiftYear(step: Long) {
        _uiState.update { it.copy(yearAnchor = it.yearAnchor + step.toInt()) }
        refreshDerivedStatsAndFilters()
    }

    fun setYearAnchor(year: Int) {
        _uiState.update { it.copy(yearAnchor = year) }
        refreshDerivedStatsAndFilters()
    }

    fun shiftHistoryDate(step: Long) {
        _uiState.update { it.copy(historyFilterDate = it.historyFilterDate.plusDays(step)) }
        refreshDerivedStatsAndFilters()
    }

    fun setHistoryFilterDate(date: LocalDate) {
        _uiState.update { it.copy(historyFilterDate = date) }
        refreshDerivedStatsAndFilters()
    }

    fun updateProfile(profile: UserProfile) {
        _uiState.update { it.copy(profile = profile) }
    }

    fun toggleBluetoothConnection() {
        _uiState.update { it.copy(bluetoothConnected = !it.bluetoothConnected) }
    }

    fun setMetricVisible(metric: RecordingMetric, visible: Boolean) {
        _uiState.update { state ->
            val next = state.settings.visibleMetrics.toMutableSet()
            if (visible) {
                next.add(metric)
            } else {
                if (next.size == 1 && next.contains(metric)) return@update state
                next.remove(metric)
            }
            state.copy(settings = state.settings.copy(visibleMetrics = next))
        }
        persistSettings()
    }

    fun updateMapApiKey(key: String) {
        updateServiceSettings { it.copy(mapApiKey = key, mapEnabled = it.mapEnabled && key.isNotBlank()) }
    }

    fun setMapEnabled(enabled: Boolean) {
        updateServiceSettings {
            it.copy(mapEnabled = enabled && it.mapApiKey.isNotBlank())
        }
    }

    fun updateWeatherApiKey(key: String) {
        updateServiceSettings { it.copy(weatherApiKey = key, weatherEnabled = it.weatherEnabled && key.isNotBlank()) }
    }

    fun setWeatherEnabled(enabled: Boolean) {
        updateServiceSettings {
            it.copy(weatherEnabled = enabled && it.weatherApiKey.isNotBlank())
        }
    }

    fun updateShareApiKey(key: String) {
        updateServiceSettings { it.copy(shareApiKey = key, shareEnabled = it.shareEnabled && key.isNotBlank()) }
    }

    fun setShareEnabled(enabled: Boolean) {
        updateServiceSettings {
            it.copy(shareEnabled = enabled && it.shareApiKey.isNotBlank())
        }
    }

    fun setLocalBackupEnabled(enabled: Boolean) {
        updateBackupConfig { it.copy(localBackupEnabled = enabled) }
    }

    fun setCloudProvider(provider: BackupCloudProvider) {
        updateBackupConfig {
            it.copy(
                cloudConfig = it.cloudConfig.copy(
                    provider = provider,
                    enabled = if (provider == BackupCloudProvider.NONE) false else it.cloudConfig.enabled,
                )
            )
        }
    }

    fun setCloudBackupEnabled(enabled: Boolean) {
        updateBackupConfig {
            it.copy(
                cloudConfig = it.cloudConfig.copy(
                    enabled = enabled && it.cloudConfig.isConfigured,
                )
            )
        }
    }

    fun updateCloudApiKey(value: String) {
        updateBackupConfig {
            it.copy(
                cloudConfig = it.cloudConfig.copy(
                    apiKey = value,
                    enabled = it.cloudConfig.enabled && value.isNotBlank() && it.cloudConfig.bucketOrPath.isNotBlank(),
                )
            )
        }
    }

    fun updateCloudSecret(value: String) {
        updateBackupConfig {
            it.copy(cloudConfig = it.cloudConfig.copy(secret = value))
        }
    }

    fun updateCloudBucketOrPath(value: String) {
        updateBackupConfig {
            it.copy(
                cloudConfig = it.cloudConfig.copy(
                    bucketOrPath = value,
                    enabled = it.cloudConfig.enabled && it.cloudConfig.apiKey.isNotBlank() && value.isNotBlank(),
                )
            )
        }
    }

    fun updateCloudEndpoint(value: String) {
        updateBackupConfig {
            it.copy(cloudConfig = it.cloudConfig.copy(endpoint = value))
        }
    }

    fun applyBackupTemplate(template: BackupStrategyTemplate) {
        updateBackupConfig {
            val strategy = if (template == BackupStrategyTemplate.CUSTOM) {
                it.strategy
            } else {
                it.strategy.copy(
                    intervalHours = template.intervalHours,
                    wifiOnly = template.wifiOnly,
                    chargingOnly = template.chargingOnly,
                    retainDays = template.retainDays,
                )
            }
            it.copy(strategyTemplate = template, strategy = strategy)
        }
    }

    fun setBackupAutoEnabled(enabled: Boolean) {
        updateBackupConfig {
            it.copy(strategy = it.strategy.copy(autoBackupEnabled = enabled))
        }
    }

    fun updateBackupIntervalHours(value: Int) {
        updateBackupConfig {
            it.copy(
                strategyTemplate = BackupStrategyTemplate.CUSTOM,
                strategy = it.strategy.copy(intervalHours = value.coerceIn(1, 168)),
            )
        }
    }

    fun updateBackupRetainDays(value: Int) {
        updateBackupConfig {
            it.copy(
                strategyTemplate = BackupStrategyTemplate.CUSTOM,
                strategy = it.strategy.copy(retainDays = value.coerceIn(1, 365)),
            )
        }
    }

    fun setBackupWifiOnly(enabled: Boolean) {
        updateBackupConfig {
            it.copy(
                strategyTemplate = BackupStrategyTemplate.CUSTOM,
                strategy = it.strategy.copy(wifiOnly = enabled),
            )
        }
    }

    fun setBackupChargingOnly(enabled: Boolean) {
        updateBackupConfig {
            it.copy(
                strategyTemplate = BackupStrategyTemplate.CUSTOM,
                strategy = it.strategy.copy(chargingOnly = enabled),
            )
        }
    }

    fun runManualBackup() {
        if (_uiState.value.backupInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(backupInProgress = true) }
            val now = System.currentTimeMillis()
            val backupConfig = _uiState.value.settings.backupConfig
            val history = repository.loadWorkoutSummaries()
            val targets = mutableListOf<String>()

            val backupJson = buildString {
                append("{\"backup_time_millis\":")
                append(now)
                append(",\"workout_count\":")
                append(history.size)
                append(",\"workouts\":[")
                append(history.joinToString(",") { summary ->
                    val record = summary.record
                    "{\"id\":${record.id},\"type\":\"${record.type.name}\",\"start\":${record.startTimeMillis},\"end\":${record.endTimeMillis},\"distance\":${record.totalDistanceMeters},\"elevation\":${record.totalElevationGainMeters},\"track_points\":${summary.trackPointCount}}"
                })
                append("]}")
            }

            var localPath: String? = null
            if (backupConfig.localBackupEnabled) {
                val folder = File(getApplication<Application>().filesDir, "backups")
                folder.mkdirs()
                val file = File(folder, "backup_${now}.json")
                file.writeText(backupJson)
                localPath = file.absolutePath
                targets += "本地"
                pruneLocalBackups(folder, backupConfig.strategy.retainDays)
            }

            if (backupConfig.cloudConfig.isActive) {
                val folder = File(getApplication<Application>().filesDir, "backups")
                folder.mkdirs()
                val task = File(folder, "cloud_task_${now}.json")
                val cloud = backupConfig.cloudConfig
                task.writeText(
                    """
                    {"provider":"${cloud.provider.name}","endpoint":"${cloud.endpoint}","bucket_or_path":"${cloud.bucketOrPath}","source_file":"${localPath ?: ""}","created_at":$now}
                    """.trimIndent()
                )
                targets += "云端(${cloud.provider.label})"
            }

            val result = if (targets.isEmpty()) {
                "备份失败：未启用本地或云端备份目标。"
            } else {
                "备份完成：${targets.joinToString("、")}"
            }

            updateBackupConfig {
                it.copy(
                    lastBackupTimeMillis = now,
                    lastBackupResult = result,
                )
            }
            _uiState.update { it.copy(backupInProgress = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTickerAndInputs()
    }

    private fun refreshFromDatabase() {
        val history = repository.loadWorkoutSummaries()
        _uiState.update {
            it.copy(
                history = history,
                lastWorkout = history.firstOrNull(),
                selectedWorkoutId = history.firstOrNull()?.record?.id,
                selectedWorkoutTrack = history.firstOrNull()?.record?.id?.let(repository::loadTrackPoints)
                    ?: emptyList(),
            )
        }
        refreshDerivedStatsAndFilters()
    }

    private fun refreshDerivedStatsAndFilters() {
        val state = _uiState.value
        val history = state.history

        val weekStart = state.weekAnchorDate.minusDays(state.weekAnchorDate.dayOfWeek.value.toLong() - 1)
        val weekEnd = weekStart.plusDays(6)
        val monthStart = state.monthAnchor.atDay(1)
        val monthEnd = state.monthAnchor.atEndOfMonth()
        val yearStart = LocalDate.of(state.yearAnchor, 1, 1)
        val yearEnd = LocalDate.of(state.yearAnchor, 12, 31)

        _uiState.update {
            it.copy(
                weeklyStats = statsInRange(history, weekStart, weekEnd),
                monthlyStats = statsInRange(history, monthStart, monthEnd),
                yearlyStats = statsInRange(history, yearStart, yearEnd),
                filteredHistory = history.filter { summary ->
                    toLocalDate(summary.record.startTimeMillis) == it.historyFilterDate
                },
            )
        }
    }

    private fun updateBackupConfig(transform: (BackupConfig) -> BackupConfig) {
        _uiState.update { state ->
            val changed = transform(state.settings.backupConfig)
            val normalizedCloud = changed.cloudConfig.copy(
                enabled = changed.cloudConfig.enabled && changed.cloudConfig.isConfigured,
            )
            val normalized = changed.copy(cloudConfig = normalizedCloud)
            state.copy(settings = state.settings.copy(backupConfig = normalized))
        }
        persistSettings()
    }

    private fun updateServiceSettings(transform: (ServiceIntegrationConfig) -> ServiceIntegrationConfig) {
        _uiState.update { state ->
            val current = state.settings.serviceIntegration
            val changed = transform(current)
            val updated = changed.copy(
                mapEnabled = changed.mapEnabled && changed.mapApiKey.isNotBlank(),
                weatherEnabled = changed.weatherEnabled && changed.weatherApiKey.isNotBlank(),
                shareEnabled = changed.shareEnabled && changed.shareApiKey.isNotBlank(),
            )
            state.copy(settings = state.settings.copy(serviceIntegration = updated))
        }
        persistSettings()
    }

    private fun loadSettings() {
        val metricsRaw = prefs.getString("visible_metrics", null)
        val metrics = metricsRaw
            ?.split(",")
            ?.mapNotNull { name -> RecordingMetric.entries.find { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: AppSettings().visibleMetrics

        val mapApiKey = prefs.getString("map_api_key", "") ?: ""
        val mapEnabled = prefs.getBoolean("map_enabled", false) && mapApiKey.isNotBlank()
        val weatherApiKey = prefs.getString("weather_api_key", "") ?: ""
        val weatherEnabled = prefs.getBoolean("weather_enabled", false) && weatherApiKey.isNotBlank()
        val shareApiKey = prefs.getString("share_api_key", "") ?: ""
        val shareEnabled = prefs.getBoolean("share_enabled", false) && shareApiKey.isNotBlank()

        val localBackupEnabled = prefs.getBoolean("backup_local_enabled", true)
        val cloudProvider = runCatching {
            BackupCloudProvider.valueOf(
                prefs.getString("backup_cloud_provider", BackupCloudProvider.NONE.name)
                    ?: BackupCloudProvider.NONE.name
            )
        }.getOrElse { BackupCloudProvider.NONE }
        val cloudApiKey = prefs.getString("backup_cloud_api_key", "") ?: ""
        val cloudSecret = prefs.getString("backup_cloud_secret", "") ?: ""
        val cloudBucketOrPath = prefs.getString("backup_cloud_bucket_path", "") ?: ""
        val cloudEndpoint = prefs.getString("backup_cloud_endpoint", "") ?: ""
        val cloudEnabledRaw = prefs.getBoolean("backup_cloud_enabled", false)
        val strategyTemplate = runCatching {
            BackupStrategyTemplate.valueOf(
                prefs.getString("backup_template", BackupStrategyTemplate.SAFE_DAILY.name)
                    ?: BackupStrategyTemplate.SAFE_DAILY.name
            )
        }.getOrElse { BackupStrategyTemplate.SAFE_DAILY }
        val strategy = BackupStrategy(
            autoBackupEnabled = prefs.getBoolean("backup_auto_enabled", false),
            intervalHours = prefs.getInt("backup_interval_hours", strategyTemplate.intervalHours).coerceIn(1, 168),
            wifiOnly = prefs.getBoolean("backup_wifi_only", strategyTemplate.wifiOnly),
            chargingOnly = prefs.getBoolean("backup_charging_only", strategyTemplate.chargingOnly),
            retainDays = prefs.getInt("backup_retain_days", strategyTemplate.retainDays).coerceIn(1, 365),
        )
        val cloudConfig = CloudBackupConfig(
            provider = cloudProvider,
            enabled = cloudEnabledRaw,
            apiKey = cloudApiKey,
            secret = cloudSecret,
            bucketOrPath = cloudBucketOrPath,
            endpoint = cloudEndpoint,
        )
        val lastBackupTimeMillis = prefs.getLong("backup_last_time", -1L).takeIf { it > 0 }
        val lastBackupResult = prefs.getString("backup_last_result", "") ?: ""

        _uiState.update {
            it.copy(
                settings = AppSettings(
                    visibleMetrics = metrics,
                    serviceIntegration = ServiceIntegrationConfig(
                        mapApiKey = mapApiKey,
                        mapEnabled = mapEnabled,
                        weatherApiKey = weatherApiKey,
                        weatherEnabled = weatherEnabled,
                        shareApiKey = shareApiKey,
                        shareEnabled = shareEnabled,
                    ),
                    backupConfig = BackupConfig(
                        localBackupEnabled = localBackupEnabled,
                        cloudConfig = cloudConfig.copy(enabled = cloudConfig.enabled && cloudConfig.isConfigured),
                        strategyTemplate = strategyTemplate,
                        strategy = strategy,
                        lastBackupTimeMillis = lastBackupTimeMillis,
                        lastBackupResult = lastBackupResult,
                    ),
                ),
            )
        }
    }

    private fun persistSettings() {
        val settings = _uiState.value.settings
        prefs.edit()
            .putString("visible_metrics", settings.visibleMetrics.joinToString(",") { it.name })
            .putString("map_api_key", settings.serviceIntegration.mapApiKey)
            .putBoolean("map_enabled", settings.serviceIntegration.mapEnabled)
            .putString("weather_api_key", settings.serviceIntegration.weatherApiKey)
            .putBoolean("weather_enabled", settings.serviceIntegration.weatherEnabled)
            .putString("share_api_key", settings.serviceIntegration.shareApiKey)
            .putBoolean("share_enabled", settings.serviceIntegration.shareEnabled)
            .putBoolean("backup_local_enabled", settings.backupConfig.localBackupEnabled)
            .putString("backup_cloud_provider", settings.backupConfig.cloudConfig.provider.name)
            .putBoolean("backup_cloud_enabled", settings.backupConfig.cloudConfig.enabled)
            .putString("backup_cloud_api_key", settings.backupConfig.cloudConfig.apiKey)
            .putString("backup_cloud_secret", settings.backupConfig.cloudConfig.secret)
            .putString("backup_cloud_bucket_path", settings.backupConfig.cloudConfig.bucketOrPath)
            .putString("backup_cloud_endpoint", settings.backupConfig.cloudConfig.endpoint)
            .putString("backup_template", settings.backupConfig.strategyTemplate.name)
            .putBoolean("backup_auto_enabled", settings.backupConfig.strategy.autoBackupEnabled)
            .putInt("backup_interval_hours", settings.backupConfig.strategy.intervalHours)
            .putBoolean("backup_wifi_only", settings.backupConfig.strategy.wifiOnly)
            .putBoolean("backup_charging_only", settings.backupConfig.strategy.chargingOnly)
            .putInt("backup_retain_days", settings.backupConfig.strategy.retainDays)
            .putLong("backup_last_time", settings.backupConfig.lastBackupTimeMillis ?: -1L)
            .putString("backup_last_result", settings.backupConfig.lastBackupResult)
            .apply()
    }

    private fun pruneLocalBackups(folder: File, retainDays: Int) {
        val threshold = System.currentTimeMillis() - retainDays * 24L * 3600L * 1000L
        folder.listFiles()
            ?.filter { it.name.startsWith("backup_") && it.name.endsWith(".json") }
            ?.filter { it.lastModified() < threshold }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun startInputs() {
        if (gyroscope != null) {
            sensorManager.registerListener(gyroListener, gyroscope, SensorManager.SENSOR_DELAY_UI)
        }
        if (!hasLocationPermission()) return

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    locationListener,
                    Looper.getMainLooper(),
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1500L,
                    0f,
                    locationListener,
                    Looper.getMainLooper(),
                )
            }
        } catch (_: SecurityException) {
            _uiState.update { it.copy(hasLocationFix = false, locationAccuracyMeters = null) }
        }
    }

    private fun stopTickerAndInputs() {
        tickerJob?.cancel()
        sensorManager.unregisterListener(gyroListener)
        runCatching { locationManager.removeUpdates(locationListener) }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value

                if (state.recordingStatus == RecordingStatus.PAUSED || state.recordingStatus == RecordingStatus.FINISHED) {
                    continue
                }

                val now = System.currentTimeMillis()
                val location = latestLocation
                val speedFromLocation = if (location != null && location.hasSpeed()) location.speed.toDouble() else 0.0
                val speedFromGyro = max(0.0, (latestGyroMagnitude - 0.06f) * 4.2).toDouble()
                val derivedSpeed = max(speedFromLocation, speedFromGyro)
                val locationMoving = speedFromLocation >= 0.7 && (now - lastLocationMillis) <= 8_000
                val gyroMoving = latestGyroMagnitude >= 0.12f && (now - lastGyroMillis) <= 3_000
                val movementDetected = locationMoving || gyroMoving

                val nextLowSpeed = if (!movementDetected) state.lowSpeedSeconds + 1 else 0
                val autoPause = nextLowSpeed >= 10

                val nextStatus = when {
                    autoPause -> RecordingStatus.AUTO_PAUSED
                    state.recordingStatus == RecordingStatus.AUTO_PAUSED && movementDetected -> RecordingStatus.RECORDING
                    state.recordingStatus == RecordingStatus.AUTO_PAUSED -> RecordingStatus.AUTO_PAUSED
                    else -> RecordingStatus.RECORDING
                }

                val recordingActive = nextStatus == RecordingStatus.RECORDING
                val elapsed = if (recordingActive) state.elapsedSeconds + 1 else state.elapsedSeconds

                val resolvedLat = location?.latitude ?: run {
                    fallbackLat += Random.nextDouble(-0.000005, 0.000005)
                    fallbackLat
                }
                val resolvedLon = location?.longitude ?: run {
                    fallbackLon += Random.nextDouble(-0.000005, 0.000005)
                    fallbackLon
                }
                val altitude = location?.altitude ?: state.currentAltitude + Random.nextDouble(-0.3, 0.6)
                val heartRate = (112 + (derivedSpeed * 8.0) + Random.nextDouble(-3.0, 3.0)).roundToInt()

                val nextDistance = if (recordingActive) state.distanceMeters + derivedSpeed else state.distanceMeters
                val nextAscent = if (recordingActive) {
                    state.totalElevationGain + max(0.0, altitude - state.currentAltitude)
                } else {
                    state.totalElevationGain
                }
                val pace = if (nextDistance > 1.0) {
                    ((elapsed / (nextDistance / 1000.0)).roundToInt())
                } else {
                    0
                }

                val nextPoints = if (recordingActive) {
                    state.trackPoints + TrackPoint(
                        workoutId = 0,
                        timestampMillis = now,
                        latitude = resolvedLat,
                        longitude = resolvedLon,
                        altitude = altitude,
                        heartRate = heartRate,
                        speedMps = derivedSpeed,
                    )
                } else {
                    state.trackPoints
                }

                _uiState.update {
                    it.copy(
                        recordingStatus = nextStatus,
                        elapsedSeconds = elapsed,
                        distanceMeters = nextDistance,
                        averagePaceSecondsPerKm = pace,
                        currentAltitude = altitude,
                        totalElevationGain = nextAscent,
                        currentHeartRate = heartRate,
                        currentSpeedMps = derivedSpeed,
                        lowSpeedSeconds = nextLowSpeed,
                        movementDetected = movementDetected,
                        trackPoints = nextPoints,
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun statsInRange(history: List<WorkoutSummary>, start: LocalDate, end: LocalDate): StatsSummary {
        val filtered = history.filter {
            val d = toLocalDate(it.record.startTimeMillis)
            !d.isBefore(start) && !d.isAfter(end)
        }
        val distance = filtered.sumOf { it.record.totalDistanceMeters }
        val durationSeconds = filtered.sumOf { it.record.duration.seconds }
        val elevation = filtered.sumOf { it.record.totalElevationGainMeters }
        return StatsSummary(
            totalDistanceMeters = distance,
            totalDurationSeconds = durationSeconds,
            totalElevationGainMeters = elevation,
            estimatedCalories = ((distance / 1000.0) * 55).toInt(),
        )
    }
}
