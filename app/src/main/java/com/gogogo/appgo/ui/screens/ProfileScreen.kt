package com.gogogo.appgo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Patterns
import com.gogogo.appgo.model.BackupCloudProvider
import com.gogogo.appgo.model.BackupStrategyTemplate
import com.gogogo.appgo.model.HistoryDisplayField
import com.gogogo.appgo.model.RecordingMetric
import com.gogogo.appgo.model.UserProfile
import com.gogogo.appgo.ui.AppUiState
import com.gogogo.appgo.ui.common.formatDateTime

@Composable
fun ProfileScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onUpdateProfile: (UserProfile) -> Unit,
    onHistoryFieldVisibleChange: (HistoryDisplayField, Boolean) -> Unit,
    onMetricVisibleChange: (RecordingMetric, Boolean) -> Unit,
    onShowTrackAreaChange: (Boolean) -> Unit,
    onPanelRefreshIntervalChange: (Int) -> Unit,
    onCompassAutoCalibrationEnabledChange: (Boolean) -> Unit,
    onMoveBottomNavItem: (String, Int) -> Unit,
    onResetBottomNavOrder: () -> Unit,
    onMoveMyPageItem: (String, Int) -> Unit,
    onResetMyPageOrder: () -> Unit,
    onMapUseSystemServiceChange: (Boolean) -> Unit,
    onMapEnabledChange: (Boolean) -> Unit,
    onMapApiKeyChange: (String) -> Unit,
    onWeatherEnabledChange: (Boolean) -> Unit,
    onWeatherApiKeyChange: (String) -> Unit,
    onShareEnabledChange: (Boolean) -> Unit,
    onShareApiKeyChange: (String) -> Unit,
    onSmtpEnabledChange: (Boolean) -> Unit,
    onSmtpHostChange: (String) -> Unit,
    onSmtpPortChange: (Int) -> Unit,
    onSmtpUsernameChange: (String) -> Unit,
    onSmtpPasswordChange: (String) -> Unit,
    onSmtpFromEmailChange: (String) -> Unit,
    onSmtpUseTlsChange: (Boolean) -> Unit,
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
    historyContent: @Composable () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf("device") }
    val tabLabelMap = mapOf(
        "device" to "设备与安全",
        "history" to "历史",
        "settings" to "设置",
    )
    val orderedTabs = uiState.settings.myPageOrder.filter { tabLabelMap.containsKey(it) }
    if (tab !in orderedTabs) {
        tab = orderedTabs.firstOrNull() ?: "device"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orderedTabs) { key ->
                AssistChip(
                    onClick = { tab = key },
                    label = { Text(tabLabelMap[key] ?: key) },
                    border = if (tab == key) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                )
            }
        }

        when (tab) {
            "device" -> DeviceSecuritySection(
                uiState = uiState,
                onUpdateProfile = onUpdateProfile,
            )
            "history" -> historyContent()
            "settings" -> ProfileSettingsScreen(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                onMetricVisibleChange = onMetricVisibleChange,
                onHistoryFieldVisibleChange = onHistoryFieldVisibleChange,
                onShowTrackAreaChange = onShowTrackAreaChange,
                onPanelRefreshIntervalChange = onPanelRefreshIntervalChange,
                onCompassAutoCalibrationEnabledChange = onCompassAutoCalibrationEnabledChange,
                onMoveBottomNavItem = onMoveBottomNavItem,
                onResetBottomNavOrder = onResetBottomNavOrder,
                onMoveMyPageItem = onMoveMyPageItem,
                onResetMyPageOrder = onResetMyPageOrder,
                onMapUseSystemServiceChange = onMapUseSystemServiceChange,
                onMapEnabledChange = onMapEnabledChange,
                onMapApiKeyChange = onMapApiKeyChange,
                onWeatherEnabledChange = onWeatherEnabledChange,
                onWeatherApiKeyChange = onWeatherApiKeyChange,
                onShareEnabledChange = onShareEnabledChange,
                onShareApiKeyChange = onShareApiKeyChange,
                onSmtpEnabledChange = onSmtpEnabledChange,
                onSmtpHostChange = onSmtpHostChange,
                onSmtpPortChange = onSmtpPortChange,
                onSmtpUsernameChange = onSmtpUsernameChange,
                onSmtpPasswordChange = onSmtpPasswordChange,
                onSmtpFromEmailChange = onSmtpFromEmailChange,
                onSmtpUseTlsChange = onSmtpUseTlsChange,
                onLocalBackupEnabledChange = onLocalBackupEnabledChange,
                onCloudProviderChange = onCloudProviderChange,
                onCloudBackupEnabledChange = onCloudBackupEnabledChange,
                onCloudApiKeyChange = onCloudApiKeyChange,
                onCloudSecretChange = onCloudSecretChange,
                onCloudBucketOrPathChange = onCloudBucketOrPathChange,
                onCloudEndpointChange = onCloudEndpointChange,
                onTemplateChange = onTemplateChange,
                onBackupAutoEnabledChange = onBackupAutoEnabledChange,
                onBackupIntervalHoursChange = onBackupIntervalHoursChange,
                onBackupRetainDaysChange = onBackupRetainDaysChange,
                onBackupWifiOnlyChange = onBackupWifiOnlyChange,
                onBackupChargingOnlyChange = onBackupChargingOnlyChange,
                onRunManualBackup = onRunManualBackup,
                onRefreshBackupFiles = onRefreshBackupFiles,
                onSelectBackupFile = onSelectBackupFile,
                onBackupFileContentChange = onBackupFileContentChange,
                onSaveBackupFileContent = onSaveBackupFileContent,
            )
        }
    }
}

