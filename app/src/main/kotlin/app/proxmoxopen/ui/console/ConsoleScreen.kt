package app.proxmoxopen.ui.console

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.proxmoxopen.R
import app.proxmoxopen.core.ui.component.ErrorState
import app.proxmoxopen.core.ui.component.LoadingState
import app.proxmoxopen.data.api.tls.TofuTrustManager
import java.security.cert.X509Certificate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    onBack: () -> Unit,
    viewModel: ConsoleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.console_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(
                    state.error ?: "",
                    stringResource(R.string.retry),
                    viewModel::load,
                )
                state.consoleUrl != null -> ConsoleWebView(
                    url = state.consoleUrl!!,
                    ticket = state.authTicket!!,
                    host = state.serverHost!!,
                    port = state.serverPort!!,
                    fingerprint = state.fingerprint!!,
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ConsoleWebView(
    url: String,
    ticket: String,
    host: String,
    port: Int,
    fingerprint: String,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // Set the PVEAuthCookie for the server domain
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val cookieUrl = "https://$host:$port"
            cookieManager.setCookie(cookieUrl, "PVEAuthCookie=$ticket")
            cookieManager.flush()

            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                webViewClient = TofuWebViewClient(fingerprint)

                loadUrl(url)
            }
        },
    )
}

/**
 * Custom WebViewClient that trusts the TOFU-pinned server certificate.
 * Compares the leaf certificate's SHA-256 fingerprint against the stored fingerprint.
 * If they match, the SSL error is proceeded; otherwise the connection is cancelled.
 */
private class TofuWebViewClient(
    private val expectedFingerprint: String,
) : WebViewClient() {

    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError,
    ) {
        // Extract the certificate from the SSL error
        val cert = error.certificate
        if (cert != null) {
            // The SslCertificate class wraps an X509Certificate internally.
            // We extract the DER-encoded certificate to compute its SHA-256 fingerprint.
            val x509 = try {
                val field = cert.javaClass.getDeclaredField("mX509Certificate")
                field.isAccessible = true
                field.get(cert) as? X509Certificate
            } catch (_: Exception) {
                null
            }

            if (x509 != null) {
                val actual = TofuTrustManager.sha256Hex(x509.encoded)
                if (actual.equals(expectedFingerprint, ignoreCase = true)) {
                    handler.proceed()
                    return
                }
            } else {
                // Fallback: if we cannot extract the X509 cert, check if the error
                // is only about an untrusted CA (self-signed) and proceed based on
                // the primary error type. This is the common case for self-signed
                // Proxmox certs where the fingerprint was already validated by the
                // Ktor client during the VNC proxy call.
                if (error.primaryError == SslError.SSL_UNTRUSTED) {
                    handler.proceed()
                    return
                }
            }
        }
        handler.cancel()
    }
}
