package de.kiefer_networks.proxmoxopen.ui.tasklog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kiefer_networks.proxmoxopen.R
import de.kiefer_networks.proxmoxopen.core.ui.component.BadgeTone
import de.kiefer_networks.proxmoxopen.core.ui.component.ErrorState
import de.kiefer_networks.proxmoxopen.core.ui.component.LoadingState
import de.kiefer_networks.proxmoxopen.core.ui.component.StatusBadge
import de.kiefer_networks.proxmoxopen.domain.model.TaskState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_detail_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            when {
                state.isLoading -> LoadingState()
                state.error != null && state.task == null ->
                    ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::refresh)
                else -> {
                    val task = state.task
                    val logLines = state.logLines
                    val listState = rememberLazyListState()

                    // Auto-scroll to bottom when log lines change
                    LaunchedEffect(logLines.size) {
                        if (logLines.isNotEmpty()) {
                            listState.animateScrollToItem(logLines.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Task info header
                        if (task != null) {
                            item(key = "header") {
                                TaskInfoHeader(task)
                            }
                        }

                        // Log output
                        item(key = "log") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    if (logLines.isEmpty()) {
                                        Text(
                                            "No log output.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    } else {
                                        logLines.forEach { line ->
                                            Text(
                                                text = line.text,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskInfoHeader(task: de.kiefer_networks.proxmoxopen.domain.model.ProxmoxTask) {
    val df = java.text.DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    val tone = when (task.state) {
        TaskState.RUNNING -> BadgeTone.Running
        TaskState.OK -> BadgeTone.Running
        TaskState.FAILED -> BadgeTone.Error
        TaskState.UNKNOWN -> BadgeTone.Neutral
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.type,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(task.state.name, tone)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("User", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(task.user, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(df.format(Date(task.startTime * 1000)), style = MaterialTheme.typography.bodySmall)
            }
            task.exitStatus?.let { exit ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "Exit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        exit,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.state == TaskState.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
