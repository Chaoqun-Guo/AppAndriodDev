package com.gogogo.appgo.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gogogo.appgo.model.RecordingMetric
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.SensorWorkStatus
import com.gogogo.appgo.ui.AppUiState
import kotlin.math.roundToInt

fun recordingStatusText(status: RecordingStatus): String = when (status) {
    RecordingStatus.IDLE -> "未开始"
    RecordingStatus.RECORDING -> "记录中"
    RecordingStatus.PAUSED -> "手动暂停"
    RecordingStatus.AUTO_PAUSED -> "自动暂停"
    RecordingStatus.FINISHED -> "待保存"
}

fun buildRecordingMetrics(uiState: AppUiState): List<Pair<String, String>> {
    val all = mapOf(
        RecordingMetric.DISTANCE to formatDistance(uiState.distanceMeters),
        RecordingMetric.DURATION to formatDuration(uiState.elapsedSeconds),
        RecordingMetric.PACE to formatPace(uiState.averagePaceSecondsPerKm),
        RecordingMetric.ALTITUDE to "${uiState.currentAltitude.roundToInt()} m",
        RecordingMetric.ELEVATION to "${uiState.totalElevationGain.roundToInt()} m",
        RecordingMetric.SPEED to formatMps(uiState.currentSpeedMps),
        RecordingMetric.MOVEMENT to if (uiState.movementDetected) "运动中" else "静止",
    )
    return RecordingMetric.entries
        .filter { it != RecordingMetric.HEART_RATE }
        .filter { uiState.settings.visibleMetrics.contains(it) }
        .map { metric -> metric.label to (all[metric] ?: "--") }
}

@Composable
fun SensorStatusDot(title: String, status: SensorWorkStatus) {
    val color = when (status) {
        SensorWorkStatus.GOOD -> Color(0xFF2E7D32)
        SensorWorkStatus.WARN -> Color(0xFFF9A825)
        SensorWorkStatus.BAD -> Color(0xFFC62828)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("●", color = color)
        Text(title, style = MaterialTheme.typography.bodySmall)
    }
}
