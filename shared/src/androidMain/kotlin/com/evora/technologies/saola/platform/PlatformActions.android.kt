package com.evora.technologies.saola.platform

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
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File

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
actual fun rememberMailSharer(): (
    recipient: String,
    subject: String,
    body: String,
    attachmentPath: String?,
) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { recipient, subject, body, attachmentPath ->
            // A grant, not a copy: `FileProvider` hands the chosen app a read permission on
            // this one URI for the life of the activity it starts. Attaching the file any
            // other way means either a world-readable copy in the cache or a `file://` URI,
            // and the second has thrown `FileUriExposedException` since Android 7.
            val attachment = attachmentPath?.let { path ->
                runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
                }.onFailure {
                    // Not fatal, and deliberately not surfaced: the report is already saved,
                    // and a complaint that arrives without its photograph is still a
                    // complaint. Failing the whole share over the attachment would lose both.
                    log.w(it) { "Could not attach $path to the report" }
                }.getOrNull()
            }

            // `ShareCompat.IntentBuilder` rather than a hand-built `ACTION_SEND` inside
            // `Intent.createChooser`, and the difference is not stylistic — the hand-built
            // version was written first and is broken in a way no build catches.
            // `createChooser` wraps the send intent, so `FLAG_GRANT_READ_URI_PERMISSION` and
            // `EXTRA_STREAM` end up on the *inner* one while the resolver process reads the
            // *outer* one. The system chooser then cannot open the URI it is being asked to
            // preview, and says so only in logcat:
            //
            //   Permission Denial: opening provider androidx.core.content.FileProvider …
            //   No content provider found for permission check: content://….fileprovider/…
            //
            // `createChooserIntent()` runs `migrateExtraStreamToClipData` over the chooser
            // itself, which is what puts the URI in its `ClipData` and the grant flag beside
            // it. Reproduced on a Pixel 7 Pro API 37 on 05.08.2026: the sheet opened with the
            // text intact and an empty grey square where the photograph should have been.
            val chooser = ShareCompat.IntentBuilder(context)
                // `image/jpeg` narrows the chooser to apps that can carry the photograph;
                // with nothing to carry, `text/plain` keeps every mail and message app in it.
                .setType(if (attachment != null) "image/jpeg" else "text/plain")
                // Honoured by every mail app and ignored by the rest, which is the whole of
                // what "addressed where the platform allows" means here.
                .setEmailTo(arrayOf(recipient))
                .setSubject(subject)
                .setText(body)
                .apply { attachment?.let(::setStream) }
                .setChooserTitle(subject)
                .createChooserIntent()

            runCatching { context.startActivity(chooser) }
                .onFailure { log.w(it) { "Nothing on this device can send the report" } }
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
