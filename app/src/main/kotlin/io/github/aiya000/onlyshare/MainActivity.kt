package io.github.aiya000.onlyshare

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * Hands whatever the clipboard holds straight to the system share sheet.
 *
 * The activity itself has no UI. Its window is transparent and empty, and it only exists
 * because the clipboard can only be read by the app that holds the window focus, which is
 * also why the clipboard is read in [onWindowFocusChanged] rather than in [onCreate].
 */
class MainActivity : ComponentActivity() {

    /** The clipboard is shared once per launch, not again every time the focus comes back. */
    private var shared = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        shared = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || shared) {
            return
        }
        shared = true

        shareClipboard()
        finish()
    }

    private fun shareClipboard() {
        val content = readClipboardContent()
        if (content == null) {
            toast(R.string.clipboard_empty)
            return
        }

        try {
            share(content)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app_to_share)
        }
    }

    private fun toast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
