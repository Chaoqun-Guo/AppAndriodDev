package com.gogogo.appgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.BackupCloudProvider
import com.gogogo.appgo.model.BackupStrategyTemplate
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.RecordingMetric
import com.gogogo.appgo.model.HeartRateZoneStat
import com.gogogo.appgo.model.PrivacyMode
import com.gogogo.appgo.model.SensorWorkStatus
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.TrackPoint
import com.gogogo.appgo.model.UserProfile
import com.gogogo.appgo.ui.AppUiState
import com.gogogo.appgo.ui.AppViewModel
import com.gogogo.appgo.ui.theme.AppGoTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGoTheme {
                AppGoApp()
            }
        }
    }
}

@Composable
fun AppGoApp(viewModel: AppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination },
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestination.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onSelectType = viewModel::selectExerciseType,
                    onStart = {
                        viewModel.startWorkout()
                        currentDestination = AppDestination.RECORD
                    },
                )

                AppDestination.RECORD -> RecordingScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onPauseResume = viewModel::togglePause,
                    onEnd = viewModel::endWorkout,
                    onAddMarker = viewModel::addMarker,
                )

                AppDestination.HISTORY -> HistoryScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onOpenDetail = viewModel::openWorkoutDetail,
                    onShiftWeek = viewModel::shiftWeek,
                    onShiftMonth = viewModel::shiftMonth,
                    onShiftYear = viewModel::shiftYear,
                    onShiftHistoryDate = viewModel::shiftHistoryDate,
                    onSetWeekDate = viewModel::setWeekAnchorDate,
                    onSetMonth = viewModel::setMonthAnchor,
                    onSetYear = viewModel::setYearAnchor,
                    onSetHistoryDate = viewModel::setHistoryFilterDate,
                    onSetHistoryTypeFilter = viewModel::setHistoryTypeFilter,
                )

                AppDestination.PROFILE -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onToggleBluetooth = viewModel::toggleBluetoothConnection,
                    onUpdateProfile = viewModel::updateProfile,
                )

                AppDestination.PANEL -> DataPanelScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                )

                AppDestination.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onMetricVisibleChange = viewModel::setMetricVisible,
                    onHomePinEnabledChange = viewModel::setHomePinEnabled,
                    onHomePinTextChange = viewModel::updateHomePinText,
                    onShowTrackAreaChange = viewModel::setShowTrackArea,
                    onWeeklyGoalKmChange = viewModel::setWeeklyGoalKm,
                    onDefaultPrivacyModeChange = viewModel::setDefaultPrivacyMode,
                    onMapEnabledChange = viewModel::setMapEnabled,
                    onMapApiKeyChange = viewModel::updateMapApiKey,
                    onWeatherEnabledChange = viewModel::setWeatherEnabled,
                    onWeatherApiKeyChange = viewModel::updateWeatherApiKey,
                    onShareEnabledChange = viewModel::setShareEnabled,
                    onShareApiKeyChange = viewModel::updateShareApiKey,
                    onLocalBackupEnabledChange = viewModel::setLocalBackupEnabled,
                    onCloudProviderChange = viewModel::setCloudProvider,
                    onCloudBackupEnabledChange = viewModel::setCloudBackupEnabled,
                    onCloudApiKeyChange = viewModel::updateCloudApiKey,
                    onCloudSecretChange = viewModel::updateCloudSecret,
                    onCloudBucketOrPathChange = viewModel::updateCloudBucketOrPath,
                    onCloudEndpointChange = viewModel::updateCloudEndpoint,
                    onTemplateChange = viewModel::applyBackupTemplate,
                    onBackupAutoEnabledChange = viewModel::setBackupAutoEnabled,
                    onBackupIntervalHoursChange = viewModel::updateBackupIntervalHours,
                    onBackupRetainDaysChange = viewModel::updateBackupRetainDays,
                    onBackupWifiOnlyChange = viewModel::setBackupWifiOnly,
                    onBackupChargingOnlyChange = viewModel::setBackupChargingOnly,
                    onRunManualBackup = viewModel::runManualBackup,
                    onRefreshBackupFiles = viewModel::refreshBackupFiles,
                    onSelectBackupFile = viewModel::openBackupFile,
                    onBackupFileContentChange = viewModel::updateBackupFileContent,
                    onSaveBackupFileContent = viewModel::saveBackupFileContent,
                )
            }
        }
    }

    if (uiState.summaryDialogVisible && uiState.pendingRecord != null) {
        SummaryDialog(
            uiState = uiState,
            onSave = {
                viewModel.saveWorkout()
                currentDestination = AppDestination.HISTORY
            },
            onDiscard = viewModel::discardWorkout,
        )
    }
}

