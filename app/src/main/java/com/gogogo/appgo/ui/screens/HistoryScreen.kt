package com.gogogo.appgo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gogogo.appgo.model.DetailReplayDirection
import com.gogogo.appgo.model.DetailReplayPoint
import com.gogogo.appgo.model.DetailReplaySpeed
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.HeartRateZoneStat
import com.gogogo.appgo.model.HistoryDisplayField
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.WorkoutSummary
import com.gogogo.appgo.ui.AppUiState
import com.gogogo.appgo.ui.common.formatDateTime
import com.gogogo.appgo.ui.common.formatDistance
import com.gogogo.appgo.ui.common.formatDuration
import com.gogogo.appgo.ui.common.formatPace
import com.gogogo.appgo.ui.common.weekDisplay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class HistoryPickerTarget {
    CUSTOM_START, CUSTOM_END
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun HistoryScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onOpenDetail: (Long) -> Unit,
    onSetCustomRangeStartDate: (LocalDate) -> Unit,
    onSetCustomRangeEndDate: (LocalDate) -> Unit,
    onSetHistoryTypeFilter: (ExerciseType?) -> Unit,
    onLoadMoreHistory: () -> Unit,
    onCloseWorkoutDetail: () -> Unit,
    onDetailMapLongPress: (Double, Double) -> Unit,
    onDetailReplayForward: () -> Unit,
    onDetailReplayReverse: () -> Unit,
    onDetailReplayTogglePlayPause: () -> Unit,
    onDetailReplaySpeedChange: (DetailReplaySpeed) -> Unit,
    onDetailReplaySwitchDirection: () -> Unit,
    onDetailReplaySeek: (Float) -> Unit,
    onDetailReplayExit: () -> Unit,
    onDetailReplayNoticeConsumed: () -> Unit,
) {
    var pickerTarget by remember { mutableStateOf<HistoryPickerTarget?>(null) }
    val recentRecords = uiState.history
        .asSequence()
        .filter { uiState.historyTypeFilter == null || it.record.type == uiState.historyTypeFilter }
        .sortedByDescending { it.record.startTimeMillis }
        .take(uiState.historyVisibleCount)
        .toList()
    val totalFilteredRecords = uiState.history.count { uiState.historyTypeFilter == null || it.record.type == uiState.historyTypeFilter }

    if (uiState.selectedWorkoutSummary != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("记录详情", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onCloseWorkoutDetail) { Text("返回历史") }
            }
            ActivityDetailSection(
                uiState = uiState,
                onCloseWorkoutDetail = onCloseWorkoutDetail,
                onMapLongPress = onDetailMapLongPress,
                onReplayForward = onDetailReplayForward,
                onReplayReverse = onDetailReplayReverse,
                onReplayTogglePlayPause = onDetailReplayTogglePlayPause,
                onReplaySpeedChange = onDetailReplaySpeedChange,
                onReplaySwitchDirection = onDetailReplaySwitchDirection,
                onReplaySeek = onDetailReplaySeek,
                onReplayExit = onDetailReplayExit,
                onReplayNoticeConsumed = onDetailReplayNoticeConsumed,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("历史与统计", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Text("运动类型筛选", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AssistChip(
                    onClick = { onSetHistoryTypeFilter(null) },
                    label = { Text("全部") },
                    border = if (uiState.historyTypeFilter == null) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                )
            }
            items(ExerciseType.entries) { type ->
                AssistChip(
                    onClick = { onSetHistoryTypeFilter(type) },
                    label = { Text(type.label) },
                    border = if (uiState.historyTypeFilter == type) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("自定义时间统计区间", fontWeight = FontWeight.SemiBold)
                Text("默认区间: 今天（可修改）")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickerTarget = HistoryPickerTarget.CUSTOM_START }) {
                        Text("开始: ${uiState.customRangeStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                    }
                    OutlinedButton(onClick = { pickerTarget = HistoryPickerTarget.CUSTOM_END }) {
                        Text("结束: ${uiState.customRangeEndDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                    }
                }
                Text("自定义区间统计", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                StatsSummaryRows(uiState.customRangeStats)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatsTile(
                modifier = Modifier.weight(1f),
                title = "日统计",
                subtitle = uiState.historyFilterDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                summary = uiState.dailyStats,
            )
            StatsTile(
                modifier = Modifier.weight(1f),
                title = "周统计",
                subtitle = weekDisplay(uiState.weekAnchorDate),
                summary = uiState.weeklyStats,
            )
            StatsTile(
                modifier = Modifier.weight(1f),
                title = "年统计",
                subtitle = "${uiState.yearAnchor}",
                summary = uiState.yearlyStats,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("最近记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (recentRecords.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "暂无运动记录",
                        modifier = Modifier.padding(14.dp),
                    )
                }
            } else {
                recentRecords.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenDetail(item.record.id) },
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${item.record.type.label} · ${formatDateTime(item.record.startTimeMillis)}")
                            HistorySummaryFields(summary = item, uiState = uiState)
                        }
                    }
                }
                if (recentRecords.size < totalFilteredRecords) {
                    OutlinedButton(onClick = onLoadMoreHistory, modifier = Modifier.fillMaxWidth()) {
                        Text("更多")
                    }
                }
            }
        }
    }

    pickerTarget?.let { target ->
        val initialDate = when (target) {
            HistoryPickerTarget.CUSTOM_START -> uiState.customRangeStartDate
            HistoryPickerTarget.CUSTOM_END -> uiState.customRangeEndDate
        }
        DatePickerSelectionDialog(
            title = "选择日期",
            initialDate = initialDate,
            onDismiss = { pickerTarget = null },
            onConfirm = { picked ->
                when (target) {
                    HistoryPickerTarget.CUSTOM_START -> onSetCustomRangeStartDate(picked)
                    HistoryPickerTarget.CUSTOM_END -> onSetCustomRangeEndDate(picked)
                }
                pickerTarget = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSelectionDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localDateToEpochMillis(initialDate),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(epochMillisToLocalDate(millis))
                    } ?: onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            )
            DatePicker(state = state)
        }
    }
}