@Composable
private fun DeviceSecuritySection(
    uiState: AppUiState,
    onUpdateProfile: (UserProfile) -> Unit,
) {
    var newEmail by remember(uiState.profile) { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val recipients = remember(uiState.profile) { mutableStateListOf<String>().apply { addAll(uiState.profile.shareRecipientEmails) } }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("共享与安全", fontWeight = FontWeight.SemiBold)
            Text("共享联系人邮箱", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it.trim() },
                    label = { Text("新增邮箱") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val email = newEmail.trim()
                        inputError = null
                        when {
                            email.isBlank() -> inputError = "请输入邮箱地址"
                            !isValidEmail(email) -> inputError = "邮箱格式不正确"
                            recipients.contains(email) -> inputError = "该邮箱已存在"
                            else -> {
                            recipients.add(email)
                            newEmail = ""
                            }
                        }
                    },
                ) { Text("添加") }
            }
            inputError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (recipients.isEmpty()) {
                Text("当前未配置共享联系人邮箱。", style = MaterialTheme.typography.bodySmall)
            } else {
                recipients.toList().forEachIndexed { index, email ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { value ->
                                recipients[index] = value.trim()
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("联系人${index + 1}") },
                        )
                        OutlinedButton(onClick = { recipients.removeAt(index) }) { Text("删除") }
                    }
                }
            }
            if (uiState.settings.serviceIntegration.shareActive) {
                Text("实时位置共享: 已接入共享服务")
            }
            if (uiState.settings.serviceIntegration.weatherActive) {
                Text("天气安全提醒: 已接入天气服务")
            }
            Text("支持多联系人共享，支持增删改查并保存。")
            Button(
                onClick = {
                    val normalized = recipients.map { it.trim() }.filter { isValidEmail(it) }.distinct()
                    onUpdateProfile(
                        uiState.profile.copy(
                            shareRecipientEmails = normalized,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存共享联系人")
            }
        }
    }
}

