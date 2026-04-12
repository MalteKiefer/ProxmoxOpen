package de.kiefer_networks.proxmoxopen.ui.console

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    onBack: () -> Unit,
    viewModel: ConsoleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val termViewRef = remember { arrayOfNulls<TerminalView>(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title, style = MaterialTheme.typography.titleMedium)
                        if (state.isConnected) Text("Connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    // Keyboard toggle button
                    if (state.isConnected) {
                        IconButton(onClick = {
                            termViewRef[0]?.let { view ->
                                view.requestFocus()
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
                            }
                        }) { Icon(Icons.Outlined.Keyboard, contentDescription = "Keyboard") }
                    }
                    if (!state.isConnected && !state.isConnecting) {
                        IconButton(onClick = viewModel::connect) { Icon(Icons.Outlined.Refresh, contentDescription = null) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isConnecting) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
            when {
                state.isConnecting && !state.sessionReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Connecting…", Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.error != null && !state.isConnected -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                state.sessionReady && viewModel.terminalSession != null -> {
                    TermuxView(viewModel.terminalSession!!, viewModel, termViewRef)
                }
            }
        }
    }
}

@Composable
private fun TermuxView(session: TerminalSession, viewModel: ConsoleViewModel, ref: Array<TerminalView?>) {
    val bgColor = MaterialTheme.colorScheme.background.toArgb()
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            TerminalView(ctx, null).apply {
                setBackgroundColor(bgColor)
                setTextSize(18) // Bigger default font
                attachSession(session)
                setTerminalViewClient(object : TerminalViewClient {
                    override fun onScale(scale: Float): Float = 18f
                    override fun onSingleTapUp(e: MotionEvent?) {
                        // Show keyboard on tap
                        requestFocus()
                        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(this@apply, InputMethodManager.SHOW_IMPLICIT)
                    }
                    override fun shouldBackButtonBeMappedToEscape() = false
                    override fun shouldEnforceCharBasedInput() = true
                    override fun shouldUseCtrlSpaceWorkaround() = false
                    override fun isTerminalViewSelected() = true
                    override fun copyModeChanged(copyMode: Boolean) {}
                    override fun onKeyDown(keyCode: Int, e: KeyEvent?, s: TerminalSession?): Boolean = false
                    override fun onKeyUp(keyCode: Int, e: KeyEvent?) = false
                    override fun onLongPress(event: MotionEvent?) = false
                    override fun readControlKey() = false
                    override fun readAltKey() = false
                    override fun readShiftKey() = false
                    override fun readFnKey() = false
                    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, s: TerminalSession?): Boolean {
                        val ch = if (ctrlDown && codePoint in 'a'.code..'z'.code) {
                            (codePoint - 'a'.code + 1).toChar().toString()
                        } else {
                            String(Character.toChars(codePoint))
                        }
                        viewModel.sendInput(ch)
                        return true
                    }
                    override fun onEmulatorSet() {
                        session.emulator?.let { viewModel.updateSize(it.mRows, it.mColumns) }
                    }
                    override fun logError(tag: String?, message: String?) {}
                    override fun logWarn(tag: String?, message: String?) {}
                    override fun logInfo(tag: String?, message: String?) {}
                    override fun logDebug(tag: String?, message: String?) {}
                    override fun logVerbose(tag: String?, message: String?) {}
                    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
                    override fun logStackTrace(tag: String?, e: Exception?) {}
                })
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
                // Auto-show keyboard
                post {
                    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
                ref[0] = this
            }
        },
        update = { view -> view.onScreenUpdated() },
    )
}
