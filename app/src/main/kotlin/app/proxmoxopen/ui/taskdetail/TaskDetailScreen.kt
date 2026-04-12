package app.proxmoxopen.ui.taskdetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.BadgeTone
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.core.ui.component.StatusBadge
import app.proxmoxopen.domain.model.TaskState
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
                title = {
                    Column {
                        Text(stringResource(R.string.task_detail_title), style = MaterialTheme.typography.titleMedium)
                        state.task?.let { Text(it.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Outlined.Refresh, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null && state.task == null -> ErrorState(state.error?.message ?: "", stringResource(R.string.retry), viewModel::refresh, Modifier.padding(padding))
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Task info card
                state.task?.let { task ->
                    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    val tone = when (task.state) { TaskState.RUNNING -> BadgeTone.Running; TaskState.OK -> BadgeTone.Running; TaskState.FAILED -> BadgeTone.Error; else -> BadgeTone.Neutral }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(task.type, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                StatusBadge(task.state.name, tone)
                            }
                            Row(Modifier.fillMaxWidth()) { Text("User", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(task.user, style = MaterialTheme.typography.bodySmall) }
                            Row(Modifier.fillMaxWidth()) { Text("Started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(df.format(Date(task.startTime * 1000)), style = MaterialTheme.typography.bodySmall) }
                            task.endTime?.let { Row(Modifier.fillMaxWidth()) { Text("Ended", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(df.format(Date(it * 1000)), style = MaterialTheme.typography.bodySmall) } }
                            task.exitStatus?.let { Row(Modifier.fillMaxWidth()) { Text("Exit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(it, style = MaterialTheme.typography.bodySmall, color = if (task.state == TaskState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) } }
                        }
                    }
                }

                // Log output
                if (state.logLines.isNotEmpty()) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    ) {
                        Column(
                            Modifier.padding(12.dp).horizontalScroll(rememberScrollState()),
                        ) {
                            state.logLines.forEach { line ->
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                    ),
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
