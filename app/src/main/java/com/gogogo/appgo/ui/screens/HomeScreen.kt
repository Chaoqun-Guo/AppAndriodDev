package com.gogogo.appgo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gogogo.appgo.model.ExerciseType
import com.gogogo.appgo.model.RecordingStatus
import com.gogogo.appgo.ui.AppUiState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: AppUiState,
    onSelectType: (ExerciseType) -> Unit,
    onStartWithPathRecordingChange: (Boolean) -> Unit,
    onStart: (Boolean) -> Unit,
) {
    val workoutActive = uiState.recordingStatus == RecordingStatus.RECORDING ||
        uiState.recordingStatus == RecordingStatus.PAUSED ||
        uiState.recordingStatus == RecordingStatus.AUTO_PAUSED
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
        Text(text = "运动", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = "今天也适合出发，先确认状态再开始。", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD6E8F0))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("当前定位", color = Color(0xFFD6E8F0), style = MaterialTheme.typography.bodySmall)
                    Text(uiState.currentAddressText.ifBlank { "定位中..." }, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("天气", color = Color(0xFFD6E8F0), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${uiState.weather.temperatureC?.let { "%.1f".format(it) } ?: "--"}°C / 风 ${uiState.weather.windSpeedMs?.let { "%.1f".format(it) } ?: "--"}m/s",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (workoutActive) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("运动中", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("当前运动进行中，本页面操作已锁定。请前往「记录」页进行继续/暂停/结束。")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("开始新活动", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExerciseType.entries) { type ->
                        AssistChip(
                            onClick = { if (!workoutActive) onSelectType(type) },
                            label = { Text(type.label) },
                            border = if (uiState.selectedType == type) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        )
                    }
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    onClick = { onStart(uiState.startWithPathRecordingOnWorkout) },
                    enabled = !workoutActive,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(if (workoutActive) "运动中" else "开始 ${uiState.selectedType.label}", fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("同步路径记录")
                    Switch(
                        checked = uiState.startWithPathRecordingOnWorkout,
                        onCheckedChange = onStartWithPathRecordingChange,
                        enabled = !workoutActive,
                    )
                }
                Text(
                    "开启后可在历史详情中做正反向回溯。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
