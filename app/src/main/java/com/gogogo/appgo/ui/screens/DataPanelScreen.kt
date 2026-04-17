package com.gogogo.appgo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gogogo.appgo.ui.AppUiState
import com.gogogo.appgo.ui.common.SensorStatusDot
import com.gogogo.appgo.ui.common.azimuthDirection
import com.gogogo.appgo.ui.common.formatDateTime
import kotlin.math.roundToInt

@Composable
fun DataPanelScreen(
    modifier: Modifier = Modifier,
    uiState: AppUiState,
    onAutoCalibrationEnabledChange: (Boolean) -> Unit,
    onToggleCalibrationMode: (Boolean) -> Unit,
    onCalibrateSetNorth: () -> Unit,
    onResetCalibration: () -> Unit,
) {
    var forecastExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("实时数据面板", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("天气信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("来源: ${uiState.weather.source}")
                Text("温度 ${uiState.weather.temperatureC?.let { "%.1f".format(it) } ?: "--"}°C · 体感 ${uiState.weather.feelsLikeC?.let { "%.1f".format(it) } ?: "--"}°C")
                Text("降水 ${uiState.weather.precipitationMm?.let { "%.1f".format(it) } ?: "--"} mm · 湿度 ${uiState.weather.humidityPercent ?: "--"}% · 云量 ${uiState.weather.cloudCoverPercent ?: "--"}%")
                Text("风 ${uiState.weather.windSpeedMs?.let { "%.1f".format(it) } ?: "--"} m/s · 阵风 ${uiState.weather.windGustMs?.let { "%.1f".format(it) } ?: "--"} m/s · 风向 ${uiState.weather.windDirectionDegree ?: "--"}°")
                Text("气压 ${uiState.weather.pressureHpa?.let { "%.1f".format(it) } ?: "--"} hPa · 能见度 ${uiState.weather.visibilityKm?.let { "%.1f".format(it) } ?: "--"} km · UV ${uiState.weather.uvIndex?.let { "%.1f".format(it) } ?: "--"}")
                Text("露点 ${uiState.weather.dewPointC?.let { "%.1f".format(it) } ?: "--"}°C · 日出 ${uiState.weather.sunriseTimeText} · 日落 ${uiState.weather.sunsetTimeText}")
                Text(
                    "更新: ${if (uiState.weather.updateTimeMillis > 0) formatDateTime(uiState.weather.updateTimeMillis) else "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = { forecastExpanded = !forecastExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (forecastExpanded) "收起最近三天天气" else "展开最近三天天气")
                }
                if (forecastExpanded) {
                    uiState.weatherForecast3d.forEach { day ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${day.dateText} · ${day.weatherLabel}", fontWeight = FontWeight.SemiBold)
                                Text("温度 ${"%.1f".format(day.minTempC)}°C ~ ${"%.1f".format(day.maxTempC)}°C")
                                Text("降水 ${"%.1f".format(day.precipitationMm)}mm · 风速 ${"%.1f".format(day.windSpeedMs)}m/s")
                                Text("风向 ${day.windDirectionDegree ?: "--"}° · 阵风 ${"%.1f".format(day.windGustMs)}m/s · 能见度 ${"%.1f".format(day.visibilityKm)}km")
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("海拔高度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.currentAltitude.roundToInt()} m", style = MaterialTheme.typography.headlineMedium)
                    SensorStatusDot("海拔传感器", uiState.altitudeWorkStatus)
                }
            }
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("经纬度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("纬度: ${uiState.currentLatitude?.let { "%.6f".format(it) } ?: "--"}", style = MaterialTheme.typography.bodyMedium)
                    Text("经度: ${uiState.currentLongitude?.let { "%.6f".format(it) } ?: "--"}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (uiState.hasLocationFix) "定位状态: 正常" else "定位状态: 未获取",
                        color = if (uiState.hasLocationFix) Color(0xFF2E7D32) else Color(0xFFC62828),
                    )
                    Text("地理位置: ${uiState.currentAddressText.ifBlank { "解析中/不可用" }}", style = MaterialTheme.typography.bodySmall)
                    val mapSource = if (uiState.settings.serviceIntegration.mapUseSystemService) {
                        "手机地图服务"
                    } else if (uiState.settings.serviceIntegration.mapActive) {
                        "外部地图接口"
                    } else {
                        "外部地图接口未启用"
                    }
                    Text("地图来源: $mapSource", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("指南针", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.azimuthDegree.roundToInt()}° · ${azimuthDirection(uiState.azimuthDegree)}", style = MaterialTheme.typography.titleLarge)
                    SensorStatusDot("方位", uiState.azimuthWorkStatus)
                    SensorStatusDot("指南针", uiState.compassWorkStatus)
                    Text("校准偏移: ${"%.1f".format(uiState.settings.compassCalibrationOffsetDegrees)}°", style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("自动校准")
                        Switch(
                            checked = uiState.settings.compassAutoCalibrationEnabled,
                            onCheckedChange = onAutoCalibrationEnabledChange,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onToggleCalibrationMode(!uiState.compassCalibrationMode) }) {
                            Text(if (uiState.compassCalibrationMode) "退出校准" else "校准")
                        }
                        OutlinedButton(onClick = onResetCalibration) { Text("重置") }
                    }
                    if (uiState.compassCalibrationMode) {
                        Button(onClick = onCalibrateSetNorth) { Text("设当前方向为北") }
                    }
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("水平仪", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Pitch ${"%.1f".format(uiState.pitchDegree)}° · Roll ${"%.1f".format(uiState.rollDegree)}°")
                    LevelIndicator(
                        pitchDegree = uiState.pitchDegree,
                        rollDegree = uiState.rollDegree,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelIndicator(
    pitchDegree: Float,
    rollDegree: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.35f
        drawRect(color = Color(0xFFF7FBFC), size = size)
        drawCircle(color = Color(0xFFB0BEC5), radius = radius, center = center, style = Stroke(width = 4f))
        drawLine(color = Color(0xFF90A4AE), start = Offset(center.x - radius, center.y), end = Offset(center.x + radius, center.y), strokeWidth = 2f)
        drawLine(color = Color(0xFF90A4AE), start = Offset(center.x, center.y - radius), end = Offset(center.x, center.y + radius), strokeWidth = 2f)
        val maxOffset = radius * 0.75f
        val bubbleX = center.x + (rollDegree.coerceIn(-30f, 30f) / 30f) * maxOffset
        val bubbleY = center.y + (pitchDegree.coerceIn(-30f, 30f) / 30f) * maxOffset
        val bubbleCentered = kotlin.math.abs(pitchDegree) <= 2f && kotlin.math.abs(rollDegree) <= 2f
        val bubbleColor = if (bubbleCentered) Color(0xFF2E7D32) else Color(0xFFF9A825)
        drawCircle(color = bubbleColor, radius = 14f, center = Offset(bubbleX, bubbleY))
    }
}