enum class AppDestination(val label: String, val icon: Int) {
    HOME("运动", R.drawable.ic_home),
    RECORD("记录", R.drawable.ic_favorite),
    HISTORY("历史", R.drawable.ic_favorite),
    PROFILE("我的", R.drawable.ic_account_box),
    PANEL("面板", R.drawable.ic_home),
    SETTINGS("设置", R.drawable.ic_account_box),
}

private enum class HistoryPickerTarget {
    WEEK, MONTH, YEAR, HISTORY
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: AppUiState,
    onSelectType: (ExerciseType) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E293B), Color(0xFF164A67), Color(0xFFEBF4F6))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "户外运动记录",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "运动选项与开始按钮位于下半区域，便于单手操作",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD6E8F0),
        )

        if (uiState.settings.homePinEnabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.settings.homePinText.ifBlank { "GO!" },
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val goalMeters = uiState.settings.weeklyGoalKm * 1000.0
                val progress = if (goalMeters > 0) (uiState.weeklyDistanceMeters / goalMeters).coerceIn(0.0, 1.0) else 0.0
                Text("本周目标进度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("目标 ${uiState.settings.weeklyGoalKm} km · 已完成 ${formatDistance(uiState.weeklyDistanceMeters)}")
                Text("进度 ${(progress * 100).roundToInt()}%")
            }
        }

        uiState.lastWorkout?.let { summary ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("最近一次运动", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("类型: ${summary.record.type.label}")
                    Text("距离: ${formatDistance(summary.record.totalDistanceMeters)}")
                    Text("时长: ${formatDuration(summary.record.duration.seconds)}")
                    Text("爬升: ${summary.record.totalElevationGainMeters.roundToInt()} m")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("选择运动类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExerciseType.entries) { type ->
                        AssistChip(
                            onClick = { onSelectType(type) },
                            label = { Text(type.label) },
                            border = if (uiState.selectedType == type) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        )
                    }
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    onClick = onStart,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("开始 ${uiState.selectedType.label}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RecordingScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onPauseResume: () -> Unit,
    onEnd: () -> Unit,
    onAddMarker: () -> Unit,
) {
    val isRunning = uiState.recordingStatus == RecordingStatus.RECORDING

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("运动状态: ${statusText(uiState.recordingStatus)}", fontWeight = FontWeight.SemiBold)
                Text("运动判定: ${if (uiState.movementDetected) "检测到运动" else "静止"}")
                Text("陀螺仪: ${"%.3f".format(uiState.gyroRadPerSec)} rad/s")
                Text("方位角: ${uiState.azimuthDegree.roundToInt()}°")
                Text("速度: ${"%.2f".format(uiState.currentSpeedMps)} m/s")
                Text("定位: ${if (uiState.hasLocationFix) "已定位" else "未定位"}${uiState.locationAccuracyMeters?.let { " (±${it.roundToInt()}m)" } ?: ""}")
                if (uiState.recordingStatus == RecordingStatus.AUTO_PAUSED) {
                    Text("连续静止${uiState.lowSpeedSeconds}s，已自动暂停", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("传感器工作状态", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SensorWorkStatusItem("陀螺仪", uiState.gyroWorkStatus)
                    SensorWorkStatusItem("方位", uiState.azimuthWorkStatus)
                    SensorWorkStatusItem("指南针", uiState.compassWorkStatus)
                    SensorWorkStatusItem("海拔", uiState.altitudeWorkStatus)
                }
            }
        }

        if (uiState.settings.showTrackArea) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp,
                color = Color(0xFFE7F1F3),
            ) {
                TrackMapCanvas(
                    trackPoints = uiState.trackPoints,
                    modifier = Modifier.fillMaxSize(),
                )
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("实时数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val metrics = buildMetricItems(uiState)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                onClick = onPauseResume,
            ) {
                Text(if (isRunning) "暂停" else "继续")
            }
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                onClick = onAddMarker,
            ) {
                Text("标记")
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
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
private fun SensorWorkStatusItem(title: String, status: SensorWorkStatus) {
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

@Composable
private fun DataPanelScreen(
    modifier: Modifier = Modifier,
    uiState: AppUiState,
) {
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
                Text("海拔高度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${uiState.currentAltitude.roundToInt()} m", style = MaterialTheme.typography.headlineMedium)
                SensorWorkStatusItem("海拔传感器", uiState.altitudeWorkStatus)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("经纬度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "纬度: ${uiState.currentLatitude?.let { "%.6f".format(it) } ?: "--"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "经度: ${uiState.currentLongitude?.let { "%.6f".format(it) } ?: "--"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (uiState.hasLocationFix) "定位状态: 正常" else "定位状态: 未获取",
                    color = if (uiState.hasLocationFix) Color(0xFF2E7D32) else Color(0xFFC62828),
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("指南针", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${uiState.azimuthDegree.roundToInt()}° · ${azimuthDirection(uiState.azimuthDegree)}", style = MaterialTheme.typography.headlineMedium)
                SensorWorkStatusItem("方位", uiState.azimuthWorkStatus)
                SensorWorkStatusItem("指南针", uiState.compassWorkStatus)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("水平仪", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Pitch ${"%.1f".format(uiState.pitchDegree)}° · Roll ${"%.1f".format(uiState.rollDegree)}°")
                LevelIndicator(
                    pitchDegree = uiState.pitchDegree,
                    rollDegree = uiState.rollDegree,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
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
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 2f,
        )

        val maxOffset = radius * 0.75f
        val bubbleX = center.x + (rollDegree.coerceIn(-30f, 30f) / 30f) * maxOffset
        val bubbleY = center.y + (pitchDegree.coerceIn(-30f, 30f) / 30f) * maxOffset
        val bubbleCentered = kotlin.math.abs(pitchDegree) <= 2f && kotlin.math.abs(rollDegree) <= 2f
        val bubbleColor = if (bubbleCentered) Color(0xFF2E7D32) else Color(0xFFF9A825)
        drawCircle(color = bubbleColor, radius = 14f, center = Offset(bubbleX, bubbleY))
    }
}

@Composable
private fun HistoryScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onOpenDetail: (Long) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onShiftYear: (Long) -> Unit,
    onShiftHistoryDate: (Long) -> Unit,
    onSetWeekDate: (LocalDate) -> Unit,
    onSetMonth: (YearMonth) -> Unit,
    onSetYear: (Int) -> Unit,
    onSetHistoryDate: (LocalDate) -> Unit,
    onSetHistoryTypeFilter: (ExerciseType?) -> Unit,
) {
    var pickerTarget by remember { mutableStateOf<HistoryPickerTarget?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("历史与统计", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        DateSwitchRow(
            title = "周统计",
            value = weekDisplay(uiState.weekAnchorDate),
            onPrev = { onShiftWeek(-1) },
            onNext = { onShiftWeek(1) },
            onPickDate = { pickerTarget = HistoryPickerTarget.WEEK },
        )
        StatsCard(summary = uiState.weeklyStats)

        DateSwitchRow(
            title = "月统计",
            value = monthDisplay(uiState.monthAnchor),
            onPrev = { onShiftMonth(-1) },
            onNext = { onShiftMonth(1) },
            onPickDate = { pickerTarget = HistoryPickerTarget.MONTH },
        )
        StatsCard(summary = uiState.monthlyStats)

        DateSwitchRow(
            title = "年统计",
            value = "${uiState.yearAnchor}",
            onPrev = { onShiftYear(-1) },
            onNext = { onShiftYear(1) },
            onPickDate = { pickerTarget = HistoryPickerTarget.YEAR },
        )
        StatsCard(summary = uiState.yearlyStats)

        DateSwitchRow(
            title = "历史记录日期",
            value = uiState.historyFilterDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            onPrev = { onShiftHistoryDate(-1) },
            onNext = { onShiftHistoryDate(1) },
            onPickDate = { pickerTarget = HistoryPickerTarget.HISTORY },
        )

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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filteredHistory) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenDetail(item.record.id) },
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${item.record.type.label} · ${formatDate(item.record.startTimeMillis)}")
                        Text("距离 ${formatDistance(item.record.totalDistanceMeters)}  用时 ${formatDuration(item.record.duration.seconds)}")
                        Text("爬升 ${item.record.totalElevationGainMeters.roundToInt()}m  轨迹点 ${item.trackPointCount}")
                    }
                }
            }
            if (uiState.selectedWorkoutSummary != null) {
                item {
                    ActivityDetailSection(uiState = uiState)
                }
            }
            if (uiState.filteredHistory.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "该日期暂无运动记录",
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }
    }

    pickerTarget?.let { target ->
        val initialDate = when (target) {
            HistoryPickerTarget.WEEK -> uiState.weekAnchorDate
            HistoryPickerTarget.MONTH -> uiState.monthAnchor.atDay(1)
            HistoryPickerTarget.YEAR -> LocalDate.of(uiState.yearAnchor, 1, 1)
            HistoryPickerTarget.HISTORY -> uiState.historyFilterDate
        }
        DatePickerSelectionDialog(
            title = "选择日期",
            initialDate = initialDate,
            onDismiss = { pickerTarget = null },
            onConfirm = { picked ->
                when (target) {
                    HistoryPickerTarget.WEEK -> onSetWeekDate(picked)
                    HistoryPickerTarget.MONTH -> onSetMonth(YearMonth.from(picked))
                    HistoryPickerTarget.YEAR -> onSetYear(picked.year)
                    HistoryPickerTarget.HISTORY -> onSetHistoryDate(picked)
                }
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun DateSwitchRow(
    title: String,
    value: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPrev) { Text("上一") }
                Text(value)
                OutlinedButton(onClick = onNext) { Text("下一") }
                OutlinedButton(onClick = onPickDate) { Text("选择日期") }
            }
        }
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
private fun StatsCard(summary: StatsSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("总距离: ${formatDistance(summary.totalDistanceMeters)}")
            Text("总时长: ${formatDuration(summary.totalDurationSeconds)}")
            Text("累计爬升: ${summary.totalElevationGainMeters.roundToInt()} m")
            Text("卡路里估算: ${summary.estimatedCalories} kcal")
        }
    }
}

@Composable
private fun ActivityDetailSection(uiState: AppUiState) {
    val summary = uiState.selectedWorkoutSummary ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("活动详情分析", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${summary.record.type.label} · ${formatDate(summary.record.startTimeMillis)}")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F6F8),
            ) {
                TrackMapCanvas(
                    trackPoints = uiState.selectedWorkoutTrack,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Text("配速曲线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LineChart(
                values = uiState.selectedWorkoutPaceSeries,
                color = Color(0xFF1565C0),
                valueFormatter = { sec -> formatPace(sec.roundToInt()) },
            )

            Text("心率曲线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LineChart(
                values = uiState.selectedWorkoutHeartRateSeries,
                color = Color(0xFFD32F2F),
                valueFormatter = { bpm -> "${bpm.roundToInt()} bpm" },
            )

            Text("心率区间分布", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HeartRateZoneChart(zones = uiState.selectedWorkoutHeartRateZones)

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
private fun TrackMapCanvas(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
) {
    if (trackPoints.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("轨迹点不足，等待记录中...")
        }
        return
    }

    val lats = trackPoints.map { it.latitude }
    val lons = trackPoints.map { it.longitude }
    val minLat = lats.minOrNull() ?: 0.0
    val maxLat = lats.maxOrNull() ?: 0.0
    val minLon = lons.minOrNull() ?: 0.0
    val maxLon = lons.maxOrNull() ?: 0.0

    Canvas(modifier = modifier) {
        val pad = 24f
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        drawRect(color = Color(0xFFEAF2F5), size = size)

        val latSpan = (maxLat - minLat).takeIf { it > 1e-8 } ?: 1e-8
        val lonSpan = (maxLon - minLon).takeIf { it > 1e-8 } ?: 1e-8

        fun toOffset(point: TrackPoint): Offset {
            val x = ((point.longitude - minLon) / lonSpan).toFloat() * w + pad
            val y = (1f - ((point.latitude - minLat) / latSpan).toFloat()) * h + pad
            return Offset(x, y)
        }

        val path = Path()
        val first = toOffset(trackPoints.first())
        path.moveTo(first.x, first.y)
        trackPoints.drop(1).forEach { p ->
            val o = toOffset(p)
            path.lineTo(o.x, o.y)
        }

        drawPath(
            path = path,
            color = Color(0xFF0D6EFD),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
        )

        val start = toOffset(trackPoints.first())
        val end = toOffset(trackPoints.last())
        drawCircle(color = Color(0xFF2E7D32), radius = 8f, center = start)
        drawCircle(color = Color(0xFFD32F2F), radius = 8f, center = end)
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

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onToggleBluetooth: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
) {
    var height by remember(uiState.profile) { mutableStateOf(uiState.profile.heightCm.toString()) }
    var weight by remember(uiState.profile) { mutableStateOf(uiState.profile.weightKg.toString()) }
    var age by remember(uiState.profile) { mutableStateOf(uiState.profile.age.toString()) }
    var preference by remember(uiState.profile) { mutableStateOf(uiState.profile.preferences) }
    var emergency by remember(uiState.profile) { mutableStateOf(uiState.profile.emergencyContact) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("个人信息", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("身高(cm)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("体重(kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("年龄") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = preference, onValueChange = { preference = it }, label = { Text("运动偏好") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onUpdateProfile(
                            UserProfile(
                                heightCm = height.toIntOrNull() ?: 170,
                                weightKg = weight.toIntOrNull() ?: 65,
                                age = age.toIntOrNull() ?: 28,
                                preferences = preference,
                                emergencyContact = emergency,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("保存个人信息")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("设备", fontWeight = FontWeight.SemiBold)
                Text("蓝牙心率带: ${if (uiState.bluetoothConnected) "已连接" else "未连接"}")
                OutlinedButton(onClick = onToggleBluetooth, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.bluetoothConnected) "断开设备" else "连接设备")
                }
                if (uiState.settings.serviceIntegration.mapActive) {
                    Text("离线地图包管理: 已接入地图服务")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("安全", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = emergency, onValueChange = { emergency = it }, label = { Text("紧急联系人") }, modifier = Modifier.fillMaxWidth())
                if (uiState.settings.serviceIntegration.shareActive) {
                    Text("实时位置共享: 已接入共享服务")
                }
                if (uiState.settings.serviceIntegration.weatherActive) {
                    Text("天气安全提醒: 已接入天气服务")
                }
                Text("一键求助: 长按按钮触发短信发送（MVP占位）")
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onMetricVisibleChange: (RecordingMetric, Boolean) -> Unit,
    onHomePinEnabledChange: (Boolean) -> Unit,
    onHomePinTextChange: (String) -> Unit,
    onShowTrackAreaChange: (Boolean) -> Unit,
    onWeeklyGoalKmChange: (Int) -> Unit,
    onDefaultPrivacyModeChange: (PrivacyMode) -> Unit,
    onMapEnabledChange: (Boolean) -> Unit,
    onMapApiKeyChange: (String) -> Unit,
    onWeatherEnabledChange: (Boolean) -> Unit,
    onWeatherApiKeyChange: (String) -> Unit,
    onShareEnabledChange: (Boolean) -> Unit,
    onShareApiKeyChange: (String) -> Unit,
    onLocalBackupEnabledChange: (Boolean) -> Unit,
    onCloudProviderChange: (BackupCloudProvider) -> Unit,
    onCloudBackupEnabledChange: (Boolean) -> Unit,
    onCloudApiKeyChange: (String) -> Unit,
    onCloudSecretChange: (String) -> Unit,
    onCloudBucketOrPathChange: (String) -> Unit,
    onCloudEndpointChange: (String) -> Unit,
    onTemplateChange: (BackupStrategyTemplate) -> Unit,
    onBackupAutoEnabledChange: (Boolean) -> Unit,
    onBackupIntervalHoursChange: (Int) -> Unit,
    onBackupRetainDaysChange: (Int) -> Unit,
    onBackupWifiOnlyChange: (Boolean) -> Unit,
    onBackupChargingOnlyChange: (Boolean) -> Unit,
    onRunManualBackup: () -> Unit,
    onRefreshBackupFiles: () -> Unit,
    onSelectBackupFile: (String) -> Unit,
    onBackupFileContentChange: (String) -> Unit,
    onSaveBackupFileContent: () -> Unit,
) {
    val services = uiState.settings.serviceIntegration
    val backup = uiState.settings.backupConfig
    var weeklyGoalInput by remember(uiState.settings.weeklyGoalKm) { mutableStateOf(uiState.settings.weeklyGoalKm.toString()) }
    var intervalHoursInput by remember(backup.strategy.intervalHours) { mutableStateOf(backup.strategy.intervalHours.toString()) }
    var retainDaysInput by remember(backup.strategy.retainDays) { mutableStateOf(backup.strategy.retainDays.toString()) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("运动页面与记录页", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("显示个性化 PIN")
                    Switch(
                        checked = uiState.settings.homePinEnabled,
                        onCheckedChange = onHomePinEnabledChange,
                    )
                }
                OutlinedTextField(
                    value = uiState.settings.homePinText,
                    onValueChange = onHomePinTextChange,
                    label = { Text("PIN 文本") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("记录页显示轨迹区域")
                    Switch(
                        checked = uiState.settings.showTrackArea,
                        onCheckedChange = onShowTrackAreaChange,
                    )
                }
                OutlinedTextField(
                    value = weeklyGoalInput,
                    onValueChange = {
                        weeklyGoalInput = it
                        it.toIntOrNull()?.let(onWeeklyGoalKmChange)
                    },
                    label = { Text("每周目标里程 (km)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("默认隐私等级", style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PrivacyMode.entries) { mode ->
                        AssistChip(
                            onClick = { onDefaultPrivacyModeChange(mode) },
                            label = { Text(mode.label) },
                            border = if (uiState.settings.defaultPrivacyMode == mode) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("记录页展示项", fontWeight = FontWeight.SemiBold)
                Text("勾选后才会在记录页展示。至少保留一个字段。", style = MaterialTheme.typography.bodySmall)
                RecordingMetric.entries.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { metric ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = uiState.settings.visibleMetrics.contains(metric),
                                    onCheckedChange = { checked -> onMetricVisibleChange(metric, checked) },
                                )
                                Text(metric.label)
                            }
                        }
                    }
                }
            }
        }

        ServiceIntegrationCard(
            title = "地图服务",
            enabled = services.mapEnabled,
            apiKey = services.mapApiKey,
            isActive = services.mapActive,
            hint = "用于地图渲染/离线地图服务",
            onEnabledChange = onMapEnabledChange,
            onApiKeyChange = onMapApiKeyChange,
        )

        ServiceIntegrationCard(
            title = "天气服务",
            enabled = services.weatherEnabled,
            apiKey = services.weatherApiKey,
            isActive = services.weatherActive,
            hint = "用于天气与环境提醒",
            onEnabledChange = onWeatherEnabledChange,
            onApiKeyChange = onWeatherApiKeyChange,
        )

        ServiceIntegrationCard(
            title = "轨迹共享服务",
            enabled = services.shareEnabled,
            apiKey = services.shareApiKey,
            isActive = services.shareActive,
            hint = "用于实时位置与轨迹分享",
            onEnabledChange = onShareEnabledChange,
            onApiKeyChange = onShareApiKeyChange,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("备份配置", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("启用本地备份")
                    Switch(
                        checked = backup.localBackupEnabled,
                        onCheckedChange = onLocalBackupEnabledChange,
                    )
                }
                Text("云端提供商", style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BackupCloudProvider.entries) { provider ->
                        AssistChip(
                            onClick = { onCloudProviderChange(provider) },
                            label = { Text(provider.label) },
                            border = if (backup.cloudConfig.provider == provider) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        )
                    }
                }

                if (backup.cloudConfig.provider != BackupCloudProvider.NONE) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("启用云端备份")
                        Switch(
                            checked = backup.cloudConfig.isActive,
                            onCheckedChange = onCloudBackupEnabledChange,
                            enabled = backup.cloudConfig.isConfigured,
                        )
                    }
                    OutlinedTextField(
                        value = backup.cloudConfig.apiKey,
                        onValueChange = onCloudApiKeyChange,
                        label = { Text("Cloud API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = backup.cloudConfig.secret,
                        onValueChange = onCloudSecretChange,
                        label = { Text("Cloud Secret (可选)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = backup.cloudConfig.bucketOrPath,
                        onValueChange = onCloudBucketOrPathChange,
                        label = { Text("Bucket / Path") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = backup.cloudConfig.endpoint,
                        onValueChange = onCloudEndpointChange,
                        label = { Text("Endpoint (可选)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (!backup.cloudConfig.isConfigured) {
                        Text("云端未配置完成，不会展示/执行云备份。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("备份策略模板", style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BackupStrategyTemplate.entries) { template ->
                        AssistChip(
                            onClick = { onTemplateChange(template) },
                            label = { Text(template.label) },
                            border = if (backup.strategyTemplate == template) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("自动备份")
                    Switch(checked = backup.strategy.autoBackupEnabled, onCheckedChange = onBackupAutoEnabledChange)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("仅 Wi-Fi")
                    Switch(checked = backup.strategy.wifiOnly, onCheckedChange = onBackupWifiOnlyChange)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("仅充电时")
                    Switch(checked = backup.strategy.chargingOnly, onCheckedChange = onBackupChargingOnlyChange)
                }
                OutlinedTextField(
                    value = intervalHoursInput,
                    onValueChange = {
                        intervalHoursInput = it
                        it.toIntOrNull()?.let(onBackupIntervalHoursChange)
                    },
                    label = { Text("备份间隔（小时）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = retainDaysInput,
                    onValueChange = {
                        retainDaysInput = it
                        it.toIntOrNull()?.let(onBackupRetainDaysChange)
                    },
                    label = { Text("保留天数") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Button(
                    onClick = onRunManualBackup,
                    enabled = !uiState.backupInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.backupInProgress) "备份执行中..." else "立即备份")
                }
                backup.lastBackupTimeMillis?.let { ts ->
                    Text("最近备份: ${formatDate(ts)}", style = MaterialTheme.typography.bodySmall)
                }
                if (backup.lastBackupResult.isNotBlank()) {
                    Text("结果: ${backup.lastBackupResult}", style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("历史备份文件", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = onRefreshBackupFiles) { Text("刷新") }
                }
                if (uiState.backupFiles.isEmpty()) {
                    Text("暂无备份文件。")
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.backupFiles) { file ->
                            AssistChip(
                                onClick = { onSelectBackupFile(file) },
                                label = { Text(file) },
                                border = if (uiState.selectedBackupFileName == file) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = uiState.selectedBackupFileContent,
                        onValueChange = onBackupFileContentChange,
                        label = { Text("备份文件内容（可编辑）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                    Button(
                        onClick = onSaveBackupFileContent,
                        enabled = uiState.selectedBackupFileName != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存当前备份文件")
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceIntegrationCard(
    title: String,
    enabled: Boolean,
    apiKey: String,
    isActive: Boolean,
    hint: String,
    onEnabledChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = enabled && apiKey.isNotBlank(),
                    onCheckedChange = { onEnabledChange(it) },
                    enabled = apiKey.isNotBlank(),
                )
            }
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("请输入 API Key") },
                singleLine = true,
            )
            Text(hint, style = MaterialTheme.typography.bodySmall)
            if (apiKey.isBlank()) {
                Text("未提供 API Key，不显示该服务相关内容。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            } else {
                Text(if (isActive) "状态: 已启用" else "状态: 已提供 key，未启用", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SummaryDialog(
    uiState: AppUiState,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val pending = uiState.pendingRecord ?: return
    AlertDialog(
        onDismissRequest = {},
        title = { Text("运动摘要") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("类型: ${pending.type.label}")
                Text("距离: ${formatDistance(pending.totalDistanceMeters)}")
                Text("时长: ${formatDuration(pending.duration.seconds)}")
                Text("爬升: ${pending.totalElevationGainMeters.roundToInt()} m")
                Text("平均心率: ${pending.averageHeartRate ?: "--"} bpm")
                Text("标记点: ${uiState.markerPoints.size}")
                Text("隐私: ${uiState.settings.defaultPrivacyMode.label}")
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDiscard) { Text("丢弃") }
        },
    )
}

private fun statusText(status: RecordingStatus): String = when (status) {
    RecordingStatus.IDLE -> "未开始"
    RecordingStatus.RECORDING -> "记录中"
    RecordingStatus.PAUSED -> "手动暂停"
    RecordingStatus.AUTO_PAUSED -> "自动暂停"
    RecordingStatus.FINISHED -> "待保存"
}

private fun buildMetricItems(uiState: AppUiState): List<Pair<String, String>> {
    val all = mapOf(
        RecordingMetric.DISTANCE to formatDistance(uiState.distanceMeters),
        RecordingMetric.DURATION to formatDuration(uiState.elapsedSeconds),
        RecordingMetric.PACE to formatPace(uiState.averagePaceSecondsPerKm),
        RecordingMetric.ALTITUDE to "${uiState.currentAltitude.roundToInt()} m",
        RecordingMetric.ELEVATION to "${uiState.totalElevationGain.roundToInt()} m",
        RecordingMetric.HEART_RATE to (uiState.currentHeartRate?.let { "$it bpm" } ?: "--"),
        RecordingMetric.SPEED to "${"%.2f".format(uiState.currentSpeedMps)} m/s",
        RecordingMetric.MOVEMENT to if (uiState.movementDetected) "运动中" else "静止",
    )
    return RecordingMetric.entries
        .filter { uiState.settings.visibleMetrics.contains(it) }
        .map { metric -> metric.label to (all[metric] ?: "--") }
}

private fun weekDisplay(anchor: LocalDate): String {
    val monday = anchor.minusDays(anchor.dayOfWeek.value.toLong() - 1)
    val sunday = monday.plusDays(6)
    val fmt = DateTimeFormatter.ofPattern("MM-dd")
    return "${monday.format(fmt)} ~ ${sunday.format(fmt)}"
}

private fun monthDisplay(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))

private fun formatDistance(distanceMeters: Double): String =
    if (distanceMeters >= 1000) "${"%.2f".format(distanceMeters / 1000.0)} km" else "${distanceMeters.roundToInt()} m"

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatPace(secondsPerKm: Int): String {
    if (secondsPerKm <= 0) return "--"
    val min = secondsPerKm / 60
    val sec = secondsPerKm % 60
    return "%d'%02d\"/km".format(min, sec)
}

private fun formatDate(millis: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime())

private fun localDateToEpochMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun epochMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun azimuthDirection(azimuth: Float): String {
    val normalized = ((azimuth % 360) + 360) % 360
    return when (normalized) {
        in 337.5f..360f, in 0f..22.5f -> "北"
        in 22.5f..67.5f -> "东北"
        in 67.5f..112.5f -> "东"
        in 112.5f..157.5f -> "东南"
        in 157.5f..202.5f -> "南"
        in 202.5f..247.5f -> "西南"
        in 247.5f..292.5f -> "西"
        else -> "西北"
    }
}
