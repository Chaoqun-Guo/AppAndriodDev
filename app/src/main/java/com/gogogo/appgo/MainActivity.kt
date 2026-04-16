package com.gogogo.appgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.StatsSummary
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
                )

                AppDestination.PROFILE -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onToggleBluetooth = viewModel::toggleBluetoothConnection,
                    onUpdateProfile = viewModel::updateProfile,
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
                Text("速度: ${"%.2f".format(uiState.currentSpeedMps)} m/s")
                Text("定位: ${if (uiState.hasLocationFix) "已定位" else "未定位"}${uiState.locationAccuracyMeters?.let { " (±${it.roundToInt()}m)" } ?: ""}")
                if (uiState.recordingStatus == RecordingStatus.AUTO_PAUSED) {
                    Text("连续静止${uiState.lowSpeedSeconds}s，已自动暂停", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp,
            color = Color(0xFFE7F1F3),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("轨迹区域（定位/传感器驱动）\n已记录轨迹点: ${uiState.trackPoints.size}", color = Color(0xFF274450))
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("实时数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DataCell("里程", formatDistance(uiState.distanceMeters))
                    DataCell("用时", formatDuration(uiState.elapsedSeconds))
                    DataCell("配速", formatPace(uiState.averagePaceSecondsPerKm))
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DataCell("海拔", "${uiState.currentAltitude.roundToInt()} m")
                    DataCell("爬升", "${uiState.totalElevationGain.roundToInt()} m")
                    DataCell("心率", uiState.currentHeartRate?.let { "$it bpm" } ?: "--")
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
                Text("离线地图包管理: MVP占位")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("安全", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = emergency, onValueChange = { emergency = it }, label = { Text("紧急联系人") }, modifier = Modifier.fillMaxWidth())
                Text("实时位置共享: 待接入后端链接服务")
                Text("一键求助: 长按按钮触发短信发送（MVP占位）")
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
