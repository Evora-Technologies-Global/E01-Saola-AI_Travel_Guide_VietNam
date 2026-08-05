package com.duylt.trave.vietlensai.feature.sovereignty.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion

// The four fixed values the statement is printed in, in one place because five composables
// draw on them and two presentation branches arrange those five.
//
// The same argument `GuidePalette` in `Color.kt` settles for the conversation, one step
// smaller: these are derived from Vermilion and PaperCream rather than being colours of their
// own, and only this feature draws them. A copy of `SeaWash` in the tablet branch would be a
// second wash a shade away from the first, on a page whose whole point is that it looks
// identical everywhere.

/**
 * The panels: the page's own lacquer, darkened.
 *
 * Composited to an opaque colour rather than left as translucent black. Laid over the page as
 * a wash, the hatching underneath shows through and crosses each panel's own hatching at a
 * different phase, which reads as a printing fault rather than as texture.
 */
internal val SeaWash = Color.Black.copy(alpha = 0.18f).compositeOver(Vermilion)

/** The disc behind the compass mark and behind the close button — cream, barely there. */
internal val LacquerChip = PaperCream.copy(alpha = 0.14f)

/** The second voice: the supporting paragraph and the note under the seal. */
internal const val SOVEREIGNTY_SECONDARY_ALPHA = 0.72f

/** The hatching inside a panel, far lighter than the page's own so the two do not beat. */
internal const val SOVEREIGNTY_PANEL_HATCH_ALPHA = 0.04f
