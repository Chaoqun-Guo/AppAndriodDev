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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Slider
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.DetailReplayDirection
import com.gogogo.appgo.model.DetailReplayPoint
import com.gogogo.appgo.model.DetailReplaySpeed
import com.gogogo.appgo.model.BackupCloudProvider
import com.gogogo.appgo.model.BackupStrategyTemplate
import com.gogogo.appgo.model.BacktrackDirection
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.model.RecordingMetric
import com.gogogo.appgo.model.HeartRateZoneStat
import com.gogogo.appgo.model.HistoryDisplayField
import com.gogogo.appgo.model.SensorWorkStatus
import com.gogogo.appgo.model.StatsSummary
import com.gogogo.appgo.model.TrackPoint
import com.gogogo.appgo.model.UserProfile
import com.gogogo.appgo.ui.AppUiState
import com.gogogo.appgo.ui.AppViewModel
import com.gogogo.appgo.ui.common.formatDistance
import com.gogogo.appgo.ui.common.formatDuration
import com.gogogo.appgo.ui.screens.DataPanelScreen
import com.gogogo.appgo.ui.screens.HistoryScreen
import com.gogogo.appgo.ui.screens.HomeScreen
import com.gogogo.appgo.ui.screens.ProfileScreen
import com.gogogo.appgo.ui.screens.RecordingScreen
import com.gogogo.appgo.ui.theme.AppGoTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.Instant
import java.time.LocalDate
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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.PANEL) }
    val bottomDestinations = orderedBottomDestinations(uiState.settings.bottomNavOrder)

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            bottomDestinations.forEach { destination ->
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
                    onStartWithPathRecordingChange = viewModel::setStartWithPathRecordingOnWorkout,
                    onStart = { enablePathRecording ->
                        viewModel.startWorkout(enablePathRecording)
                        currentDestination = AppDestination.RECORD
                    },
                )

                AppDestination.RECORD -> RecordingScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onResume = viewModel::resumeWorkout,
                    onPause = viewModel::pauseWorkout,
                    onEnd = viewModel::endWorkout,
                    onAddMarker = viewModel::addMarker,
                    onToggleOnlineSharing = viewModel::toggleOnlineSharing,
                )

                AppDestination.PANEL -> DataPanelScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onAutoCalibrationEnabledChange = viewModel::setCompassAutoCalibrationEnabled,
                    onToggleCalibrationMode = viewModel::setCompassCalibrationMode,
                    onCalibrateSetNorth = viewModel::calibrateCompassSetCurrentAsNorth,
                    onResetCalibration = viewModel::resetCompassCalibration,
                )
                AppDestination.PROFILE -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = uiState,
                    onUpdateProfile = viewModel::updateProfile,
                    onHistoryFieldVisibleChange = viewModel::setHistoryFieldVisible,
                    onMetricVisibleChange = viewModel::setMetricVisible,
                    onShowTrackAreaChange = viewModel::setShowTrackArea,
                    onPanelRefreshIntervalChange = viewModel::setPanelRefreshIntervalSeconds,
                    onCompassAutoCalibrationEnabledChange = viewModel::setCompassAutoCalibrationEnabled,
                    onMoveBottomNavItem = viewModel::moveBottomNavItem,
                    onResetBottomNavOrder = viewModel::resetBottomNavOrder,
                    onMoveMyPageItem = viewModel::moveMyPageItem,
                    onResetMyPageOrder = viewModel::resetMyPageOrder,
                    onMapUseSystemServiceChange = viewModel::setMapUseSystemService,
                    onMapEnabledChange = viewModel::setMapEnabled,
                    onMapApiKeyChange = viewModel::updateMapApiKey,
                    onWeatherEnabledChange = viewModel::setWeatherEnabled,
                    onWeatherApiKeyChange = viewModel::updateWeatherApiKey,
                    onShareEnabledChange = viewModel::setShareEnabled,
                    onShareApiKeyChange = viewModel::updateShareApiKey,
                    onSmtpEnabledChange = viewModel::setSmtpEnabled,
                    onSmtpHostChange = viewModel::updateSmtpHost,
                    onSmtpPortChange = viewModel::updateSmtpPort,
                    onSmtpUsernameChange = viewModel::updateSmtpUsername,
                    onSmtpPasswordChange = viewModel::updateSmtpPassword,
                    onSmtpFromEmailChange = viewModel::updateSmtpFromEmail,
                    onSmtpUseTlsChange = viewModel::setSmtpUseTls,
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
                    historyContent = {
                        HistoryScreen(
                            modifier = Modifier.fillMaxWidth(),
                            uiState = uiState,
                            onOpenDetail = viewModel::openWorkoutDetail,
                            onSetCustomRangeStartDate = viewModel::setCustomRangeStartDate,
                            onSetCustomRangeEndDate = viewModel::setCustomRangeEndDate,
                            onSetHistoryTypeFilter = viewModel::setHistoryTypeFilter,
                            onLoadMoreHistory = viewModel::loadMoreHistory,
                            onCloseWorkoutDetail = viewModel::closeWorkoutDetail,
                            onDetailMapLongPress = viewModel::selectReplayAnchorByMapLongPress,
                            onDetailReplayForward = viewModel::startDetailReplayForward,
                            onDetailReplayReverse = viewModel::startDetailReplayReverse,
                            onDetailReplayTogglePlayPause = viewModel::toggleDetailReplayPlayPause,
                            onDetailReplaySpeedChange = viewModel::setDetailReplaySpeed,
                            onDetailReplaySwitchDirection = viewModel::switchDetailReplayDirection,
                            onDetailReplaySeek = viewModel::seekDetailReplayByProgress,
                            onDetailReplayExit = viewModel::exitDetailReplayPanel,
                            onDetailReplayNoticeConsumed = viewModel::clearDetailReplayNotice,
                        )
                    },
                )

            }
        }
    }

    if (uiState.summaryDialogVisible && uiState.pendingRecord != null) {
        SummaryDialog(
            uiState = uiState,
            onSave = {
                viewModel.saveWorkout()
                currentDestination = AppDestination.PROFILE
            },
            onDiscard = viewModel::discardWorkout,
        )
    }
}

enum class AppDestination(val label: String, val icon: Int) {
    PANEL("数据面板", R.drawable.ic_home),
    HOME("运动", R.drawable.ic_home),
    RECORD("记录", R.drawable.ic_favorite),
    PROFILE("我的", R.drawable.ic_account_box),
}

private fun orderedBottomDestinations(order: List<String>): List<AppDestination> {
    val mapping = mapOf(
        "PANEL" to AppDestination.PANEL,
        "HOME" to AppDestination.HOME,
        "RECORD" to AppDestination.RECORD,
        "PROFILE" to AppDestination.PROFILE,
    )
    val defaults = listOf(AppDestination.PANEL, AppDestination.HOME, AppDestination.RECORD, AppDestination.PROFILE)
    val ordered = order.mapNotNull { mapping[it.uppercase()] }.distinct().toMutableList()
    defaults.forEach { if (!ordered.contains(it)) ordered.add(it) }
    return ordered
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
