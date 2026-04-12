package app.proxmoxopen.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

@Composable
fun OverflowMenu(items: List<MenuItem>) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        item.label,
                        color = item.tint ?: MaterialTheme.colorScheme.onSurface,
                    )
                },
                leadingIcon = item.icon?.let { icon ->
                    { Icon(icon, contentDescription = null, tint = item.tint ?: MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                onClick = { expanded = false; item.onClick() },
            )
        }
    }
}
