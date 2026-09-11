package io.github.aiya000.onlyshare

import android.content.Context
import android.content.Intent

/** Opens the system share sheet for [content]. */
fun Context.share(content: ClipboardContent) {
    val intent = when (content) {
        is ClipboardContent.Text -> content.toSendIntent()
        is ClipboardContent.Streams -> content.toSendIntent()
    }
    startActivity(Intent.createChooser(intent, null))
}

private fun ClipboardContent.Text.toSendIntent(): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

private fun ClipboardContent.Streams.toSendIntent(): Intent {
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }

    // FLAG_GRANT_READ_URI_PERMISSION only looks at the intent's data and clip data, never
    // at EXTRA_STREAM. Without the clip data the app the user picks receives URIs it is
    // not allowed to open.
    return intent.apply {
        type = mimeType
        clipData = uris.toClipData(mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
