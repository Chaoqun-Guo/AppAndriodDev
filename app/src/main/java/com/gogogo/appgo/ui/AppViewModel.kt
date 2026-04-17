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
import android.hardware.GeomagneticField
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Patterns
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gogogo.appgo.data.WorkoutRepository
import com.gogogo.appgo.domain.history.HistoryDomain
import com.gogogo.appgo.domain.replay.ReplayDomain
import com.gogogo.appgo.domain.share.SmtpEmailSender
import com.gogogo.appgo.domain.weather.WeatherDomain
import com.gogogo.appgo.model.BackupCloudProvider
import com.gogogo.appgo.model.BackupConfig
import com.gogogo.appgo.model.BacktrackDirection
import com.gogogo.appgo.model.BacktrackRouteSummary
import com.gogogo.appgo.model.BackupStrategy
import com.gogogo.appgo.model.BackupStrategyTemplate
import com.gogogo.appgo.model.BreadcrumbPoint
import com.gogogo.appgo.model.CloudBackupConfig
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.HeartRateZoneStat
import com.gogogo.appgo.model.HistoryDisplayField
import com.gogogo.appgo.model.DetailReplayDirection
import com.gogogo.appgo.model.DetailReplayPoint
import com.gogogo.appgo.model.DetailReplaySpeed
import com.gogogo.appgo.model.MarkerPoint
import com.gogogo.appgo.model.AppSettings
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.RecordingMetric
import com.gogogo.appgo.model.SensorWorkStatus
import com.gogogo.appgo.model.SegmentPace
import com.gogogo.appgo.model.ServiceIntegrationConfig
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.TrackPoint
import com.gogogo.appgo.model.UserProfile
import com.gogogo.appgo.model.WeatherDailyForecast
import com.gogogo.appgo.model.WeatherSnapshot
import com.gogogo.appgo.model.WorkoutRecord
import com.gogogo.appgo.model.WorkoutSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.coroutines.resume

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
    val azimuthDegree: Float = 0f,
    val pitchDegree: Float = 0f,
    val rollDegree: Float = 0f,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val currentAddressText: String = "",
    val weather: WeatherSnapshot = WeatherSnapshot(),
    val weatherForecast3d: List<WeatherDailyForecast> = emptyList(),
    val hasLocationFix: Boolean = false,
    val locationAccuracyMeters: Float? = null,
    val gyroWorkStatus: SensorWorkStatus = SensorWorkStatus.BAD,
    val azimuthWorkStatus: SensorWorkStatus = SensorWorkStatus.BAD,
    val compassWorkStatus: SensorWorkStatus = SensorWorkStatus.BAD,
    val altitudeWorkStatus: SensorWorkStatus = SensorWorkStatus.BAD,
    val compassCalibrationMode: Boolean = false,
    val compassBacktrackMode: Boolean = false,
    val compassBacktrackTargetIndex: Int = -1,
    val compassBacktrackBearingDegree: Float? = null,
    val compassBacktrackDistanceMeters: Double? = null,
    val compassBacktrackDeviationMeters: Double? = null,
    val compassBacktrackDeviationStatus: SensorWorkStatus = SensorWorkStatus.WARN,
    val compassBacktrackPointerDeviationDegree: Float? = null,
    val compassBacktrackPointerDeviationStatus: SensorWorkStatus = SensorWorkStatus.WARN,
    val backtrackDirection: BacktrackDirection = BacktrackDirection.REVERSE,
    val startWithPathRecordingOnWorkout: Boolean = false,
    val workoutPathRecordingEnabled: Boolean = false,
    val backtrackRoutes: List<BacktrackRouteSummary> = emptyList(),
    val selectedBacktrackRouteId: Long? = null,
    val activeBacktrackNodeCount: Int = 0,
    val activeBacktrackRouteName: String = "实时轨迹",
    val activeBacktrackNodes: List<BreadcrumbPoint> = emptyList(),
    val isBacktrackRoutePersisting: Boolean = false,
    val trackPoints: List<TrackPoint> = emptyList(),
    val markerPoints: List<MarkerPoint> = emptyList(),
    val history: List<WorkoutSummary> = emptyList(),
    val filteredHistory: List<WorkoutSummary> = emptyList(),
    val historyVisibleCount: Int = 5,
    val historyTypeFilter: ExerciseType? = null,
    val selectedWorkoutId: Long? = null,
    val selectedWorkoutTrack: List<TrackPoint> = emptyList(),
    val selectedWorkoutReplayPoints: List<DetailReplayPoint> = emptyList(),
    val selectedWorkoutSummary: WorkoutSummary? = null,
    val selectedWorkoutSegmentPace: List<SegmentPace> = emptyList(),
    val selectedWorkoutHeartRateZones: List<HeartRateZoneStat> = emptyList(),
    val selectedWorkoutPaceSeries: List<Float> = emptyList(),
    val selectedWorkoutHeartRateSeries: List<Float> = emptyList(),
    val lastWorkout: WorkoutSummary? = null,
    val weeklyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val dailyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val monthlyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val yearlyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val customRangeStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val weeklyDistanceMeters: Double = 0.0,
    val weekAnchorDate: LocalDate = LocalDate.now(),
    val monthAnchor: YearMonth = YearMonth.now(),
    val yearAnchor: Int = LocalDate.now().year,
    val historyFilterDate: LocalDate = LocalDate.now(),
    val customRangeStartDate: LocalDate = LocalDate.now(),
    val customRangeEndDate: LocalDate = LocalDate.now(),
    val summaryDialogVisible: Boolean = false,
    val pendingRecord: WorkoutRecord? = null,
    val profile: UserProfile = UserProfile(),
    val bluetoothConnected: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val backupInProgress: Boolean = false,
    val backupFiles: List<String> = emptyList(),
    val selectedBackupFileName: String? = null,
    val selectedBackupFileContent: String = "",
    val breadcrumbTrail: List<BreadcrumbPoint> = emptyList(),
    val onlineSharingEnabled: Boolean = false,
    val shareEmailStatus: String = "",
    val detailReplayPanelVisible: Boolean = false,
    val detailReplayAnchorIndex: Int = -1,
    val detailReplayCurrentIndex: Int = -1,
    val detailReplayDirection: DetailReplayDirection = DetailReplayDirection.FORWARD,
    val detailReplayPlaying: Boolean = false,
    val detailReplaySpeed: DetailReplaySpeed = DetailReplaySpeed.X1,
    val detailReplayNotice: String = "",
    val detailReplayNoticeVersion: Long = 0L,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("appgo_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var startTimeMillis: Long = 0
    private var tickerJob: Job? = null
    private var detailReplayJob: Job? = null
    private var monitoringInputs = false
    private var latestLocation: Location? = null
    private var latestGyroMagnitude = 0f
    private var latestAzimuthDegrees = 0f
    private var latestAzimuthRawDegrees = 0f
    private var latestMagneticHeadingDegrees = 0f
    private var latestPitchDegrees = 0f
    private var latestRollDegrees = 0f
    private var lastLocationMillis = 0L
    private var lastGyroMillis = 0L
    private var lastAzimuthMillis = 0L
    private var lastCompassMillis = 0L
    private var lastAltitudeSensorMillis = 0L
    private var latestPressureHpa: Float? = null
    private var lastWeatherUpdateMillis = 0L
    private var lastAddressResolveMillis = 0L
    private var fallbackLat = 31.2304
    private var fallbackLon = 121.4737
    private val breadcrumbTrail = mutableListOf<BreadcrumbPoint>()
    private val activeBacktrackNodes = mutableListOf<BreadcrumbPoint>()
    private val routeNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latestLocation = location
            lastLocationMillis = System.currentTimeMillis()
            updateAzimuthDisplay()
            appendBreadcrumb(location)
            maybeResolveAddress(location)
            updateBacktrackFromLocation(location)
            _uiState.update {
                it.copy(
                    currentLatitude = location.latitude,
                    currentLongitude = location.longitude,
                    hasLocationFix = true,
                    locationAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                    breadcrumbTrail = breadcrumbTrail.toList(),
                    activeBacktrackNodes = activeBacktrackNodes.toList(),
                )
            }
            refreshWeatherSnapshot(location)
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

    private val rotationVectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            val rotation = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotation, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotation, orientation)
            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            latestAzimuthRawDegrees = azimuth
            latestPitchDegrees = pitch
            latestRollDegrees = roll
            lastAzimuthMillis = System.currentTimeMillis()
            updateAzimuthDisplay()
            _uiState.update {
                it.copy(
                    pitchDegree = pitch,
                    rollDegree = roll,
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val magneticListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
            val x = event.values.getOrNull(0) ?: 0f
            val y = event.values.getOrNull(1) ?: 0f
            val heading = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
            if (heading.isFinite()) {
                latestMagneticHeadingDegrees = normalizeDegrees(heading)
                lastCompassMillis = System.currentTimeMillis()
                updateAzimuthDisplay()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val pressureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_PRESSURE) return
            lastAltitudeSensorMillis = System.currentTimeMillis()
            val pressure = event.values.getOrNull(0) ?: return
            latestPressureHpa = pressure
            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure).toDouble()
            _uiState.update { it.copy(currentAltitude = altitude) }
            refreshWeatherSnapshot()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        loadSettings()
        loadProfile()
        refreshFromDatabase()
        refreshBacktrackRoutes()
        refreshBackupFiles()
        startInputs(forceRestartLocation = true)
        refreshWeatherSnapshot(force = true)
    }

    fun selectExerciseType(type: ExerciseType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun startWorkout(enablePathRecording: Boolean = _uiState.value.startWithPathRecordingOnWorkout) {
        if (_uiState.value.recordingStatus == RecordingStatus.RECORDING) return

        startTimeMillis = System.currentTimeMillis()
        if (enablePathRecording) {
            prepareBacktrackNodesForMode()
        }
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
                workoutPathRecordingEnabled = enablePathRecording,
                compassBacktrackMode = enablePathRecording && activeBacktrackNodes.size >= 2,
                activeBacktrackNodeCount = activeBacktrackNodes.size,
            )
        }
        if (enablePathRecording) {
            setNearestBacktrackTarget()
            updateBacktrackFromLocation(latestLocation)
        }
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

    fun pauseWorkout() {
        _uiState.update { state ->
            if (state.recordingStatus == RecordingStatus.RECORDING) {
                state.copy(recordingStatus = RecordingStatus.PAUSED)
            } else {
                state
            }
        }
    }

    fun resumeWorkout() {
        _uiState.update { state ->
            if (state.recordingStatus == RecordingStatus.PAUSED || state.recordingStatus == RecordingStatus.AUTO_PAUSED) {
                state.copy(recordingStatus = RecordingStatus.RECORDING, lowSpeedSeconds = 0)
            } else {
                state
            }
        }
    }

    fun endWorkout() {
        stopTickerOnly()
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
        val savedWorkoutId = repository.saveWorkout(record, state.trackPoints, state.markerPoints)
        val routeNodes = state.trackPoints.map {
            BreadcrumbPoint(
                timestampMillis = it.timestampMillis,
                latitude = it.latitude,
                longitude = it.longitude,
            )
        }
        if (state.workoutPathRecordingEnabled && routeNodes.size >= 2) {
            repository.saveBacktrackRoute(
                routeName = "回溯_运动_${savedWorkoutId}",
                sourceWorkoutId = savedWorkoutId,
                nodes = routeNodes,
            )
        }
        _uiState.update {
            it.copy(
                recordingStatus = RecordingStatus.IDLE,
                summaryDialogVisible = false,
                pendingRecord = null,
                workoutPathRecordingEnabled = false,
            )
        }
        refreshFromDatabase()
        refreshBacktrackRoutes()
    }

    fun discardWorkout() {
        stopTickerOnly()
        _uiState.update {
            it.copy(
                recordingStatus = RecordingStatus.IDLE,
                summaryDialogVisible = false,
                pendingRecord = null,
                trackPoints = emptyList(),
                markerPoints = emptyList(),
                workoutPathRecordingEnabled = false,
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
        val track = repository.loadTrackPoints(workoutId)
        val summary = _uiState.value.history.firstOrNull { it.record.id == workoutId }
        val analysis = analyzeWorkoutTrack(track)
        val replayPoints = ReplayDomain.buildReplayPoints(track)
        detailReplayJob?.cancel()
        _uiState.update {
            it.copy(
                selectedWorkoutId = workoutId,
                selectedWorkoutTrack = track,
                selectedWorkoutReplayPoints = replayPoints,
                selectedWorkoutSummary = summary,
                selectedWorkoutSegmentPace = analysis.segmentPace,
                selectedWorkoutHeartRateZones = analysis.heartRateZones,
                selectedWorkoutPaceSeries = analysis.paceSeries,
                selectedWorkoutHeartRateSeries = analysis.heartRateSeries,
                detailReplayPanelVisible = false,
                detailReplayAnchorIndex = -1,
                detailReplayCurrentIndex = -1,
                detailReplayDirection = DetailReplayDirection.FORWARD,
                detailReplayPlaying = false,
            )
        }
    }

    fun closeWorkoutDetail() {
        detailReplayJob?.cancel()
        _uiState.update {
            it.copy(
                selectedWorkoutId = null,
                selectedWorkoutTrack = emptyList(),
                selectedWorkoutReplayPoints = emptyList(),
                selectedWorkoutSummary = null,
                selectedWorkoutSegmentPace = emptyList(),
                selectedWorkoutHeartRateZones = emptyList(),
                selectedWorkoutPaceSeries = emptyList(),
                selectedWorkoutHeartRateSeries = emptyList(),
                detailReplayPanelVisible = false,
                detailReplayAnchorIndex = -1,
                detailReplayCurrentIndex = -1,
                detailReplayPlaying = false,
            )
        }
    }

    fun clearDetailReplayNotice() {
        _uiState.update { it.copy(detailReplayNotice = "") }
    }

    fun selectReplayAnchorByMapLongPress(latitude: Double, longitude: Double) {
        val points = _uiState.value.selectedWorkoutReplayPoints
        if (points.size < 2) {
            notifyDetailReplay("轨迹数据不足")
            return
        }
        val nearest = points.minByOrNull { p ->
            haversineMeters(latitude, longitude, p.latitude, p.longitude)
        } ?: return
        val nearestDistance = haversineMeters(latitude, longitude, nearest.latitude, nearest.longitude)
        if (nearestDistance > 30.0) {
            notifyDetailReplay("附近无轨迹点，请长按轨迹线附近")
            return
        }
        detailReplayJob?.cancel()
        _uiState.update {
            it.copy(
                detailReplayPanelVisible = true,
                detailReplayAnchorIndex = nearest.index,
                detailReplayCurrentIndex = nearest.index,
                detailReplayDirection = DetailReplayDirection.FORWARD,
                detailReplayPlaying = false,
            )
        }
    }

    fun startDetailReplayForward() {
        val state = _uiState.value
        val points = state.selectedWorkoutReplayPoints
        val anchor = state.detailReplayAnchorIndex
        if (points.size < 2 || anchor !in points.indices) {
            notifyDetailReplay("轨迹数据不足")
            return
        }
        if (anchor >= points.lastIndex) {
            notifyDetailReplay("已位于终点")
            return
        }
        _uiState.update {
            it.copy(
                detailReplayPanelVisible = true,
                detailReplayDirection = DetailReplayDirection.FORWARD,
                detailReplayCurrentIndex = anchor,
                detailReplayPlaying = true,
            )
        }
        startDetailReplayLoop()
    }

    fun startDetailReplayReverse() {
        val state = _uiState.value
        val points = state.selectedWorkoutReplayPoints
        val anchor = state.detailReplayAnchorIndex
        if (points.size < 2 || anchor !in points.indices) {
            notifyDetailReplay("轨迹数据不足")
            return
        }
        if (anchor <= 0) {
            notifyDetailReplay("已位于起点")
            return
        }
        _uiState.update {
            it.copy(
                detailReplayPanelVisible = true,
                detailReplayDirection = DetailReplayDirection.REVERSE,
                detailReplayCurrentIndex = anchor,
                detailReplayPlaying = true,
            )
        }
        startDetailReplayLoop()
    }

    fun toggleDetailReplayPlayPause() {
        val state = _uiState.value
        if (state.detailReplayAnchorIndex !in state.selectedWorkoutReplayPoints.indices) return
        val next = !state.detailReplayPlaying
        _uiState.update { it.copy(detailReplayPlaying = next) }
        if (next) startDetailReplayLoop() else detailReplayJob?.cancel()
    }

    fun setDetailReplaySpeed(speed: DetailReplaySpeed) {
        _uiState.update { it.copy(detailReplaySpeed = speed) }
    }

    fun switchDetailReplayDirection() {
        _uiState.update {
            it.copy(
                detailReplayDirection = if (it.detailReplayDirection == DetailReplayDirection.FORWARD) {
                    DetailReplayDirection.REVERSE
                } else {
                    DetailReplayDirection.FORWARD
                }
            )
        }
    }

    fun seekDetailReplayByProgress(progress: Float) {
        val state = _uiState.value
        val points = state.selectedWorkoutReplayPoints
        if (points.isEmpty()) return
        val index = ((points.lastIndex) * progress.coerceIn(0f, 1f)).roundToInt().coerceIn(0, points.lastIndex)
        detailReplayJob?.cancel()
        _uiState.update {
            it.copy(
                detailReplayCurrentIndex = index,
                detailReplayPlaying = false,
            )
        }
    }

    fun exitDetailReplayPanel() {
        detailReplayJob?.cancel()
        _uiState.update {
            it.copy(
                detailReplayPanelVisible = false,
                detailReplayAnchorIndex = -1,
                detailReplayCurrentIndex = -1,
                detailReplayPlaying = false,
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
        _uiState.update {
            val nextDate = it.historyFilterDate.plusDays(step)
            it.copy(
                historyFilterDate = nextDate,
                historyVisibleCount = 5,
                weekAnchorDate = nextDate,
                monthAnchor = YearMonth.from(nextDate),
                yearAnchor = nextDate.year,
            )
        }
        refreshDerivedStatsAndFilters()
    }

    fun setHistoryFilterDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                historyFilterDate = date,
                historyVisibleCount = 5,
                weekAnchorDate = date,
                monthAnchor = YearMonth.from(date),
                yearAnchor = date.year,
            )
        }
        refreshDerivedStatsAndFilters()
    }

    fun setCustomRangeStartDate(date: LocalDate) {
        _uiState.update {
            val end = if (date.isAfter(it.customRangeEndDate)) date else it.customRangeEndDate
            it.copy(customRangeStartDate = date, customRangeEndDate = end, historyVisibleCount = 5)
        }
        refreshDerivedStatsAndFilters()
    }

    fun setCustomRangeEndDate(date: LocalDate) {
        _uiState.update {
            val start = if (date.isBefore(it.customRangeStartDate)) date else it.customRangeStartDate
            it.copy(customRangeStartDate = start, customRangeEndDate = date, historyVisibleCount = 5)
        }
        refreshDerivedStatsAndFilters()
    }

    fun setHistoryTypeFilter(type: ExerciseType?) {
        _uiState.update { it.copy(historyTypeFilter = type, historyVisibleCount = 5) }
        refreshDerivedStatsAndFilters()
    }

    fun loadMoreHistory() {
        _uiState.update { it.copy(historyVisibleCount = (it.historyVisibleCount + 5).coerceAtMost(200)) }
    }

    fun resetHistoryVisibleCount() {
        _uiState.update { it.copy(historyVisibleCount = 5) }
    }

    fun updateProfile(profile: UserProfile) {
        val normalized = profile.copy(
            shareRecipientEmails = profile.shareRecipientEmails
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct(),
        )
        _uiState.update { it.copy(profile = normalized) }
        persistProfile()
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

    fun setHistoryFieldVisible(field: HistoryDisplayField, visible: Boolean) {
        _uiState.update { state ->
            val next = state.settings.visibleHistoryFields.toMutableSet()
            if (visible) {
                next.add(field)
            } else {
                if (next.size == 1 && next.contains(field)) return@update state
                next.remove(field)
            }
            state.copy(settings = state.settings.copy(visibleHistoryFields = next))
        }
        persistSettings()
    }

    fun setShowTrackArea(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(showTrackArea = enabled)) }
        persistSettings()
    }

    fun setPanelRefreshIntervalSeconds(seconds: Int) {
        _uiState.update {
            it.copy(settings = it.settings.copy(panelRefreshIntervalSeconds = seconds.coerceIn(1, 30)))
        }
        persistSettings()
        startInputs(forceRestartLocation = true)
    }

    fun setCompassAutoCalibrationEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(settings = it.settings.copy(compassAutoCalibrationEnabled = enabled))
        }
        persistSettings()
        updateAzimuthDisplay()
    }

    fun setCompassCalibrationMode(enabled: Boolean) {
        _uiState.update { it.copy(compassCalibrationMode = enabled) }
    }

    fun calibrateCompassSetCurrentAsNorth() {
        val targetOffset = normalizeDegrees(-latestAzimuthRawDegrees)
        _uiState.update {
            it.copy(settings = it.settings.copy(compassCalibrationOffsetDegrees = targetOffset))
        }
        persistSettings()
        updateAzimuthDisplay()
    }

    fun resetCompassCalibration() {
        _uiState.update {
            it.copy(settings = it.settings.copy(compassCalibrationOffsetDegrees = 0f))
        }
        persistSettings()
        updateAzimuthDisplay()
    }

    fun toggleCompassBacktrackMode(enabled: Boolean) {
        if (enabled) {
            prepareBacktrackNodesForMode()
            if (activeBacktrackNodes.size < 2) {
                _uiState.update {
                    it.copy(
                        compassBacktrackMode = false,
                        compassBacktrackTargetIndex = -1,
                        activeBacktrackNodeCount = activeBacktrackNodes.size,
                    )
                }
                return
            }
            _uiState.update {
                it.copy(
                    compassBacktrackMode = true,
                    activeBacktrackNodeCount = activeBacktrackNodes.size,
                )
            }
            setNearestBacktrackTarget()
            updateBacktrackFromLocation(latestLocation)
            return
        }

        _uiState.update {
            it.copy(
                compassBacktrackMode = false,
                compassBacktrackTargetIndex = -1,
                compassBacktrackBearingDegree = null,
                compassBacktrackDistanceMeters = null,
                compassBacktrackDeviationMeters = null,
            )
        }
    }

    fun backtrackToPreviousPoint() {
        _uiState.update {
            if (!it.compassBacktrackMode || activeBacktrackNodes.isEmpty()) return@update it
            val nextIndex = (it.compassBacktrackTargetIndex - 1).coerceAtLeast(0)
            it.copy(compassBacktrackTargetIndex = nextIndex)
        }
        updateBacktrackFromLocation(latestLocation)
    }

    fun backtrackToNextPoint() {
        _uiState.update {
            if (!it.compassBacktrackMode || activeBacktrackNodes.isEmpty()) return@update it
            val maxIndex = (activeBacktrackNodes.size - 1).coerceAtLeast(0)
            val nextIndex = (it.compassBacktrackTargetIndex + 1).coerceAtMost(maxIndex)
            it.copy(compassBacktrackTargetIndex = nextIndex)
        }
        updateBacktrackFromLocation(latestLocation)
    }

    fun setBacktrackTargetIndex(index: Int) {
        _uiState.update {
            val maxIndex = (activeBacktrackNodes.size - 1).coerceAtLeast(0)
            it.copy(compassBacktrackTargetIndex = index.coerceIn(0, maxIndex))
        }
        updateBacktrackFromLocation(latestLocation)
    }

    fun startBacktrackFromHistory(routeId: Long, direction: BacktrackDirection) {
        selectBacktrackRoute(routeId)
        setBacktrackDirection(direction)
        toggleCompassBacktrackMode(true)
    }

    fun setBacktrackDirection(direction: BacktrackDirection) {
        _uiState.update { it.copy(backtrackDirection = direction) }
        if (_uiState.value.compassBacktrackMode) {
            setNearestBacktrackTarget()
            updateBacktrackFromLocation(latestLocation)
        }
    }

    fun setStartWithPathRecordingOnWorkout(enabled: Boolean) {
        _uiState.update { it.copy(startWithPathRecordingOnWorkout = enabled) }
    }

    fun refreshBacktrackRoutes() {
        val routes = repository.loadBacktrackRoutes()
        _uiState.update { state ->
            val selected = when {
                state.selectedBacktrackRouteId != null &&
                    routes.any { it.id == state.selectedBacktrackRouteId } -> state.selectedBacktrackRouteId
                else -> routes.firstOrNull()?.id
            }
            val activeName = routes.firstOrNull { it.id == selected }?.name ?: "实时轨迹"
            state.copy(
                backtrackRoutes = routes,
                selectedBacktrackRouteId = selected,
                activeBacktrackRouteName = activeName,
            )
        }
        prepareBacktrackNodesForMode()
    }

    fun selectBacktrackRoute(routeId: Long?) {
        _uiState.update { state ->
            val selected = if (routeId == null || state.backtrackRoutes.any { it.id == routeId }) routeId else state.selectedBacktrackRouteId
            val name = state.backtrackRoutes.firstOrNull { it.id == selected }?.name ?: "实时轨迹"
            state.copy(
                selectedBacktrackRouteId = selected,
                activeBacktrackRouteName = name,
            )
        }
        prepareBacktrackNodesForMode()
        if (_uiState.value.compassBacktrackMode) {
            setNearestBacktrackTarget()
            updateBacktrackFromLocation(latestLocation)
        }
    }

    fun saveCurrentBreadcrumbAsBacktrackRoute() {
        if (breadcrumbTrail.size < 2 || _uiState.value.isBacktrackRoutePersisting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBacktrackRoutePersisting = true) }
            val name = "回溯_手动_${LocalDateTime.now().format(routeNameFormatter)}"
            val routeId = repository.saveBacktrackRoute(
                routeName = name,
                sourceWorkoutId = null,
                nodes = breadcrumbTrail.toList(),
            )
            refreshBacktrackRoutes()
            _uiState.update { it.copy(isBacktrackRoutePersisting = false) }
            if (routeId > 0) {
                selectBacktrackRoute(routeId)
            }
            prepareBacktrackNodesForMode()
        }
    }

    fun moveBottomNavItem(tabKey: String, direction: Int) {
        _uiState.update { state ->
            val order = state.settings.bottomNavOrder.toMutableList()
            val index = order.indexOf(tabKey)
            if (index == -1) return@update state
            val target = (index + direction).coerceIn(0, order.lastIndex)
            if (target == index) return@update state
            val item = order.removeAt(index)
            order.add(target, item)
            state.copy(settings = state.settings.copy(bottomNavOrder = order))
        }
        persistSettings()
    }

    fun resetBottomNavOrder() {
        _uiState.update {
            it.copy(settings = it.settings.copy(bottomNavOrder = listOf("PANEL", "HOME", "RECORD", "PROFILE")))
        }
        persistSettings()
    }

    fun moveMyPageItem(sectionKey: String, direction: Int) {
        _uiState.update { state ->
            val order = state.settings.myPageOrder.toMutableList()
            val index = order.indexOf(sectionKey)
            if (index == -1) return@update state
            val target = (index + direction).coerceIn(0, order.lastIndex)
            if (target == index) return@update state
            val item = order.removeAt(index)
            order.add(target, item)
            state.copy(settings = state.settings.copy(myPageOrder = order))
        }
        persistSettings()
    }

    fun resetMyPageOrder() {
        _uiState.update {
            it.copy(settings = it.settings.copy(myPageOrder = listOf("device", "history", "settings")))
        }
        persistSettings()
    }

    fun setWeeklyGoalKm(km: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(weeklyGoalKm = km.coerceIn(1, 500))) }
        persistSettings()
        refreshDerivedStatsAndFilters()
    }

    fun updateMapApiKey(key: String) {
        updateServiceSettings { it.copy(mapApiKey = key, mapEnabled = it.mapEnabled && key.isNotBlank()) }
    }

    fun setMapUseSystemService(enabled: Boolean) {
        updateServiceSettings { it.copy(mapUseSystemService = enabled) }
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

    fun updateSmtpHost(value: String) {
        updateServiceSettings { it.copy(smtpHost = value.trim()) }
    }

    fun updateSmtpPort(value: Int) {
        updateServiceSettings { it.copy(smtpPort = value.coerceIn(1, 65535)) }
    }

    fun updateSmtpUsername(value: String) {
        updateServiceSettings { it.copy(smtpUsername = value.trim()) }
    }

    fun updateSmtpPassword(value: String) {
        updateServiceSettings { it.copy(smtpPassword = value) }
    }

    fun updateSmtpFromEmail(value: String) {
        updateServiceSettings { it.copy(smtpFromEmail = value.trim()) }
    }

    fun setSmtpUseTls(enabled: Boolean) {
        updateServiceSettings { it.copy(smtpUseTls = enabled) }
    }

    fun setSmtpEnabled(enabled: Boolean) {
        updateServiceSettings { it.copy(smtpEnabled = enabled && it.smtpConfigured) }
    }

    fun toggleOnlineSharing(enabled: Boolean) {
        _uiState.update {
            it.copy(
                onlineSharingEnabled = enabled,
                shareEmailStatus = if (!enabled) "在线轨迹共享已关闭" else it.shareEmailStatus,
            )
        }
        if (enabled) {
            sendOnlineShareEmail()
        }
    }

    private fun sendOnlineShareEmail() {
        val state = _uiState.value
        val smtp = state.settings.serviceIntegration
        val recipients = state.profile.shareRecipientEmails
            .map { it.trim() }
            .filter { it.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(it).matches() }
            .distinct()

        if (!smtp.smtpActive) {
            _uiState.update { it.copy(shareEmailStatus = "SMTP 未配置完成，无法发送共享邮件") }
            return
        }
        if (recipients.isEmpty()) {
            _uiState.update { it.copy(shareEmailStatus = "未配置有效共享邮箱，无法发送") }
            return
        }

        val now = System.currentTimeMillis()
        val lat = state.currentLatitude
        val lon = state.currentLongitude
        val link = buildLiveShareLink(now, lat, lon)
        val subject = "AppGo 在线轨迹共享 - ${state.selectedType.label}"
        val body = buildString {
            appendLine("你收到一条来自 AppGo 的在线轨迹共享。")
            appendLine()
            appendLine("运动类型: ${state.selectedType.label}")
            appendLine("状态: ${state.recordingStatus.name}")
            appendLine("里程: ${"%.2f".format(state.distanceMeters / 1000.0)} km")
            appendLine("用时: ${state.elapsedSeconds} 秒")
            appendLine("位置: ${lat?.let { "%.6f".format(it) } ?: "--"}, ${lon?.let { "%.6f".format(it) } ?: "--"}")
            appendLine("共享链接: $link")
        }

        _uiState.update { it.copy(shareEmailStatus = "正在发送共享邮件...") }
        viewModelScope.launch(Dispatchers.IO) {
            val result = SmtpEmailSender.send(
                host = smtp.smtpHost,
                port = smtp.smtpPort,
                useTls = smtp.smtpUseTls,
                username = smtp.smtpUsername,
                password = smtp.smtpPassword,
                fromEmail = smtp.smtpFromEmail,
                toEmails = recipients,
                subject = subject,
                body = body,
            )
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(shareEmailStatus = "共享邮件已发送（${recipients.size}人）")
                } else {
                    it.copy(shareEmailStatus = "共享邮件发送失败: ${result.exceptionOrNull()?.message ?: "未知错误"}")
                }
            }
        }
    }

    private fun buildLiveShareLink(now: Long, latitude: Double?, longitude: Double?): String {
        val latText = latitude?.let { "%.6f".format(it) } ?: "0"
        val lonText = longitude?.let { "%.6f".format(it) } ?: "0"
        return "https://appgo.local/live?ts=$now&lat=$latText&lon=$lonText"
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
                val folder = backupFolder()
                val file = File(folder, "backup_${now}.json")
                file.writeText(backupJson)
                localPath = file.absolutePath
                targets += "本地"
                pruneLocalBackups(folder, backupConfig.strategy.retainDays)
            }

            if (backupConfig.cloudConfig.isActive) {
                val folder = backupFolder()
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
            refreshBackupFiles()
            _uiState.update { it.copy(backupInProgress = false) }
        }
    }

    fun refreshBackupFiles() {
        val files = backupFolder().listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }
            ?: emptyList()
        _uiState.update { state ->
            val selected = state.selectedBackupFileName
            val nextSelected = if (selected != null && files.contains(selected)) selected else files.firstOrNull()
            val nextContent = nextSelected?.let { fileName ->
                runCatching { File(backupFolder(), fileName).readText() }.getOrDefault("")
            } ?: ""
            state.copy(
                backupFiles = files,
                selectedBackupFileName = nextSelected,
                selectedBackupFileContent = nextContent,
            )
        }
    }

    fun openBackupFile(fileName: String) {
        val content = runCatching { File(backupFolder(), fileName).readText() }.getOrDefault("")
        _uiState.update {
            it.copy(
                selectedBackupFileName = fileName,
                selectedBackupFileContent = content,
            )
        }
    }

    fun updateBackupFileContent(content: String) {
        _uiState.update { it.copy(selectedBackupFileContent = content) }
    }

    fun saveBackupFileContent() {
        val state = _uiState.value
        val selected = state.selectedBackupFileName ?: return
        val file = File(backupFolder(), selected)
        runCatching { file.writeText(state.selectedBackupFileContent) }
        refreshBackupFiles()
    }

    override fun onCleared() {
        super.onCleared()
        stopTickerOnly()
        detailReplayJob?.cancel()
        stopInputs()
    }

    private fun refreshFromDatabase() {
        val history = repository.loadWorkoutSummaries()
        val defaultId: Long? = null
        val defaultTrack = emptyList<TrackPoint>()
        val replayPoints = emptyList<DetailReplayPoint>()
        val analysis = analyzeWorkoutTrack(defaultTrack)
        _uiState.update {
            it.copy(
                history = history,
                lastWorkout = history.firstOrNull(),
                selectedWorkoutId = defaultId,
                selectedWorkoutTrack = defaultTrack,
                selectedWorkoutReplayPoints = replayPoints,
                selectedWorkoutSummary = null,
                selectedWorkoutSegmentPace = analysis.segmentPace,
                selectedWorkoutHeartRateZones = analysis.heartRateZones,
                selectedWorkoutPaceSeries = analysis.paceSeries,
                selectedWorkoutHeartRateSeries = analysis.heartRateSeries,
                detailReplayPanelVisible = false,
                detailReplayAnchorIndex = -1,
                detailReplayCurrentIndex = -1,
                detailReplayDirection = DetailReplayDirection.FORWARD,
                detailReplayPlaying = false,
            )
        }
        refreshDerivedStatsAndFilters()
        refreshBacktrackRoutes()
    }

    private fun refreshDerivedStatsAndFilters() {
        val state = _uiState.value
        val derived = HistoryDomain.derive(
            history = state.history,
            historyFilterDate = state.historyFilterDate,
            weekAnchorDate = state.weekAnchorDate,
            monthAnchor = state.monthAnchor,
            yearAnchor = state.yearAnchor,
            customRangeStartDate = state.customRangeStartDate,
            customRangeEndDate = state.customRangeEndDate,
            historyTypeFilter = state.historyTypeFilter,
        )

        _uiState.update {
            it.copy(
                dailyStats = derived.dailyStats,
                weeklyStats = derived.weeklyStats,
                monthlyStats = derived.monthlyStats,
                yearlyStats = derived.yearlyStats,
                customRangeStats = derived.customRangeStats,
                filteredHistory = derived.filteredHistory,
                weeklyDistanceMeters = derived.weeklyStats.totalDistanceMeters,
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
                smtpEnabled = changed.smtpEnabled && changed.smtpConfigured,
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
        val historyFieldsRaw = prefs.getString("visible_history_fields", null)
        val historyFields = historyFieldsRaw
            ?.split(",")
            ?.mapNotNull { name -> HistoryDisplayField.entries.find { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: AppSettings().visibleHistoryFields

        val mapApiKey = prefs.getString("map_api_key", "") ?: ""
        val mapUseSystemService = prefs.getBoolean("map_use_system_service", true)
        val mapEnabled = prefs.getBoolean("map_enabled", false) && mapApiKey.isNotBlank()
        val weatherApiKey = prefs.getString("weather_api_key", "") ?: ""
        val weatherEnabled = prefs.getBoolean("weather_enabled", false) && weatherApiKey.isNotBlank()
        val shareApiKey = prefs.getString("share_api_key", "") ?: ""
        val shareEnabled = prefs.getBoolean("share_enabled", false) && shareApiKey.isNotBlank()
        val smtpHost = prefs.getString("smtp_host", "") ?: ""
        val smtpPort = prefs.getInt("smtp_port", 587).coerceIn(1, 65535)
        val smtpUsername = prefs.getString("smtp_username", "") ?: ""
        val smtpPassword = prefs.getString("smtp_password", "") ?: ""
        val smtpFromEmail = prefs.getString("smtp_from_email", "") ?: ""
        val smtpUseTls = prefs.getBoolean("smtp_use_tls", true)
        val smtpEnabled = prefs.getBoolean("smtp_enabled", false)
        val showTrackArea = prefs.getBoolean("show_track_area", true)
        val panelRefreshIntervalSeconds = prefs.getInt("panel_refresh_interval_sec", 2).coerceIn(1, 30)
        val compassAutoCalibrationEnabled = prefs.getBoolean("compass_auto_calibration_enabled", true)
        val bottomNavOrder = normalizeBottomNavOrder(
            prefs.getString("bottom_nav_order", null)
                ?.split(",")
                ?.map { it.trim().uppercase(Locale.ROOT) }
                ?: emptyList()
        )
        val myPageOrder = normalizeMyPageOrder(
            prefs.getString("my_page_order", null)
                ?.split(",")
                ?.map { it.trim() }
                ?: emptyList()
        )
        val weeklyGoalKm = prefs.getInt("weekly_goal_km", 30).coerceIn(1, 500)
        val compassCalibrationOffsetDegrees = prefs.getFloat("compass_calibration_offset_deg", 0f)

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
                    showTrackArea = showTrackArea,
                    panelRefreshIntervalSeconds = panelRefreshIntervalSeconds,
                    compassAutoCalibrationEnabled = compassAutoCalibrationEnabled,
                    weeklyGoalKm = weeklyGoalKm,
                    compassCalibrationOffsetDegrees = compassCalibrationOffsetDegrees,
                    bottomNavOrder = bottomNavOrder,
                    myPageOrder = myPageOrder,
                    visibleHistoryFields = historyFields,
                    serviceIntegration = ServiceIntegrationConfig(
                        mapUseSystemService = mapUseSystemService,
                        mapApiKey = mapApiKey,
                        mapEnabled = mapEnabled,
                        weatherApiKey = weatherApiKey,
                        weatherEnabled = weatherEnabled,
                        shareApiKey = shareApiKey,
                        shareEnabled = shareEnabled,
                        smtpHost = smtpHost,
                        smtpPort = smtpPort,
                        smtpUsername = smtpUsername,
                        smtpPassword = smtpPassword,
                        smtpFromEmail = smtpFromEmail,
                        smtpUseTls = smtpUseTls,
                        smtpEnabled = smtpEnabled,
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

    private fun loadProfile() {
        val recipients = prefs.getString("profile_share_recipients", null)
            ?.split(";")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
        _uiState.update { it.copy(profile = it.profile.copy(shareRecipientEmails = recipients)) }
    }

    private fun persistSettings() {
        val settings = _uiState.value.settings
        prefs.edit()
            .putString("visible_metrics", settings.visibleMetrics.joinToString(",") { it.name })
            .putString("visible_history_fields", settings.visibleHistoryFields.joinToString(",") { it.name })
            .putBoolean("show_track_area", settings.showTrackArea)
            .putInt("panel_refresh_interval_sec", settings.panelRefreshIntervalSeconds)
            .putBoolean("compass_auto_calibration_enabled", settings.compassAutoCalibrationEnabled)
            .putInt("weekly_goal_km", settings.weeklyGoalKm)
            .putFloat("compass_calibration_offset_deg", settings.compassCalibrationOffsetDegrees)
            .putString("bottom_nav_order", normalizeBottomNavOrder(settings.bottomNavOrder).joinToString(","))
            .putString("my_page_order", normalizeMyPageOrder(settings.myPageOrder).joinToString(","))
            .putBoolean("map_use_system_service", settings.serviceIntegration.mapUseSystemService)
            .putString("map_api_key", settings.serviceIntegration.mapApiKey)
            .putBoolean("map_enabled", settings.serviceIntegration.mapEnabled)
            .putString("weather_api_key", settings.serviceIntegration.weatherApiKey)
            .putBoolean("weather_enabled", settings.serviceIntegration.weatherEnabled)
            .putString("share_api_key", settings.serviceIntegration.shareApiKey)
            .putBoolean("share_enabled", settings.serviceIntegration.shareEnabled)
            .putString("smtp_host", settings.serviceIntegration.smtpHost)
            .putInt("smtp_port", settings.serviceIntegration.smtpPort)
            .putString("smtp_username", settings.serviceIntegration.smtpUsername)
            .putString("smtp_password", settings.serviceIntegration.smtpPassword)
            .putString("smtp_from_email", settings.serviceIntegration.smtpFromEmail)
            .putBoolean("smtp_use_tls", settings.serviceIntegration.smtpUseTls)
            .putBoolean("smtp_enabled", settings.serviceIntegration.smtpEnabled)
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

    private fun persistProfile() {
        val profile = _uiState.value.profile
        prefs.edit()
            .putString("profile_share_recipients", profile.shareRecipientEmails.joinToString(";"))
            .apply()
    }

    private fun pruneLocalBackups(folder: File, retainDays: Int) {
        val threshold = System.currentTimeMillis() - retainDays * 24L * 3600L * 1000L
        folder.listFiles()
            ?.filter { it.name.startsWith("backup_") && it.name.endsWith(".json") }
            ?.filter { it.lastModified() < threshold }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun backupFolder(): File {
        val folder = File(getApplication<Application>().filesDir, "backups")
        folder.mkdirs()
        return folder
    }

    private fun startInputs(forceRestartLocation: Boolean = false) {
        if (!monitoringInputs) {
            if (gyroscope != null) {
                sensorManager.registerListener(gyroListener, gyroscope, SensorManager.SENSOR_DELAY_UI)
            }
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(rotationVectorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            }
            if (magneticSensor != null) {
                sensorManager.registerListener(magneticListener, magneticSensor, SensorManager.SENSOR_DELAY_UI)
            }
            if (pressureSensor != null) {
                sensorManager.registerListener(pressureListener, pressureSensor, SensorManager.SENSOR_DELAY_UI)
            }
            monitoringInputs = true
        }

        if (!hasLocationPermission()) return
        if (forceRestartLocation) {
            runCatching { locationManager.removeUpdates(locationListener) }
        }

        val intervalMillis = (_uiState.value.settings.panelRefreshIntervalSeconds * 1000L).coerceAtLeast(1000L)
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMillis,
                    0f,
                    locationListener,
                    Looper.getMainLooper(),
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalMillis,
                    0f,
                    locationListener,
                    Looper.getMainLooper(),
                )
            }
        } catch (_: SecurityException) {
            _uiState.update { it.copy(hasLocationFix = false, locationAccuracyMeters = null) }
        }
    }

    private fun stopTickerOnly() {
        tickerJob?.cancel()
    }

    private fun stopInputs() {
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(rotationVectorListener)
        sensorManager.unregisterListener(magneticListener)
        sensorManager.unregisterListener(pressureListener)
        runCatching { locationManager.removeUpdates(locationListener) }
        monitoringInputs = false
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value

                if (state.recordingStatus == RecordingStatus.FINISHED) {
                    continue
                }

                val now = System.currentTimeMillis()
                if (now - lastWeatherUpdateMillis >= 10_000) {
                    refreshWeatherSnapshot(latestLocation, force = true)
                }
                val location = latestLocation
                val speedFromLocation = if (location != null && location.hasSpeed()) location.speed.toDouble() else 0.0
                val speedFromGyro = max(0.0, (latestGyroMagnitude - 0.06f) * 4.2).toDouble()
                val derivedSpeed = max(speedFromLocation, speedFromGyro)
                val locationMoving = speedFromLocation >= 0.7 && (now - lastLocationMillis) <= 8_000
                val gyroMoving = latestGyroMagnitude >= 0.12f && (now - lastGyroMillis) <= 3_000
                val movementDetected = locationMoving || gyroMoving
                val gyroStatus = workStatus(gyroscope != null, now, lastGyroMillis)
                val azimuthStatus = workStatus(rotationVectorSensor != null, now, lastAzimuthMillis)
                val compassStatus = workStatus(magneticSensor != null, now, lastCompassMillis)
                val altitudeStatus = workStatus(pressureSensor != null, now, lastAltitudeSensorMillis)

                val nextLowSpeed = if (!movementDetected) state.lowSpeedSeconds + 1 else 0
                val autoPause = nextLowSpeed >= 10

                val nextStatus = when {
                    state.recordingStatus == RecordingStatus.PAUSED -> RecordingStatus.PAUSED
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
                        azimuthDegree = latestAzimuthDegrees,
                        pitchDegree = latestPitchDegrees,
                        rollDegree = latestRollDegrees,
                        gyroWorkStatus = gyroStatus,
                        azimuthWorkStatus = azimuthStatus,
                        compassWorkStatus = compassStatus,
                        altitudeWorkStatus = altitudeStatus,
                        trackPoints = nextPoints,
                    )
                }
            }
        }
    }

    private fun startDetailReplayLoop() {
        detailReplayJob?.cancel()
        detailReplayJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                val points = state.selectedWorkoutReplayPoints
                if (!state.detailReplayPlaying || !state.detailReplayPanelVisible || points.size < 2) break
                val current = state.detailReplayCurrentIndex
                if (current !in points.indices) break

                val next = when (state.detailReplayDirection) {
                    DetailReplayDirection.FORWARD -> {
                        if (current >= points.lastIndex) {
                            _uiState.update { it.copy(detailReplayPlaying = false) }
                            notifyDetailReplay("已到达终点")
                            break
                        }
                        ReplayDomain.nextIndex(
                            current = current,
                            lastIndex = points.lastIndex,
                            direction = state.detailReplayDirection,
                            isLongTrack = points.size > 20_000,
                        )
                    }
                    DetailReplayDirection.REVERSE -> {
                        if (current <= 0) {
                            _uiState.update { it.copy(detailReplayPlaying = false) }
                            notifyDetailReplay("已回到起点")
                            break
                        }
                        ReplayDomain.nextIndex(
                            current = current,
                            lastIndex = points.lastIndex,
                            direction = state.detailReplayDirection,
                            isLongTrack = points.size > 20_000,
                        )
                    }
                }
                _uiState.update { it.copy(detailReplayCurrentIndex = next) }
                delay(state.detailReplaySpeed.frameDelayMillis)
            }
        }
    }

    private fun notifyDetailReplay(message: String) {
        _uiState.update {
            it.copy(
                detailReplayNotice = message,
                detailReplayNoticeVersion = it.detailReplayNoticeVersion + 1,
                detailReplayPlaying = false,
            )
        }
    }

    private data class WorkoutAnalysis(
        val segmentPace: List<SegmentPace>,
        val heartRateZones: List<HeartRateZoneStat>,
        val paceSeries: List<Float>,
        val heartRateSeries: List<Float>,
    )

    private fun analyzeWorkoutTrack(track: List<TrackPoint>): WorkoutAnalysis {
        if (track.size < 2) {
            return WorkoutAnalysis(
                segmentPace = emptyList(),
                heartRateZones = defaultHeartRateZones(),
                paceSeries = emptyList(),
                heartRateSeries = emptyList(),
            )
        }

        val segmentPace = mutableListOf<SegmentPace>()
        var segDistance = 0.0
        var segDuration = 0L
        var segIndex = 1

        val paceSeries = mutableListOf<Float>()
        val heartRateSeries = mutableListOf<Float>()
        val zones = mutableMapOf(
            "Z1(<=119)" to 0L,
            "Z2(120-139)" to 0L,
            "Z3(140-159)" to 0L,
            "Z4(160-179)" to 0L,
            "Z5(>=180)" to 0L,
        )

        for (i in 1 until track.size) {
            val prev = track[i - 1]
            val cur = track[i]
            val deltaSec = ((cur.timestampMillis - prev.timestampMillis) / 1000L).coerceAtLeast(1L)
            val deltaDistance = distanceMeters(prev.latitude, prev.longitude, cur.latitude, cur.longitude)

            segDistance += deltaDistance
            segDuration += deltaSec

            val instantPace = if (deltaDistance > 1.0) {
                ((deltaSec / (deltaDistance / 1000.0)).coerceIn(120.0, 2000.0)).toFloat()
            } else {
                0f
            }
            paceSeries += instantPace
            heartRateSeries += (cur.heartRate ?: 0).toFloat()

            cur.heartRate?.let { hr ->
                val zoneKey = when {
                    hr <= 119 -> "Z1(<=119)"
                    hr <= 139 -> "Z2(120-139)"
                    hr <= 159 -> "Z3(140-159)"
                    hr <= 179 -> "Z4(160-179)"
                    else -> "Z5(>=180)"
                }
                zones[zoneKey] = (zones[zoneKey] ?: 0L) + deltaSec
            }

            if (segDistance >= 1000.0) {
                val paceSecPerKm = if (segDistance > 1.0) ((segDuration / (segDistance / 1000.0)).roundToInt()) else 0
                segmentPace += SegmentPace(
                    segmentIndex = segIndex,
                    distanceMeters = segDistance,
                    durationSeconds = segDuration,
                    paceSecondsPerKm = paceSecPerKm,
                )
                segIndex += 1
                segDistance = 0.0
                segDuration = 0L
            }
        }

        if (segDistance > 10) {
            val paceSecPerKm = if (segDistance > 1.0) ((segDuration / (segDistance / 1000.0)).roundToInt()) else 0
            segmentPace += SegmentPace(
                segmentIndex = segIndex,
                distanceMeters = segDistance,
                durationSeconds = segDuration,
                paceSecondsPerKm = paceSecPerKm,
            )
        }

        val heartRateZones = listOf(
            HeartRateZoneStat("Z1(<=119)", 0, 119, zones["Z1(<=119)"] ?: 0),
            HeartRateZoneStat("Z2(120-139)", 120, 139, zones["Z2(120-139)"] ?: 0),
            HeartRateZoneStat("Z3(140-159)", 140, 159, zones["Z3(140-159)"] ?: 0),
            HeartRateZoneStat("Z4(160-179)", 160, 179, zones["Z4(160-179)"] ?: 0),
            HeartRateZoneStat("Z5(>=180)", 180, 255, zones["Z5(>=180)"] ?: 0),
        )

        return WorkoutAnalysis(
            segmentPace = segmentPace,
            heartRateZones = heartRateZones,
            paceSeries = paceSeries,
            heartRateSeries = heartRateSeries,
        )
    }

    private fun defaultHeartRateZones(): List<HeartRateZoneStat> = listOf(
        HeartRateZoneStat("Z1(<=119)", 0, 119, 0),
        HeartRateZoneStat("Z2(120-139)", 120, 139, 0),
        HeartRateZoneStat("Z3(140-159)", 140, 159, 0),
        HeartRateZoneStat("Z4(160-179)", 160, 179, 0),
        HeartRateZoneStat("Z5(>=180)", 180, 255, 0),
    )

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        ReplayDomain.haversineMeters(lat1, lon1, lat2, lon2)

    private fun workStatus(sensorAvailable: Boolean, nowMillis: Long, lastMillis: Long): SensorWorkStatus {
        if (!sensorAvailable) return SensorWorkStatus.BAD
        if (lastMillis <= 0) return SensorWorkStatus.WARN
        val age = nowMillis - lastMillis
        return when {
            age <= 4_000 -> SensorWorkStatus.GOOD
            age <= 12_000 -> SensorWorkStatus.WARN
            else -> SensorWorkStatus.BAD
        }
    }

    private fun refreshWeatherSnapshot(location: Location? = latestLocation, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastWeatherUpdateMillis < 10_000) return
        lastWeatherUpdateMillis = now

        val state = _uiState.value
        val humidity = (58 + Random.nextInt(-8, 12)).coerceIn(20, 98)
        val weather = WeatherDomain.buildLocationDrivenWeather(
            now = now,
            latitude = location?.latitude ?: state.currentLatitude,
            longitude = location?.longitude ?: state.currentLongitude,
            altitude = state.currentAltitude,
            currentSpeedMps = state.currentSpeedMps,
            pressureHpa = latestPressureHpa,
            windDirectionDegree = location?.bearing?.roundToInt()?.let { b -> ((b % 360) + 360) % 360 },
            humidity = humidity,
        )

        _uiState.update {
            it.copy(
                weather = weather,
                weatherForecast3d = WeatherDomain.buildThreeDayForecast(weather, now),
            )
        }
    }

    private fun updateAzimuthDisplay() {
        val location = latestLocation
        val declination = if (location != null) {
            GeomagneticField(
                location.latitude.toFloat(),
                location.longitude.toFloat(),
                location.altitude.toFloat(),
                System.currentTimeMillis(),
            ).declination
        } else {
            0f
        }
        val state = _uiState.value
        val baseAzimuth = latestAzimuthRawDegrees
        val offset = _uiState.value.settings.compassCalibrationOffsetDegrees
        val target = normalizeDegrees(baseAzimuth + declination + offset)
        latestAzimuthDegrees = if (state.settings.compassAutoCalibrationEnabled) {
            val current = latestAzimuthDegrees
            if (current == 0f) {
                target
            } else {
                normalizeDegrees(current + signedAngleDelta(current, target) * 0.25f)
            }
        } else {
            target
        }
        _uiState.update { it.copy(azimuthDegree = latestAzimuthDegrees) }
    }

    private fun normalizeDegrees(degrees: Float): Float {
        var v = degrees % 360f
        if (v < 0f) v += 360f
        return v
    }

    private fun signedAngleDelta(from: Float, to: Float): Float {
        var delta = (to - from + 540f) % 360f - 180f
        if (!delta.isFinite()) delta = 0f
        return delta
    }

    private fun normalizeBottomNavOrder(raw: List<String>): List<String> {
        val default = listOf("PANEL", "HOME", "RECORD", "PROFILE")
        if (raw.isEmpty()) return default
        val filtered = raw.filter { it in default }.distinct().toMutableList()
        default.forEach { if (!filtered.contains(it)) filtered.add(it) }
        return filtered.take(default.size)
    }

    private fun normalizeMyPageOrder(raw: List<String>): List<String> {
        val default = listOf("device", "history", "settings")
        if (raw.isEmpty()) return default
        val filtered = raw.map { it.lowercase(Locale.ROOT) }.filter { it in default }.distinct().toMutableList()
        default.forEach { if (!filtered.contains(it)) filtered.add(it) }
        return filtered.take(default.size)
    }

    private fun appendBreadcrumb(location: Location) {
        val now = System.currentTimeMillis()
        val last = breadcrumbTrail.lastOrNull()
        var changed = false
        if (last == null) {
            breadcrumbTrail += BreadcrumbPoint(now, location.latitude, location.longitude)
            changed = true
        } else {
            val distance = distanceMeters(last.latitude, last.longitude, location.latitude, location.longitude)
            val timeDelta = now - last.timestampMillis
            if (distance >= 8.0 || timeDelta >= 15_000) {
                breadcrumbTrail += BreadcrumbPoint(now, location.latitude, location.longitude)
                if (breadcrumbTrail.size > 400) {
                    breadcrumbTrail.removeAt(0)
                }
                changed = true
            }
        }
        if (changed && _uiState.value.selectedBacktrackRouteId == null) {
            prepareBacktrackNodesForMode()
        }
    }

    private fun maybeResolveAddress(location: Location) {
        val now = System.currentTimeMillis()
        if (now - lastAddressResolveMillis < 20_000) return
        lastAddressResolveMillis = now

        viewModelScope.launch(Dispatchers.IO) {
            val text = runCatching { resolveAddressText(location) }.getOrDefault("")

            _uiState.update { state ->
                if (text.isNotBlank()) {
                    state.copy(currentAddressText = text)
                } else {
                    state.copy(currentAddressText = "经纬度 ${"%.5f".format(location.latitude)}, ${"%.5f".format(location.longitude)}")
                }
            }
        }
    }

    private suspend fun resolveAddressText(location: Location): String {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        val first = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<Address?> { cont ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    },
                )
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        }

        return if (first == null) {
            ""
        } else {
            listOfNotNull(
                first.countryName,
                first.adminArea,
                first.subAdminArea,
                first.locality,
                first.subLocality,
                first.thoroughfare,
                first.featureName,
            ).joinToString(" ").trim()
        }
    }

    private fun updateBacktrackFromLocation(location: Location?) {
        val state = _uiState.value
        if (!state.compassBacktrackMode || activeBacktrackNodes.isEmpty() || location == null) {
            _uiState.update {
                it.copy(
                    compassBacktrackBearingDegree = null,
                    compassBacktrackDistanceMeters = null,
                    compassBacktrackDeviationMeters = null,
                    compassBacktrackPointerDeviationDegree = null,
                )
            }
            return
        }

        val targetIndex = state.compassBacktrackTargetIndex.coerceIn(0, activeBacktrackNodes.lastIndex)
        val target = activeBacktrackNodes[targetIndex]
        val targetLocation = Location("backtrack-target").apply {
            latitude = target.latitude
            longitude = target.longitude
        }
        val bearing = location.bearingTo(targetLocation)
        val distance = location.distanceTo(targetLocation).toDouble()
        val step = if (state.backtrackDirection == BacktrackDirection.REVERSE) -1 else 1
        val nextIndex = targetIndex + step
        val deviation = if (nextIndex in activeBacktrackNodes.indices) {
            distancePointToSegmentMeters(
                latitude = location.latitude,
                longitude = location.longitude,
                a = activeBacktrackNodes[targetIndex],
                b = activeBacktrackNodes[nextIndex],
            )
        } else {
            distance
        }
        val deviationStatus = when {
            deviation <= 15.0 -> SensorWorkStatus.GOOD
            deviation <= 40.0 -> SensorWorkStatus.WARN
            else -> SensorWorkStatus.BAD
        }
        val pointerDeviation = kotlin.math.abs(signedAngleDelta(latestAzimuthDegrees, normalizeDegrees(bearing)))
        val pointerStatus = when {
            pointerDeviation <= 15f -> SensorWorkStatus.GOOD
            pointerDeviation <= 45f -> SensorWorkStatus.WARN
            else -> SensorWorkStatus.BAD
        }
        val progressedIndex = if (distance <= 12.0 && nextIndex in activeBacktrackNodes.indices) nextIndex else targetIndex

        _uiState.update {
            it.copy(
                compassBacktrackTargetIndex = progressedIndex,
                compassBacktrackBearingDegree = normalizeDegrees(bearing),
                compassBacktrackDistanceMeters = distance,
                compassBacktrackDeviationMeters = deviation,
                compassBacktrackDeviationStatus = deviationStatus,
                compassBacktrackPointerDeviationDegree = pointerDeviation,
                compassBacktrackPointerDeviationStatus = pointerStatus,
            )
        }
    }

    private fun prepareBacktrackNodesForMode() {
        val selectedRouteId = _uiState.value.selectedBacktrackRouteId
        val nodes = if (selectedRouteId != null) {
            repository.loadBacktrackNodes(selectedRouteId)
        } else {
            breadcrumbTrail.toList()
        }
        activeBacktrackNodes.clear()
        activeBacktrackNodes.addAll(nodes)
        _uiState.update { state ->
            val routeName = state.backtrackRoutes.firstOrNull { it.id == state.selectedBacktrackRouteId }?.name ?: "实时轨迹"
            val clampedIndex = if (activeBacktrackNodes.isEmpty()) -1 else state.compassBacktrackTargetIndex.coerceIn(0, activeBacktrackNodes.lastIndex)
            state.copy(
                activeBacktrackNodeCount = activeBacktrackNodes.size,
                activeBacktrackRouteName = routeName,
                compassBacktrackTargetIndex = clampedIndex,
                activeBacktrackNodes = activeBacktrackNodes.toList(),
            )
        }
    }

    private fun setNearestBacktrackTarget() {
        val location = latestLocation
        val state = _uiState.value
        if (activeBacktrackNodes.isEmpty()) {
            _uiState.update { it.copy(compassBacktrackTargetIndex = -1) }
            return
        }
        val fallback = if (state.backtrackDirection == BacktrackDirection.REVERSE) {
            (activeBacktrackNodes.size - 1).coerceAtLeast(0)
        } else {
            0
        }
        if (location == null) {
            _uiState.update { it.copy(compassBacktrackTargetIndex = fallback) }
            return
        }
        val nearestIndex = activeBacktrackNodes.indices.minByOrNull { idx ->
            val node = activeBacktrackNodes[idx]
            distanceMeters(location.latitude, location.longitude, node.latitude, node.longitude)
        } ?: fallback
        _uiState.update { it.copy(compassBacktrackTargetIndex = nearestIndex) }
    }

    private fun distancePointToSegmentMeters(
        latitude: Double,
        longitude: Double,
        a: BreadcrumbPoint,
        b: BreadcrumbPoint,
    ): Double {
        val meanLat = Math.toRadians((a.latitude + b.latitude + latitude) / 3.0)
        val scaleX = 111320.0 * cos(meanLat)
        val scaleY = 110540.0
        val px = longitude * scaleX
        val py = latitude * scaleY
        val ax = a.longitude * scaleX
        val ay = a.latitude * scaleY
        val bx = b.longitude * scaleX
        val by = b.latitude * scaleY

        val abx = bx - ax
        val aby = by - ay
        val ab2 = abx * abx + aby * aby
        if (ab2 <= 1e-6) {
            return sqrt((px - ax).pow(2) + (py - ay).pow(2))
        }
        val apx = px - ax
        val apy = py - ay
        val t = (apx * abx + apy * aby) / ab2
        val clampedT = t.coerceIn(0.0, 1.0)
        val closestX = ax + abx * clampedT
        val closestY = ay + aby * clampedT
        return sqrt((px - closestX).pow(2) + (py - closestY).pow(2))
    }

    private fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

}
