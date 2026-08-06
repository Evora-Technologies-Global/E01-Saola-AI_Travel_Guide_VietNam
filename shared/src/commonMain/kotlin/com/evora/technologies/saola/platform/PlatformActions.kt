package com.evora.technologies.saola.platform

import androidx.compose.runtime.Composable

/*
 * The handful of things a screen asks the operating system to do on its behalf.
 *
 * All composables, so each platform can hold whatever it needs for the length of the
 * composition — an `ActivityResultLauncher` on Android, a delegate object on iOS — without
 * that leaking into a screen that only wants a lambda to hand a button.
 */

/**
 * Opens the system photo picker and hands back paths inside app storage.
 *
 * The copies are what the paths point at, not the picker's own temporary grants: on Android
 * that grant is revoked when the process dies, and on iOS the URL points into a container the
 * system reclaims — either way the journal would be left holding a photo it can no longer read.
 *
 * @param maxItems how many photos the traveller may choose in this one trip through the
 *   picker. Passed to the system so the limit is enforced where they can see it, in the
 *   picker's own selection counter, rather than by silently dropping the surplus after the
 *   fact. Callers with room for one more pass 1; the picker then opens in single-select.
 * @param onPicked receives only the photos that imported successfully, in the order they
 *   were chosen. Never called with an empty list, including when the picker is dismissed.
 * @return a lambda that launches the picker. Nothing happens until it is called.
 */
@Composable
expect fun rememberPhotoPicker(
    maxItems: Int,
    onPicked: (paths: List<String>) -> Unit,
): () -> Unit

/**
 * Shares text through the system share sheet.
 *
 * @return a lambda taking the subject line — where the platform has one to fill — and the body.
 */
@Composable
expect fun rememberTextSharer(): (title: String, body: String) -> Unit

/**
 * Shares text and, where there is one, a photograph — addressed, where the platform allows it.
 *
 * Distinct from [rememberTextSharer] rather than a widening of it, because the two are
 * different acts. Sharing a discovery hands a paragraph to whoever the traveller chooses;
 * this hands a complaint to a named address with the evidence attached, and the attachment
 * is what makes it worth reading. Widening the existing one would have put four parameters
 * on the call every screen makes to share a place.
 *
 * **The recipient is a best effort, and the difference is visible to the traveller.** Android
 * fills the To: field of any mail app through `EXTRA_EMAIL`; iOS's share sheet has no
 * recipient field at all, so there the address is written into the body instead — see the
 * `IS_APPLE_PLATFORM` branch in `feature/discovery/ReportMail.kt`, which is exactly the kind
 * of *content* difference that constant exists for. Either way the traveller sees the whole
 * message before anything is sent, and can send it somewhere else entirely.
 *
 * @param attachmentPath an absolute path inside app storage, or null. On Android it is served
 *   through the `FileProvider` declared in the app manifest; a path outside the directories
 *   that provider declares throws, which is why this takes a capture path and not any file.
 */
@Composable
expect fun rememberMailSharer(): (
    recipient: String,
    subject: String,
    body: String,
    attachmentPath: String?,
) -> Unit

/**
 * Opens a URL in whatever app claims it — a maps link, in practice.
 *
 * Silently does nothing for a URL no installed app handles, which is the same outcome the
 * traveller sees either way and is not worth an error dialog over.
 */
@Composable
expect fun rememberUrlOpener(): (url: String) -> Unit

/**
 * Puts plain text on the system clipboard.
 *
 * `LocalClipboard` is multiplatform, but the `ClipEntry` it takes is not: on Android the type
 * wraps a `ClipData` and is built from one, while on iOS it is created through a companion
 * factory. Wrapping the whole operation is what keeps that difference out of the screen.
 */
@Composable
expect fun rememberTextCopier(): (text: String) -> Unit
