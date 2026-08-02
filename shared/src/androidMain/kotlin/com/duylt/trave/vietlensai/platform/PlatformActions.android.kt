package com.duylt.trave.vietlensai.platform

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import com.duylt.trave.vietlensai.core.util.log
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun rememberPhotoPicker(
    maxItems: Int,
    onPicked: (paths: List<String>) -> Unit,
): () -> Unit {
    val captureStore = koinInject<CaptureStore>()
    val scope = rememberCoroutineScope()
    val slots = maxItems.coerceAtLeast(1)

    // Read through a snapshot state so the launchers, which are registered once, always
    // call the callback this composition passed rather than the one it was born with.
    val currentOnPicked by rememberUpdatedState(onPicked)

    val importAll: (List<Uri>) -> Unit = { uris ->
        if (uris.isNotEmpty()) {
            // Copied into app storage before the paths leave here: the picker's grant does
            // not survive the process, and the journal would be left holding unreadable
            // photos. Sequential rather than concurrent — the selection order is the order
            // the traveller expects to see them in, and six copies is not worth a race.
            scope.launch {
                val imported = uris.mapNotNull { uri ->
                    when (val result = captureStore.importFromPicker(uri.toString())) {
                        is AppResult.Success -> result.data
                        is AppResult.Failure -> {
                            log.w { "Could not import a picked photo: ${result.error}" }
                            null
                        }
                    }
                }
                if (imported.isNotEmpty()) currentOnPicked(imported)
            }
        }
    }

    // `PickMultipleVisualMedia` rejects a limit of one, so the last free slot has to go
    // through the single-item contract. Both are registered up front because registration
    // happens at composition, long before the traveller taps anything.
    val singleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> importAll(listOfNotNull(uri)) }

    val multipleContract = remember(slots) {
        ActivityResultContracts.PickMultipleVisualMedia(slots.coerceAtLeast(2))
    }
    val multipleLauncher = rememberLauncherForActivityResult(multipleContract) { uris: List<Uri> ->
        importAll(uris)
    }

    return remember(slots, singleLauncher, multipleLauncher) {
        {
            val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            if (slots == 1) singleLauncher.launch(request) else multipleLauncher.launch(request)
        }
    }
}

@Composable
actual fun rememberTextSharer(): (title: String, body: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { title, body ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(intent, title))
        }
    }
}

@Composable
actual fun rememberUrlOpener(): (url: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { log.w(it) { "Nothing on this device opens $url" } }
        }
    }
}

@Composable
actual fun rememberTextCopier(): (text: String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text ->
            scope.launch {
                clipboard.setClipEntry(
                    ClipEntry(ClipData.newPlainText(CLIP_LABEL, text)),
                )
            }
        }
    }
}

/** Shown by the system's paste preview on Android 13+, so it names what was copied. */
private const val CLIP_LABEL = "translation"
