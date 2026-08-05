package com.evora.technologies.saola.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.runtime.rememberCoroutineScope
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.repository.CaptureStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject

/**
 * The topmost view controller, which is what anything modal has to be presented from.
 *
 * Walked rather than held: Compose on iOS lives inside a controller the app shell owns, and
 * reaching for it through the key window is the only way that does not require plumbing a
 * reference down through every screen.
 */
@OptIn(ExperimentalForeignApi::class)
private fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull()
    val root = scene?.windows
        ?.filterIsInstance<platform.UIKit.UIWindow>()
        ?.firstOrNull { it.isKeyWindow() }
        ?.rootViewController
    return generateSequence(root) { it.presentedViewController }.lastOrNull()
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPhotoPicker(
    maxItems: Int,
    onPicked: (paths: List<String>) -> Unit,
): () -> Unit {
    val captureStore = koinInject<CaptureStore>()
    val scope = rememberCoroutineScope()
    val slots = maxItems.coerceAtLeast(1)

    // Read through a snapshot state so the delegate, which is built once, always calls the
    // callback this composition passed rather than the one it was born with.
    val currentOnPicked = rememberUpdatedState(onPicked)

    // Held for the length of the composition: PHPickerViewController keeps only a weak
    // reference to its delegate, and a per-call one would be collected before the traveller
    // finished choosing a photo.
    val delegate = remember(captureStore, scope) {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true, null)
                val providers = didFinishPicking
                    .filterIsInstance<PHPickerResult>()
                    .map { it.itemProvider }
                if (providers.isEmpty()) return

                // Indexed rather than appended: the load callbacks finish in whatever order
                // the photos happen to decode in, and the traveller expects the strip to
                // read in the order they tapped.
                val kept = arrayOfNulls<String>(providers.size)
                var outstanding = providers.size

                providers.forEachIndexed { index, provider ->
                    provider.loadFileRepresentationForTypeIdentifier(
                        UTTypeImage.identifier,
                    ) { url: NSURL?, error ->
                        // Copied here and not a moment later: iOS deletes the file this URL
                        // points at the instant this block returns, so handing the path to a
                        // coroutine — as an `importFromPicker` call would — races the
                        // deletion and usually loses.
                        val stored = url?.path?.let { source ->
                            val target = captureStore.newCapturePath()
                            val copied = NSFileManager.defaultManager.copyItemAtPath(
                                srcPath = source,
                                toPath = target,
                                error = null,
                            )
                            target.takeIf { copied }
                        }
                        if (stored == null) {
                            log.w { "Could not keep a picked photo: ${error?.localizedDescription}" }
                        }

                        // These callbacks arrive on arbitrary queues. The bookkeeping is
                        // funnelled onto the main dispatcher so the counter and the array
                        // are only ever touched from one of them.
                        scope.launch {
                            kept[index] = stored
                            outstanding--
                            if (outstanding == 0) {
                                val paths = kept.filterNotNull()
                                if (paths.isNotEmpty()) currentOnPicked.value(paths)
                            }
                        }
                    }
                }
            }
        }
    }

    return remember(delegate, slots) {
        {
            val configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = slots.toLong()
            }
            val picker = PHPickerViewController(configuration).apply { setDelegate(delegate) }
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberTextSharer(): (title: String, body: String) -> Unit = remember {
    { title, body ->
        // iOS has no subject field for a plain-text share, so the title is prepended to the
        // body — a mail app fills its own subject from the first line.
        val items = listOf(if (title.isBlank()) body else "$title\n\n$body")
        val controller = UIActivityViewController(
            activityItems = items,
            applicationActivities = null,
        )
        topViewController()?.presentViewController(controller, animated = true, completion = null)
    }
}

@Composable
actual fun rememberUrlOpener(): (url: String) -> Unit = remember {
    { url ->
        val target = NSURL.URLWithString(url)
        if (target == null) {
            log.w { "Not a URL: $url" }
        } else {
            UIApplication.sharedApplication.openURL(target, emptyMap<Any?, Any>()) { opened ->
                if (!opened) log.w { "Nothing on this device opens $url" }
            }
        }
    }
}

/** Referenced so the import stays honest about what a picker result carries. */
private typealias PickedItem = NSItemProvider

@Composable
actual fun rememberTextCopier(): (text: String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text ->
            scope.launch { clipboard.setClipEntry(ClipEntry.withPlainText(text)) }
        }
    }
}
