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
                        csrfToken = state.csrfToken ?: "",
                        username = state.username ?: "root@pam",
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
    csrfToken: String,
    username: String,
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
            // Set cookie for both https and http, with and without port
            val cookieValue = "PVEAuthCookie=$cookie; path=/"
            cookieManager.setCookie("https://$host", cookieValue)
            cookieManager.setCookie("https://$host:$port", cookieValue)
            cookieManager.setCookie("http://$host", cookieValue)
            cookieManager.setCookie("http://$host:$port", cookieValue)
            cookieManager.setCookie(host, cookieValue)
            cookieManager.flush()

            WebView(context).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
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
                        handler?.proceed()
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject auth into Proxmox's ExtJS framework via localStorage + JS
                        view?.evaluateJavascript("""
                            (function() {
                                try {
                                    // Set auth in Proxmox's expected format
                                    var authCookie = '$cookie';
                                    if (window.Proxmox) {
                                        window.Proxmox.UserName = 'root@pam';
                                        window.Proxmox.CSRFPreventionToken = '';
                                    }
                                    // Set cookie via JS as fallback
                                    document.cookie = 'PVEAuthCookie=' + authCookie + '; path=/';

                                    // Try to auto-login if login form is visible
                                    if (document.querySelector('.x-window')) {
                                        // Login dialog detected — inject credentials
                                        var loginBtn = document.querySelector('button');
                                        if (loginBtn) {
                                            // Force reload with cookie set
                                            location.reload();
                                        }
                                    }
                                } catch(e) { console.log('Auth inject error: ' + e); }
                            })();
                        """.trimIndent(), null)
                    }
                }

                ref[0] = this
                // Load the origin first to set localStorage, then redirect to console
                loadUrl("https://$host:$port/")
                postDelayed({
                    evaluateJavascript("""
                        (function() {
                            var loginData = {
                                username: '$username',
                                CSRFPreventionToken: '$csrfToken',
                                cap: {"vms":1,"dc":1,"access":1,"nodes":1,"storage":1,"sdn":1}
                            };
                            window.localStorage.setItem('LoginData', JSON.stringify(loginData));
                            document.cookie = 'PVEAuthCookie=$cookie; path=/';
                            window.location.href = '$url';
                        })();
                    """.trimIndent(), null)
                }, 1500)
            }
        },
    )
}