private fun isValidEmail(email: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(email).matches()

@Composable
private fun ProfileSettingsScreen(
    modifier: Modifier,
    uiState: AppUiState,
    onMetricVisibleChange: (RecordingMetric, Boolean) -> Unit,
    onHistoryFieldVisibleChange: (HistoryDisplayField, Boolean) -> Unit,
    onShowTrackAreaChange: (Boolean) -> Unit,
    onPanelRefreshIntervalChange: (Int) -> Unit,
    onCompassAutoCalibrationEnabledChange: (Boolean) -> Unit,
    onMoveBottomNavItem: (String, Int) -> Unit,
    onResetBottomNavOrder: () -> Unit,
    onMoveMyPageItem: (String, Int) -> Unit,
    onResetMyPageOrder: () -> Unit,
    onMapUseSystemServiceChange: (Boolean) -> Unit,
    onMapEnabledChange: (Boolean) -> Unit,
    onMapApiKeyChange: (String) -> Unit,
    onWeatherEnabledChange: (Boolean) -> Unit,
    onWeatherApiKeyChange: (String) -> Unit,
    onShareEnabledChange: (Boolean) -> Unit,
    onShareApiKeyChange: (String) -> Unit,
    onSmtpEnabledChange: (Boolean) -> Unit,
    onSmtpHostChange: (String) -> Unit,
    onSmtpPortChange: (Int) -> Unit,
    onSmtpUsernameChange: (String) -> Unit,
    onSmtpPasswordChange: (String) -> Unit,
    onSmtpFromEmailChange: (String) -> Unit,
    onSmtpUseTlsChange: (Boolean) -> Unit,
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
    var panelRefreshInput by remember(uiState.settings.panelRefreshIntervalSeconds) {
        mutableStateOf(uiState.settings.panelRefreshIntervalSeconds.toString())
    }
    var intervalHoursInput by remember(backup.strategy.intervalHours) { mutableStateOf(backup.strategy.intervalHours.toString()) }
    var retainDaysInput by remember(backup.strategy.retainDays) { mutableStateOf(backup.strategy.retainDays.toString()) }
    var smtpPortInput by remember(services.smtpPort) { mutableStateOf(services.smtpPort.toString()) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        ExpandableSectionCard(title = "运动与记录", defaultExpanded = false) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("运动页面与记录页", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("记录页显示轨迹区域")
                    Switch(
                        checked = uiState.settings.showTrackArea,
                        onCheckedChange = onShowTrackAreaChange,
                    )
                }
                OutlinedTextField(
                    value = panelRefreshInput,
                    onValueChange = {
                        panelRefreshInput = it
                        it.toIntOrNull()?.let(onPanelRefreshIntervalChange)
                    },
                    label = { Text("数据面板刷新频率(秒)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("指南针自动校准")
                    Switch(
                        checked = uiState.settings.compassAutoCalibrationEnabled,
                        onCheckedChange = onCompassAutoCalibrationEnabledChange,
                    )
                }
                Text("底部导航顺序（4页）", style = MaterialTheme.typography.bodySmall)
                uiState.settings.bottomNavOrder.forEach { key ->
                    val label = when (key) {
                        "PANEL" -> "数据面板"
                        "HOME" -> "运动"
                        "RECORD" -> "记录"
                        "PROFILE" -> "我的"
                        else -> key
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onMoveBottomNavItem(key, -1) }) { Text("上移") }
                            OutlinedButton(onClick = { onMoveBottomNavItem(key, 1) }) { Text("下移") }
                        }
                    }
                }
                OutlinedButton(onClick = onResetBottomNavOrder, modifier = Modifier.fillMaxWidth()) {
                    Text("重置底部导航顺序")
                }
                HorizontalDivider()
                Text("我的页面模块顺序", style = MaterialTheme.typography.bodySmall)
                val myLabels = mapOf(
                    "device" to "设备与安全",
                    "history" to "历史",
                    "settings" to "设置",
                )
                uiState.settings.myPageOrder.forEach { key ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(myLabels[key] ?: key)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onMoveMyPageItem(key, -1) }) { Text("上移") }
                            OutlinedButton(onClick = { onMoveMyPageItem(key, 1) }) { Text("下移") }
                        }
                    }
                }
                OutlinedButton(onClick = onResetMyPageOrder, modifier = Modifier.fillMaxWidth()) {
                    Text("重置我的页面顺序")
                }
            }
        }

        ExpandableSectionCard(title = "记录展示", defaultExpanded = false) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("记录页展示项", fontWeight = FontWeight.SemiBold)
                Text("勾选后才会在记录页展示。至少保留一个字段。", style = MaterialTheme.typography.bodySmall)
                RecordingMetric.entries.filter { it != RecordingMetric.HEART_RATE }.chunked(2).forEach { row ->
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

        ExpandableSectionCard(title = "历史展示", defaultExpanded = false) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("历史记录展示项", fontWeight = FontWeight.SemiBold)
                Text("用于“我的-历史”记录卡片字段展示，至少保留一个字段。", style = MaterialTheme.typography.bodySmall)
                HistoryDisplayField.entries.filter { it != HistoryDisplayField.AVG_HEART_RATE }.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { field ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = uiState.settings.visibleHistoryFields.contains(field),
                                    onCheckedChange = { checked -> onHistoryFieldVisibleChange(field, checked) },
                                )
                                Text(field.label)
                            }
                        }
                    }
                }
            }
        }

        ExpandableSectionCard(title = "服务接入", defaultExpanded = false) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ServiceIntegrationCard(
                    title = "地图服务",
                    enabled = services.mapEnabled,
                    apiKey = services.mapApiKey,
                    isActive = services.mapActive && !services.mapUseSystemService,
                    hint = "用于地图渲染/离线地图服务",
                    onEnabledChange = onMapEnabledChange,
                    onApiKeyChange = onMapApiKeyChange,
                    extraTop = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("优先使用手机地图服务")
                            Switch(
                                checked = services.mapUseSystemService,
                                onCheckedChange = onMapUseSystemServiceChange,
                            )
                        }
                    },
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
            }
        }

        ExpandableSectionCard(title = "SMTP 邮件共享", defaultExpanded = false) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("在线轨迹共享邮件通道", fontWeight = FontWeight.SemiBold)
                Text("该部分由用户自行配置，配置完成后可通过邮件发送在线轨迹共享。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = services.smtpHost,
                    onValueChange = onSmtpHostChange,
                    label = { Text("SMTP Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = smtpPortInput,
                    onValueChange = {
                        smtpPortInput = it
                        it.toIntOrNull()?.let(onSmtpPortChange)
                    },
                    label = { Text("SMTP Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = services.smtpUsername,
                    onValueChange = onSmtpUsernameChange,
                    label = { Text("SMTP Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = services.smtpPassword,
                    onValueChange = onSmtpPasswordChange,
                    label = { Text("SMTP Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = services.smtpFromEmail,
                    onValueChange = onSmtpFromEmailChange,
                    label = { Text("发件人邮箱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("启用 STARTTLS")
                    Switch(checked = services.smtpUseTls, onCheckedChange = onSmtpUseTlsChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("启用 SMTP 邮件共享")
                    Switch(
                        checked = services.smtpEnabled && services.smtpConfigured,
                        onCheckedChange = onSmtpEnabledChange,
                        enabled = services.smtpConfigured,
                    )
                }
                if (!services.smtpConfigured) {
                    Text("SMTP 未配置完成，无法启用邮件共享。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("SMTP 配置完整，可用于在线轨迹共享邮件发送。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        ExpandableSectionCard(title = "备份设置", defaultExpanded = false) {
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
                    Text("最近备份: ${formatDateTime(ts)}", style = MaterialTheme.typography.bodySmall)
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

        ExpandableSectionCard(title = "赞助支持", defaultExpanded = false) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("本应用承诺：无广告、无订阅、无会员分级。", fontWeight = FontWeight.SemiBold)
                Text("如果你认可这个项目，可以选择自愿赞助支持开发与维护。", style = MaterialTheme.typography.bodySmall)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("赞助方式", fontWeight = FontWeight.SemiBold)
                        Text("支付宝：appgo_support@outdoor.dev")
                        Text("微信赞助：AppGo-Outdoor-Support")
                        Text("邮箱联系：support@appgo.dev")
                    }
                }
                Text("赞助不会解锁任何额外功能，只用于项目持续维护。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(defaultExpanded) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "收起" else "展开")
                }
            }
            if (expanded) content()
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
    extraTop: (@Composable () -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            extraTop?.invoke()
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
