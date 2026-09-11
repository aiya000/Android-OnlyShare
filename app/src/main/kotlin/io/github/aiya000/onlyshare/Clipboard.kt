package io.github.aiya000.onlyshare

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri

/**
 * What the clipboard holds, in the shape a share intent needs.
 *
 * The clipboard is not limited to text: copying a picture, a PDF or a recording puts a
 * `content://` URI on it instead, and those are shared as a stream rather than as text.
 */
sealed interface ClipboardContent {

    /** Plain text, including the text coerced out of richer items such as HTML. */
    data class Text(val text: String) : ClipboardContent

    /**
     * One or more `content://` URIs.
     *
     * @param mimeType the type that covers every URI in [uris]
     */
    data class Streams(val uris: List<Uri>, val mimeType: String) : ClipboardContent
}

/**
 * Reads the current clipboard, or returns null when it is empty or holds nothing that
 * can be shared.
 *
 * Since Android 10 (API 29) this only works while the calling app holds the window focus.
 */
fun Context.readClipboardContent(): ClipboardContent? {
    val manager = getSystemService(ClipboardManager::class.java) ?: return null
    if (!manager.hasPrimaryClip()) {
        return null
    }

    val clip = manager.primaryClip ?: return null
    val items = (0 until clip.itemCount).map(clip::getItemAt)

    // A clip that carries content URIs is shared as a stream even when it also carries
    // text: there the text is only a label for the content, not the content itself.
    // Other schemes (http, file) are left to the text branch -- a http URI is a link,
    // and a file URI cannot legally leave the app on API 24 and above.
    val uris = items.mapNotNull { it.uri }.filter { it.scheme == ContentResolver.SCHEME_CONTENT }
    if (uris.isNotEmpty()) {
        return ClipboardContent.Streams(uris, mimeTypeOf(uris, clip.description))
    }

    val text = items
        .mapNotNull { it.coerceToText(this)?.toString() }
        .filter { it.isNotEmpty() }
        .joinToString(separator = "\n")

    return if (text.isEmpty()) null else ClipboardContent.Text(text)
}

/**
 * The narrowest MIME type that still covers every URI of the clip: `image/png` for a
 * single screenshot, and the wildcard `image` type for a PNG next to a JPEG.
 *
 * (A KDoc cannot spell a wildcard MIME type out, because Kotlin block comments nest and
 * the slash-star of it would open one.)
 */
private fun Context.mimeTypeOf(uris: List<Uri>, description: ClipDescription?): String =
    uris
        .map { contentResolver.getType(it) ?: description?.firstNonTextMimeType() ?: ANY_TYPE }
        .reduce(::mergeMimeTypes)

private fun mergeMimeTypes(left: String, right: String): String = when {
    left == right -> left
    left.substringBefore('/') == right.substringBefore('/') -> "${left.substringBefore('/')}/*"
    else -> ANY_TYPE
}

/**
 * The declared type of the clip, ignoring the text types a clip tends to declare next to
 * its real content so that it can also be pasted into a text field.
 */
private fun ClipDescription.firstNonTextMimeType(): String? =
    (0 until mimeTypeCount)
        .map(::getMimeType)
        .firstOrNull { !it.startsWith("text/") }

private const val ANY_TYPE = "*/*"

/** The clip data to hang on a share intent, so that the URI grant travels with it. */
fun List<Uri>.toClipData(mimeType: String): ClipData {
    val clip = ClipData(null as CharSequence?, arrayOf(mimeType), ClipData.Item(first()))
    drop(1).forEach { clip.addItem(ClipData.Item(it)) }
    return clip
}
