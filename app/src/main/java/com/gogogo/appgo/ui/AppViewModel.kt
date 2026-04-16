package com.gogogo.appgo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gogogo.appgo.data.WorkoutRepository
import com.gogogo.appgo.model.Achievement
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.MarkerPoint
import com.gogogo.appgo.model.RecordingStatus
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
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
    val trackPoints: List<TrackPoint> = emptyList(),
    val markerPoints: List<MarkerPoint> = emptyList(),
    val history: List<WorkoutSummary> = emptyList(),
    val selectedWorkoutId: Long? = null,
    val selectedWorkoutTrack: List<TrackPoint> = emptyList(),
    val lastWorkout: WorkoutSummary? = null,
    val weeklyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val monthlyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val yearlyStats: StatsSummary = StatsSummary(0.0, 0, 0.0, 0),
    val achievements: Set<Achievement> = emptySet(),
    val summaryDialogVisible: Boolean = false,
    val pendingRecord: WorkoutRecord? = null,
    val profile: UserProfile = UserProfile(),
    val bluetoothConnected: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var startTimeMillis: Long = 0
    private var tickerJob: Job? = null
    private var baseLat = 31.2304
    private var baseLon = 121.4737

    init {
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
                currentSpeedMps = 1.4,
                lowSpeedSeconds = 0,
                trackPoints = emptyList(),
                markerPoints = emptyList(),
                summaryDialogVisible = false,
                pendingRecord = null,
            )
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

    fun endWorkout() {
        tickerJob?.cancel()
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

    fun updateProfile(profile: UserProfile) {
        _uiState.update { it.copy(profile = profile) }
    }

    fun toggleBluetoothConnection() {
        _uiState.update { it.copy(bluetoothConnected = !it.bluetoothConnected) }
    }

    private fun refreshFromDatabase() {
        val history = repository.loadWorkoutSummaries()
        _uiState.update {
            it.copy(
                history = history,
                lastWorkout = history.firstOrNull(),
                weeklyStats = repository.statsForDays(7),
                monthlyStats = repository.statsForDays(30),
                yearlyStats = repository.statsForYear(),
                achievements = repository.loadAchievements(),
                selectedWorkoutId = history.firstOrNull()?.record?.id,
                selectedWorkoutTrack = history.firstOrNull()?.record?.id?.let(repository::loadTrackPoints)
                    ?: emptyList(),
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.recordingStatus == RecordingStatus.PAUSED) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                    continue
                }

                if (state.recordingStatus == RecordingStatus.AUTO_PAUSED) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                    continue
                }

                if (state.recordingStatus != RecordingStatus.RECORDING) continue

                val elapsed = state.elapsedSeconds + 1
                val simulatedSpeed = max(0.2, 1.8 + sin(elapsed / 14.0) * 1.1 + Random.nextDouble(-0.35, 0.35))
                val nextLowSpeed = if (simulatedSpeed < 0.5) state.lowSpeedSeconds + 1 else 0
                val autoPause = nextLowSpeed >= 10

                val deltaLat = sin(elapsed / 60.0) * 0.000015
                val deltaLon = cos(elapsed / 57.0) * 0.000015
                baseLat += deltaLat
                baseLon += deltaLon
                val altitude = state.currentAltitude + Random.nextDouble(-0.5, 1.2)
                val hr = (118 + sin(elapsed / 8.0) * 18 + Random.nextDouble(-4.0, 4.0)).roundToInt()

                val newDistance = state.distanceMeters + simulatedSpeed
                val newAscent = state.totalElevationGain + max(0.0, altitude - state.currentAltitude)
                val pace = if (newDistance > 1.0) ((elapsed / (newDistance / 1000.0)).roundToInt()) else 0

                val nextStatus = if (autoPause) RecordingStatus.AUTO_PAUSED else RecordingStatus.RECORDING
                val points = if (autoPause) {
                    state.trackPoints
                } else {
                    state.trackPoints + TrackPoint(
                        workoutId = 0,
                        timestampMillis = System.currentTimeMillis(),
                        latitude = baseLat,
                        longitude = baseLon,
                        altitude = altitude,
                        heartRate = hr,
                        speedMps = simulatedSpeed,
                    )
                }

                _uiState.update {
                    it.copy(
                        recordingStatus = nextStatus,
                        elapsedSeconds = elapsed,
                        distanceMeters = if (autoPause) state.distanceMeters else newDistance,
                        averagePaceSecondsPerKm = pace,
                        currentAltitude = altitude,
                        totalElevationGain = if (autoPause) state.totalElevationGain else newAscent,
                        currentHeartRate = hr,
                        currentSpeedMps = simulatedSpeed,
                        lowSpeedSeconds = nextLowSpeed,
                        trackPoints = points,
                    )
                }
            }
        }
    }
}
