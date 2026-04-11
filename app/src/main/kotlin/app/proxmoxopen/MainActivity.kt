package app.proxmoxopen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.proxmoxopen.core.ui.theme.ProxMoxOpenTheme
import app.proxmoxopen.ui.nav.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProxMoxOpenTheme {
                NavGraph()
            }
        }
    }
}
