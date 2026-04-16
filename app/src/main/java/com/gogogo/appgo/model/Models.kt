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

enum class RecordingMetric(val label: String) {
    DISTANCE("里程"),
    DURATION("用时"),
    PACE("配速"),
    ALTITUDE("海拔"),
    ELEVATION("爬升"),
    HEART_RATE("心率"),
    SPEED("速度"),
    MOVEMENT("运动状态"),
}

data class ServiceIntegrationConfig(
    val mapApiKey: String = "",
    val mapEnabled: Boolean = false,
    val weatherApiKey: String = "",
    val weatherEnabled: Boolean = false,
    val shareApiKey: String = "",
    val shareEnabled: Boolean = false,
) {
    val mapActive: Boolean
        get() = mapEnabled && mapApiKey.isNotBlank()

    val weatherActive: Boolean
        get() = weatherEnabled && weatherApiKey.isNotBlank()

    val shareActive: Boolean
        get() = shareEnabled && shareApiKey.isNotBlank()
}

data class AppSettings(
    val visibleMetrics: Set<RecordingMetric> = setOf(
        RecordingMetric.DISTANCE,
        RecordingMetric.DURATION,
        RecordingMetric.PACE,
        RecordingMetric.ALTITUDE,
        RecordingMetric.ELEVATION,
        RecordingMetric.HEART_RATE,
    ),
    val serviceIntegration: ServiceIntegrationConfig = ServiceIntegrationConfig(),
    val backupConfig: BackupConfig = BackupConfig(),
)

enum class BackupCloudProvider(val label: String) {
    NONE("不使用云端"),
    GOOGLE_CLOUD("Google Cloud Storage"),
    S3_COMPATIBLE("S3 兼容对象存储"),
    WEBDAV("WebDAV 云盘"),
}

enum class BackupStrategyTemplate(
    val label: String,
    val intervalHours: Int,
    val wifiOnly: Boolean,
    val chargingOnly: Boolean,
    val retainDays: Int,
) {
    SAFE_DAILY("安全优先（每日）", intervalHours = 24, wifiOnly = true, chargingOnly = true, retainDays = 30),
    BALANCED_6H("均衡（每6小时）", intervalHours = 6, wifiOnly = true, chargingOnly = false, retainDays = 14),
    FREQUENT_1H("高频（每1小时）", intervalHours = 1, wifiOnly = false, chargingOnly = false, retainDays = 7),
    CUSTOM("自定义", intervalHours = 12, wifiOnly = true, chargingOnly = false, retainDays = 14),
}

data class BackupStrategy(
    val autoBackupEnabled: Boolean = false,
    val intervalHours: Int = BackupStrategyTemplate.SAFE_DAILY.intervalHours,
    val wifiOnly: Boolean = BackupStrategyTemplate.SAFE_DAILY.wifiOnly,
    val chargingOnly: Boolean = BackupStrategyTemplate.SAFE_DAILY.chargingOnly,
    val retainDays: Int = BackupStrategyTemplate.SAFE_DAILY.retainDays,
)

data class CloudBackupConfig(
    val provider: BackupCloudProvider = BackupCloudProvider.NONE,
    val enabled: Boolean = false,
    val apiKey: String = "",
    val secret: String = "",
    val bucketOrPath: String = "",
    val endpoint: String = "",
) {
    val isConfigured: Boolean
        get() = provider != BackupCloudProvider.NONE &&
            apiKey.isNotBlank() &&
            bucketOrPath.isNotBlank()

    val isActive: Boolean
        get() = enabled && isConfigured
}

data class BackupConfig(
    val localBackupEnabled: Boolean = true,
    val cloudConfig: CloudBackupConfig = CloudBackupConfig(),
    val strategyTemplate: BackupStrategyTemplate = BackupStrategyTemplate.SAFE_DAILY,
    val strategy: BackupStrategy = BackupStrategy(),
    val lastBackupTimeMillis: Long? = null,
    val lastBackupResult: String = "",
)