@Composable
private fun StatsSummaryRows(summary: StatsSummary) {
    Text("总距离: ${formatDistance(summary.totalDistanceMeters)}")
    Text("总时长: ${formatDuration(summary.totalDurationSeconds)}")
    Text("累计爬升: ${summary.totalElevationGainMeters.roundToInt()} m")
    Text("卡路里估算: ${summary.estimatedCalories} kcal")
}

@Composable
private fun StatsTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    summary: StatsSummary,
) {
    Card(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(formatDistance(summary.totalDistanceMeters), style = MaterialTheme.typography.titleSmall)
            Text(formatDuration(summary.totalDurationSeconds), style = MaterialTheme.typography.bodySmall)
            Text("${summary.totalElevationGainMeters.roundToInt()} m", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HistorySummaryFields(
    summary: WorkoutSummary,
    uiState: AppUiState,
) {
    val fields = uiState.settings.visibleHistoryFields
    if (fields.contains(HistoryDisplayField.DISTANCE)) {
        Text("距离 ${formatDistance(summary.record.totalDistanceMeters)}")
    }
    if (fields.contains(HistoryDisplayField.DURATION)) {
        Text("用时 ${formatDuration(summary.record.duration.seconds)}")
    }
    if (fields.contains(HistoryDisplayField.ELEVATION)) {
        Text("爬升 ${summary.record.totalElevationGainMeters.roundToInt()} m")
    }
    if (fields.contains(HistoryDisplayField.TRACK_POINTS)) {
        Text("轨迹点 ${summary.trackPointCount}")
    }
}

@Composable
private fun ActivityDetailSection(
    uiState: AppUiState,
    onCloseWorkoutDetail: () -> Unit,
    onMapLongPress: (Double, Double) -> Unit,
    onReplayForward: () -> Unit,
    onReplayReverse: () -> Unit,
    onReplayTogglePlayPause: () -> Unit,
    onReplaySpeedChange: (DetailReplaySpeed) -> Unit,
    onReplaySwitchDirection: () -> Unit,
    onReplaySeek: (Float) -> Unit,
    onReplayExit: () -> Unit,
    onReplayNoticeConsumed: () -> Unit,
) {
    val summary = uiState.selectedWorkoutSummary ?: return
    val replayPoints = uiState.selectedWorkoutReplayPoints
    val anchorIndex = uiState.detailReplayAnchorIndex
    val currentIndex = uiState.detailReplayCurrentIndex
    val anchorPoint = replayPoints.getOrNull(anchorIndex)
    val currentPoint = replayPoints.getOrNull(currentIndex)
    val context = LocalContext.current

    LaunchedEffect(uiState.detailReplayNoticeVersion) {
        if (uiState.detailReplayNotice.isNotBlank()) {
            Toast.makeText(context, uiState.detailReplayNotice, Toast.LENGTH_SHORT).show()
            onReplayNoticeConsumed()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("活动详情分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${summary.record.type.label} · ${formatDateTime(summary.record.startTimeMillis)}")
                }
                OutlinedButton(onClick = onCloseWorkoutDetail) { Text("关闭详情") }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                color = Color(0xFFF1F6F8),
            ) {
                DetailReplayMapCanvas(
                    replayPoints = replayPoints,
                    anchorIndex = anchorIndex,
                    currentIndex = currentIndex,
                    direction = uiState.detailReplayDirection,
                    onLongPressLatLon = onMapLongPress,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (replayPoints.size < 2) {
                Text("轨迹数据不足，无法使用回溯功能。", color = MaterialTheme.colorScheme.error)
            }
            if (uiState.detailReplayPanelVisible && anchorPoint != null) {
                val anchorProgress = if (replayPoints.size > 1) anchorPoint.index.toFloat() / replayPoints.lastIndex else 0f
                val currentProgress = if (replayPoints.size > 1 && currentPoint != null) {
                    currentPoint.index.toFloat() / replayPoints.lastIndex
                } else {
                    anchorProgress
                }
                val remainingToEnd = if (currentPoint != null) {
                    (replayPoints.last().cumulativeDistanceMeters - currentPoint.cumulativeDistanceMeters).coerceAtLeast(0.0)
                } else 0.0
                val returnedFromAnchor = if (currentPoint != null) {
                    kotlin.math.abs(currentPoint.cumulativeDistanceMeters - anchorPoint.cumulativeDistanceMeters)
                } else 0.0
                val currentPace = currentPoint?.speedMps?.takeIf { it > 0.3 }?.let { (1000.0 / it).roundToInt() }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("回溯控制面板", fontWeight = FontWeight.SemiBold)
                        Text(
                            "锚点: ${formatDistance(anchorPoint.cumulativeDistanceMeters)} · 海拔 ${anchorPoint.altitude.roundToInt()}m · " +
                                "速度 ${"%.2f".format(anchorPoint.speedMps)}m/s · " +
                                "配速 ${currentPace?.let { formatPace(it) } ?: "--"}"
                        )
                        Text("锚点相对位置")
                        Slider(
                            value = anchorProgress,
                            onValueChange = {},
                            valueRange = 0f..1f,
                            enabled = false,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onReplayForward, enabled = replayPoints.size >= 2) { Text("正向回溯") }
                            OutlinedButton(onClick = onReplayReverse, enabled = replayPoints.size >= 2) { Text("反向回溯") }
                        }
                        Text(
                            if (uiState.detailReplayDirection == DetailReplayDirection.FORWARD) {
                                "剩余距离: ${formatDistance(remainingToEnd)}"
                            } else {
                                "已返回距离: ${formatDistance(returnedFromAnchor)}"
                            }
                        )
                        currentPoint?.let {
                            Text("当前位置: 海拔 ${it.altitude.roundToInt()}m · 速度 ${"%.2f".format(it.speedMps)}m/s")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onReplayTogglePlayPause) {
                                Text(if (uiState.detailReplayPlaying) "暂停" else "播放")
                            }
                            OutlinedButton(onClick = onReplaySwitchDirection) {
                                Text("切换方向")
                            }
                            OutlinedButton(onClick = onReplayExit) {
                                Text("退出")
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(DetailReplaySpeed.entries.toList()) { speed ->
                                AssistChip(
                                    onClick = { onReplaySpeedChange(speed) },
                                    label = { Text(speed.label) },
                                    border = if (uiState.detailReplaySpeed == speed) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                )
                            }
                        }
                        Slider(
                            value = currentProgress.coerceIn(0f, 1f),
                            onValueChange = onReplaySeek,
                            valueRange = 0f..1f,
                        )
                    }
                }
            }

            Text("配速曲线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LineChart(
                values = uiState.selectedWorkoutPaceSeries,
                color = Color(0xFF1565C0),
                valueFormatter = { sec -> formatPace(sec.roundToInt()) },
            )

            Text("分段配速", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (uiState.selectedWorkoutSegmentPace.isEmpty()) {
                Text("暂无分段数据")
            } else {
                uiState.selectedWorkoutSegmentPace.forEach { segment ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("第${segment.segmentIndex}段")
                            Text("${"%.2f".format(segment.distanceMeters / 1000.0)} km")
                            Text(formatDuration(segment.durationSeconds))
                            Text(formatPace(segment.paceSecondsPerKm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailReplayMapCanvas(
    replayPoints: List<DetailReplayPoint>,
    anchorIndex: Int,
    currentIndex: Int,
    direction: DetailReplayDirection,
    onLongPressLatLon: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (replayPoints.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("轨迹数据不足")
        }
        return
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val lats = replayPoints.map { it.latitude }
    val lons = replayPoints.map { it.longitude }
    val minLat = lats.minOrNull() ?: 0.0
    val maxLat = lats.maxOrNull() ?: 0.0
    val minLon = lons.minOrNull() ?: 0.0
    val maxLon = lons.maxOrNull() ?: 0.0
    val latSpan = (maxLat - minLat).takeIf { it > 1e-8 } ?: 1e-8
    val lonSpan = (maxLon - minLon).takeIf { it > 1e-8 } ?: 1e-8
    val pad = 24f

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(replayPoints, canvasSize) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val w = (canvasSize.width.toFloat() - 2 * pad).coerceAtLeast(1f)
                        val h = (canvasSize.height.toFloat() - 2 * pad).coerceAtLeast(1f)
                        val xNorm = ((offset.x - pad) / w).coerceIn(0f, 1f)
                        val yNorm = ((offset.y - pad) / h).coerceIn(0f, 1f)
                        val lon = minLon + xNorm * lonSpan
                        val lat = minLat + (1f - yNorm) * latSpan
                        onLongPressLatLon(lat, lon)
                    },
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width - 2 * pad
            val h = size.height - 2 * pad
            drawRect(color = Color(0xFFEAF2F5), size = size)

            fun toOffset(p: DetailReplayPoint): Offset {
                val x = ((p.longitude - minLon) / lonSpan).toFloat() * w + pad
                val y = (1f - ((p.latitude - minLat) / latSpan).toFloat()) * h + pad
                return Offset(x, y)
            }

            val basePath = Path()
            val first = toOffset(replayPoints.first())
            basePath.moveTo(first.x, first.y)
            replayPoints.drop(1).forEach { p ->
                val o = toOffset(p)
                basePath.lineTo(o.x, o.y)
            }
            drawPath(basePath, color = Color(0xFF9E9E9E), style = Stroke(width = 5f, cap = StrokeCap.Round))

            if (anchorIndex in replayPoints.indices && currentIndex in replayPoints.indices) {
                val start = minOf(anchorIndex, currentIndex)
                val end = maxOf(anchorIndex, currentIndex)
                if (end > start) {
                    val replayPath = Path()
                    val begin = toOffset(replayPoints[start])
                    replayPath.moveTo(begin.x, begin.y)
                    for (i in (start + 1)..end) {
                        val o = toOffset(replayPoints[i])
                        replayPath.lineTo(o.x, o.y)
                    }
                    val replayColor = if (direction == DetailReplayDirection.FORWARD) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                    drawPath(replayPath, color = replayColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                }
            }
            if (anchorIndex in replayPoints.indices) {
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 10f,
                    center = toOffset(replayPoints[anchorIndex]),
                )
            }
            if (currentIndex in replayPoints.indices) {
                drawCircle(
                    color = Color(0xFF1565C0),
                    radius = 9f,
                    center = toOffset(replayPoints[currentIndex]),
                )
            }
        }
    }
}

@Composable
private fun LineChart(
    values: List<Float>,
    color: Color,
    valueFormatter: (Float) -> String,
) {
    if (values.isEmpty()) {
        Text("暂无图表数据")
        return
    }

    val maxVal = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val minVal = values.minOrNull() ?: 0f
    val latest = values.last()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("最新: ${valueFormatter(latest)}", style = MaterialTheme.typography.bodySmall)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val pad = 10f
            val chartW = size.width - 2 * pad
            val chartH = size.height - 2 * pad
            drawRect(color = Color(0xFFF6FAFC), size = size)
            val range = (maxVal - minVal).takeIf { it > 1e-6f } ?: 1f

            val path = Path()
            values.forEachIndexed { index, v ->
                val x = pad + (index.toFloat() / (values.size - 1).coerceAtLeast(1)) * chartW
                val y = pad + (1f - ((v - minVal) / range)) * chartH
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun HeartRateZoneChart(zones: List<HeartRateZoneStat>) {
    val maxSeconds = zones.maxOfOrNull { it.durationSeconds }?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        zones.forEach { zone ->
            val ratio = zone.durationSeconds.toFloat() / maxSeconds.toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${zone.zoneLabel}: ${formatDuration(zone.durationSeconds)}", style = MaterialTheme.typography.bodySmall)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                ) {
                    drawRect(color = Color(0xFFE0E7EB), size = size)
                    drawRect(
                        color = Color(0xFFD32F2F),
                        topLeft = Offset.Zero,
                        size = Size(size.width * ratio.coerceIn(0f, 1f), size.height),
                    )
                }
            }
        }
    }
}

private fun localDateToEpochMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun epochMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
