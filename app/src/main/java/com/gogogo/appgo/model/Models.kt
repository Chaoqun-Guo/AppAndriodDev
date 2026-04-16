package com.gogogo.appgo.model

import java.time.Duration
import java.time.Instant

enum class ExerciseType(val label: String) {
    HIKING("徒步"),
    CYCLING("骑行"),
    RUNNING("跑步"),
    MOUNTAINEERING("登山")
}

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    AUTO_PAUSED,
    FINISHED
}

data class TrackPoint(
    val workoutId: Long,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val heartRate: Int?,
    val speedMps: Double,
)

data class MarkerPoint(
    val id: Long = 0,
    val workoutId: Long,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String?,
    val note: String,
)

data class WorkoutRecord(
    val id: Long = 0,
    val type: ExerciseType,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDistanceMeters: Double,
    val totalElevationGainMeters: Double,
    val averageHeartRate: Int?,
    val trackFilePath: String,
) {
    val duration: Duration
        get() = Duration.between(
            Instant.ofEpochMilli(startTimeMillis),
            Instant.ofEpochMilli(endTimeMillis)
        )
}

data class WorkoutSummary(
    val record: WorkoutRecord,
    val trackPointCount: Int,
)

data class StatsSummary(
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Long,
    val totalElevationGainMeters: Double,
    val estimatedCalories: Int,
)

enum class Achievement(val title: String, val description: String) {
    FIRST_10K("首次10km", "任意一次运动距离达到10公里"),
    TOTAL_ASCENT_1000("累计爬升1000m", "累计爬升达到1000米"),
    TOTAL_DISTANCE_100K("累计100km", "累计运动距离达到100公里"),
    TOTAL_WORKOUTS_10("10次出发", "累计完成10次运动记录")
}

data class UserProfile(
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val age: Int = 28,
    val preferences: String = "徒步、跑步",
    val emergencyContact: String = "",
)
