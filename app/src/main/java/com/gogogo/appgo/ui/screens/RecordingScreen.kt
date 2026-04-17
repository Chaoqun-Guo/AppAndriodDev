package com.gogogo.appgo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.TrackPoint
import com.gogogo.appgo.ui.AppUiState
import com.gogogo.appgo.ui.common.SensorStatusDot
import com.gogogo.appgo.ui.common.buildRecordingMetrics
import com.gogogo.appgo.ui.common.formatDistance
import com.gogogo.appgo.ui.common.formatDuration
import com.gogogo.appgo.ui.common.formatPace
import com.gogogo.appgo.ui.common.recordingStatusText
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.roundToInt

@Composable
fun RecordingScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onAddMarker: () -> Unit,
    onToggleOnlineSharing: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F9FC)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("运动状态: ${recordingStatusText(uiState.recordingStatus)}", fontWeight = FontWeight.SemiBold)
                    Text("运动判定: ${if (uiState.movementDetected) "检测到运动" else "静止"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DataCell("里程", formatDistance(uiState.distanceMeters))
                        DataCell("用时", formatDuration(uiState.elapsedSeconds))
                        DataCell("均配", formatPace(uiState.averagePaceSecondsPerKm))
                    }
                    Text("定位: ${if (uiState.hasLocationFix) "已定位" else "未定位"}${uiState.locationAccuracyMeters?.let { " (±${it.roundToInt()}m)" } ?: ""}")
                    if (uiState.recordingStatus == RecordingStatus.AUTO_PAUSED) {
                        Text("连续静止${uiState.lowSpeedSeconds}s，已自动暂停", color = MaterialTheme.colorScheme.error)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("传感器状态", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    SensorStatusDot("陀螺仪", uiState.gyroWorkStatus)
                    SensorStatusDot("方位", uiState.azimuthWorkStatus)
                    SensorStatusDot("指南针", uiState.compassWorkStatus)
                    SensorStatusDot("海拔", uiState.altitudeWorkStatus)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("实时数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val metrics = buildRecordingMetrics(uiState)
                if (metrics.isEmpty()) {
                    Text("当前未选择展示项，请到设置页勾选记录面板字段。")
                } else {
                    metrics.chunked(3).forEachIndexed { index, chunk ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            chunk.forEach { (label, value) ->
                                DataCell(label, value)
                            }
                            repeat(3 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (index != metrics.lastIndex / 3) {
                            HorizontalDivider()
                        }
                    }
                }

                val service = uiState.settings.serviceIntegration
                if (service.mapActive || service.weatherActive || service.shareActive) {
                    HorizontalDivider()
                    Text("已接入服务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (service.mapActive) Text("地图服务: 已启用")
                    if (service.weatherActive) Text("天气服务: 已启用")
                    if (service.shareActive) Text("轨迹共享服务: 已启用")
                }
            }
        }

        if (uiState.settings.showTrackArea) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                tonalElevation = 2.dp,
                color = Color(0xFFE7F1F3),
            ) {
                LiveTrackMap(trackPoints = uiState.trackPoints, modifier = Modifier.fillMaxSize())
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "轨迹区域已在设置中隐藏。",
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("轨迹在线共享", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("在线共享轨迹")
                    Switch(
                        checked = uiState.onlineSharingEnabled,
                        onCheckedChange = onToggleOnlineSharing,
                        enabled = uiState.settings.serviceIntegration.shareActive,
                    )
                }
                val recipients = if (uiState.profile.shareRecipientEmails.isEmpty()) {
                    "未配置接收人（请在我的页设置）"
                } else {
                    uiState.profile.shareRecipientEmails.joinToString(", ")
                }
                Text("接收人: $recipients", style = MaterialTheme.typography.bodySmall)
                if (uiState.shareEmailStatus.isNotBlank()) {
                    Text(uiState.shareEmailStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f).height(52.dp),
                onClick = onResume,
                enabled = uiState.recordingStatus == RecordingStatus.PAUSED || uiState.recordingStatus == RecordingStatus.AUTO_PAUSED,
            ) {
                Text("继续")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f).height(52.dp),
                onClick = onAddMarker,
            ) {
                Text("标记")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f).height(52.dp),
                onClick = onPause,
                enabled = uiState.recordingStatus == RecordingStatus.RECORDING,
            ) {
                Text("暂停")
            }
            Button(
                modifier = Modifier.weight(1f).height(52.dp),
                onClick = onEnd,
            ) {
                Text("结束")
            }
        }
    }
}

@Composable
private fun DataCell(title: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LiveTrackMap(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
) {
    if (trackPoints.isEmpty()) {
        androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("等待轨迹数据...")
        }
        return
    }
    val last = trackPoints.last()
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(last.latitude, last.longitude), 16f)
    }
    androidx.compose.runtime.LaunchedEffect(trackPoints.size) {
        val latest = trackPoints.last()
        cameraState.position = CameraPosition.fromLatLngZoom(LatLng(latest.latitude, latest.longitude), 16f)
    }
    val polylinePoints = trackPoints.map { LatLng(it.latitude, it.longitude) }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(compassEnabled = true, zoomControlsEnabled = false),
    ) {
        if (polylinePoints.size >= 2) {
            Polyline(points = polylinePoints, color = Color(0xFF1565C0), width = 8f)
        }
        Marker(
            state = MarkerState(position = LatLng(last.latitude, last.longitude)),
            title = "当前位置",
        )
    }
}
