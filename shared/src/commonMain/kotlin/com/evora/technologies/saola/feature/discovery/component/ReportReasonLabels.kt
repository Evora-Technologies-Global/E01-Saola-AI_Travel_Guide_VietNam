package com.evora.technologies.saola.feature.discovery.component

import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.report_reason_inappropriate
import com.evora.technologies.saola.resources.report_reason_other
import com.evora.technologies.saola.resources.report_reason_wrong_facts
import com.evora.technologies.saola.resources.report_reason_wrong_name
import org.jetbrains.compose.resources.StringResource

/**
 * Which words name each kind of wrong — chosen without deciding how they will be read.
 *
 * The same split `core/util/ErrorMessages.kt` makes, for the same reason and with the same
 * cost of getting it wrong. Three places need this text and they cannot all call each other:
 * the sheet's chips and the footer's line resolve it inside composition with `stringResource`,
 * and `ReportMail` resolves it in a suspend function with `getString`, outside composition,
 * because the mail is composed while handling an effect. One `when` means the reason the
 * traveller ticked and the reason the mail states can never drift apart.
 *
 * A [StringResource] rather than a `@Composable` accessor precisely so the third caller
 * exists: a composable one would have forced the mail body to be built during a frame, which
 * is the mistake `LLM.md` §11 row #15 is about.
 */
internal val ReportReason.labelRes: StringResource
    get() = when (this) {
        ReportReason.WRONG_NAME -> Res.string.report_reason_wrong_name
        ReportReason.WRONG_FACTS -> Res.string.report_reason_wrong_facts
        ReportReason.INAPPROPRIATE -> Res.string.report_reason_inappropriate
        ReportReason.OTHER -> Res.string.report_reason_other
    }
