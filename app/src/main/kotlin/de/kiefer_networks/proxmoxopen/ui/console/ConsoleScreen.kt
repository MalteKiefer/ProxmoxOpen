package de.kiefer_networks.proxmoxopen.ui.console

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    onBack: () -> Unit,
    viewModel: ConsoleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val webViewRef = remember { arrayOfNulls<WebView>(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { webViewRef[0]?.reload() ?: viewModel.load() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                state.consoleUrl != null -> {
                    ProxmoxWebView(
                        url = state.consoleUrl!!,
                        cookie = state.authCookie,
                        host = state.serverHost ?: "",
                        port = state.serverPort,
                        ref = webViewRef,
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ProxmoxWebView(
    url: String,
    cookie: String?,
    host: String,
    port: Int,
    ref: Array<WebView?>,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // Inject PVEAuthCookie BEFORE loading the page
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val domain = "https://$host:$port"
            cookieManager.setCookie(domain, "PVEAuthCookie=$cookie; path=/; secure")
            cookieManager.flush()

            WebView(context).apply {
                setBackgroundColor(0xFF0B0B0C.toInt())

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                }

                webChromeClient = WebChromeClient()

                webViewClient = object : WebViewClient() {
                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        // Accept self-signed certs (TOFU — user already trusted this server)
                        handler?.proceed()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // Keep all navigation inside the WebView
                        return false
                    }
                }

                ref[0] = this
                loadUrl(url)
            }
        },
    )
}
