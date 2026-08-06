package com.evora.technologies.saola.feature.discovery

import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.feature.discovery.component.labelRes
import com.evora.technologies.saola.platform.IS_APPLE_PLATFORM
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.report_mail_intro
import com.evora.technologies.saola.resources.report_mail_note
import com.evora.technologies.saola.resources.report_mail_reason
import com.evora.technologies.saola.resources.report_mail_subject
import com.evora.technologies.saola.resources.report_mail_to
import org.jetbrains.compose.resources.getString
import kotlin.math.roundToInt

/**
 * Where a report is sent, and what it says when it gets there.
 *
 * Both arrangements compose the same message, which is the whole reason this is not a private
 * function in either screen: the phone and the large window differ in where the row sits, and
 * a second copy of the body would be a second version of what the team receives — silently
 * different in whichever one nobody re-read.
 *
 * **Everything here resolves outside composition.** [buildReportBody] is `suspend` and uses
 * `getString`, not `stringResource`, because the mail is composed while handling
 * `DiscoveryEffect.SendReport` — a plain suspend lambda that runs one main-queue turn after
 * `sendEffect`, before any frame that could have produced a string. Resolving in composition
 * and reading the result here is the defect `LLM.md` §11 row #15 describes.
 */

/**
 * The address a report is offered to.
 *
 * One constant, deliberately: it is the single thing in this feature that is a deployment
 * decision rather than a design one, and changing it must not mean finding it in two branches.
 * It is a *default*, not a destination — the share sheet lets the traveller send the message
 * anywhere, and on iOS there is no To: field to prefill at all (see [buildReportBody]).
 *
 * TODO: replace with the real support address before this ships.
 */
internal const val REPORT_RECIPIENT = "beedyto@gmail.com"

/** "Saola report — Chùa Một Cột". The place, so a mailbox of these sorts itself. */
internal suspend fun reportSubject(discovery: Discovery): String =
    getString(Res.string.report_mail_subject, discovery.title)

/**
 * The message body: what the traveller said, then what they said it about.
 *
 * The two halves are treated differently on purpose. The traveller's half is translated,
 * because it is theirs and they may well be asked about it. The details block below it is
 * **not** — it is read by whoever fixes the result, it is a diagnostic rather than a sentence,
 * and eight translations of the word "confidence" would be eight more strings to keep aligned
 * for no reader who benefits.
 */
internal suspend fun buildReportBody(
    report: DiscoveryReport,
    discovery: Discovery,
    language: AppLanguage,
): String = buildString {
    // iOS presents a `UIActivityViewController`, which has no recipient field for
    // `rememberMailSharer` to fill, so on that platform the address has to be in the text
    // where it can be read and copied. On Android `EXTRA_EMAIL` already fills the To: field
    // and this line would say the same thing twice. Exactly the kind of *content* difference
    // `IS_APPLE_PLATFORM` is documented for.
    if (IS_APPLE_PLATFORM) {
        appendLine(getString(Res.string.report_mail_to, REPORT_RECIPIENT))
        appendLine()
    }

    appendLine(getString(Res.string.report_mail_intro))
    appendLine()
    appendLine(getString(Res.string.report_mail_reason, getString(report.reason.labelRes)))
    if (report.note.isNotBlank()) {
        appendLine(getString(Res.string.report_mail_note, report.note))
    }

    appendLine()
    appendLine("--- details ---")
    appendLine("result: ${discovery.title}")
    appendLine("id: ${discovery.id}")
    appendLine("model: ${discovery.modelUsed ?: "unknown"}")
    appendLine("confidence: ${(discovery.confidence * PERCENT).roundToInt()}%")
    appendLine("captured: ${discovery.createdAt}")
    discovery.location?.let { at -> appendLine("location: ${at.latitude}, ${at.longitude}") }
    // The language the account was *written in*, which is the one the complaint is about —
    // a wrong date in the Vietnamese narration may be right in the English one.
    appendLine("language: ${language.code}")
    appendLine("reported: ${report.createdAt}")
    // Stated even when there is one, so a report that arrives with no image is
    // distinguishable from a report whose attachment was stripped in transit.
    appendLine("photo: ${if (discovery.imagePath != null) "attached" else "none"}")
}

private const val PERCENT = 100
