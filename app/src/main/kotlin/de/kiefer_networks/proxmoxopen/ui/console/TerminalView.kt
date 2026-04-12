package de.kiefer_networks.proxmoxopen.ui.console

import android.graphics.Typeface
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Minimal terminal view using a native TextView inside a ScrollView.
 * Displays terminal output in monospace and captures keyboard input.
 * For a production app, replace with a proper VT100 emulator (jackpal/androidterm).
 */
@Composable
fun TerminalView(
    output: String,
    onInput: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bgColor = Color(0xFF0B0B0C).toArgb()
    val fgColor = Color(0xFFEFEFF1).toArgb()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            val scrollView = ScrollView(context).apply {
                setBackgroundColor(bgColor)
                isFillViewport = true
            }
            val editText = EditText(context).apply {
                setBackgroundColor(bgColor)
                setTextColor(fgColor)
                typeface = Typeface.MONOSPACE
                textSize = 12f
                isFocusableInTouchMode = true
                isSingleLine = false
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setOnEditorActionListener { _, _, _ -> false }
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_ENTER -> {
                                onInput("\r")
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                addTextChangedListener(object : android.text.TextWatcher {
                    private var lastLength = 0
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { lastLength = s?.length ?: 0 }
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        val current = s?.length ?: 0
                        if (current > lastLength) {
                            val newChars = s?.subSequence(lastLength, current)?.toString() ?: ""
                            if (newChars.isNotEmpty()) onInput(newChars)
                        }
                    }
                })
            }
            scrollView.addView(editText)
            scrollView
        },
        update = { scrollView ->
            val editText = scrollView.getChildAt(0) as? EditText
            editText?.let {
                // Only update if output changed
                val currentOut = it.tag as? String
                if (currentOut != output) {
                    it.tag = output
                    it.setText(output)
                    it.setSelection(output.length)
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        },
    )
}
