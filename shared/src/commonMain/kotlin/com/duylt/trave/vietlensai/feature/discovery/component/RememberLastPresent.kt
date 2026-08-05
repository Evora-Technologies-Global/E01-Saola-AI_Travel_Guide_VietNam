package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * The value, or the last one there was.
 *
 * For content that outlives the state behind it: anything animating out is composed for a few
 * hundred milliseconds after the thing it describes has already been set to null — the note
 * editor the instant Cancel is pressed, the photo album the instant the viewer closes — and
 * without a value to fall back on it spends that time drawing nothing. The visible symptom is
 * a card that blanks on the first frame of its exit and then dissolves an empty box.
 *
 * Both arrangements animate the same two things away, which is why this sits beside the
 * components rather than inside one screen.
 */
@Composable
internal fun <T : Any> rememberLastPresent(value: T?): T? {
    val holder = remember { mutableStateOf(value) }
    // Written but not read on this path, so nothing that has already composed is invalidated
    // by keeping it current; the fallback below is the only reader, and only once the live
    // value is gone.
    if (value != null) {
        holder.value = value
        return value
    }
    return holder.value
}
