package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.Motion
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.feature.discovery.DiscoveryIntent
import com.duylt.trave.vietlensai.feature.discovery.NoteEditor
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_note_edit
import com.duylt.trave.vietlensai.resources.discovery_note_kicker
import org.jetbrains.compose.resources.stringResource

/**
 * The traveller's own note — the only thing on this page the app did not write.
 *
 * Everything above is Gemini's account of a place, and it all looks alike because it is all
 * the same voice. This block is deliberately the odd one out: dashed and empty until it is
 * filled, then the traveller's photos at full width above their own words. It is what turns
 * the record of *what was recognised* into a record of *what was lived*.
 *
 * @param onOpenPhoto given the strip it was tapped in as well as the position, because the
 *   composer's photos and the saved note's are two different lists and the viewer opens on
 *   whichever one is on screen.
 */
@Composable
internal fun NoteBlock(
    note: DiscoveryNote?,
    editor: NoteEditor?,
    isSaving: Boolean,
    language: AppLanguage,
    onCapture: () -> Unit,
    onOpenPhoto: (paths: List<String>, index: Int) -> Unit,
    onIntent: (DiscoveryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = when {
        editor != null -> NoteCard.COMPOSER
        note != null -> NoteCard.WRITTEN
        else -> NoteCard.BLANK
    }

    // The card being replaced stays composed for the length of the transition, and by then
    // the state it was drawn from is already gone — the editor is null the instant Cancel is
    // pressed, the note the instant it is deleted. Reading through the last value there was
    // is what lets the outgoing card leave with its own words and photos still on it, instead
    // of blanking on the first frame and dissolving an empty box.
    val leaving = rememberLastPresent(editor)
    val written = rememberLastPresent(note)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kicker(
                text = stringResource(Res.string.discovery_note_kicker),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            // Only offered once something is there to edit; the empty card is its own
            // invitation. It collapses towards the edge it sits against rather than fading
            // in place, so the row never holds a gap where a button used to be.
            AnimatedVisibility(
                visible = card == NoteCard.WRITTEN,
                enter = fadeIn(Motion.enter()) +
                    expandHorizontally(Motion.enter(), expandFrom = Alignment.End),
                exit = fadeOut(Motion.exit()) +
                    shrinkHorizontally(Motion.exit(), shrinkTowards = Alignment.End),
            ) {
                TextButton(onClick = { onIntent(DiscoveryIntent.StartEditNote) }) {
                    Text(
                        text = stringResource(Res.string.discovery_note_edit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        // The three cards are the same object in three states, not three different things,
        // so the block grows and shrinks between them rather than cutting. It is the one
        // animation on this page the traveller will see over and over — every note is
        // written, saved, reopened and saved again through exactly this swap.
        AnimatedContent(
            targetState = card,
            transitionSpec = { noteCardTransform() },
            modifier = Modifier.fillMaxWidth(),
            label = "noteCard",
        ) { target ->
            when (target) {
                NoteCard.COMPOSER -> leaving?.let { open ->
                    NoteComposer(
                        editor = open,
                        hasSavedNote = note != null,
                        isSaving = isSaving,
                        onCapture = onCapture,
                        onOpenPhoto = { index -> onOpenPhoto(open.photoPaths, index) },
                        onIntent = onIntent,
                    )
                }

                NoteCard.WRITTEN -> written?.let { saved ->
                    SavedNote(
                        note = saved,
                        language = language,
                        onOpenPhoto = { index -> onOpenPhoto(saved.photoPaths, index) },
                    )
                }

                NoteCard.BLANK -> EmptyNoteCard(
                    onStart = { onIntent(DiscoveryIntent.StartEditNote) },
                )
            }
        }
    }
}

/** Which of the note block's three faces is showing. */
private enum class NoteCard { BLANK, COMPOSER, WRITTEN }

/**
 * The card swaps by growing into the new one, not by sliding it in from a side.
 *
 * There is no left or right here — the composer *is* the saved note with its edges lifted —
 * so the only honest movement is the height change, which [SizeTransform] carries. The slight
 * scale on the way in stops the arriving card from looking like it was always there and merely
 * uncovered.
 */
private fun AnimatedContentTransitionScope<NoteCard>.noteCardTransform(): ContentTransform {
    val transform = (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = ARRIVE_SCALE))
        .togetherWith(fadeOut(Motion.exit()))
    return transform using SizeTransform { _, _ -> Motion.morph() }
}

/** Barely under full size: enough to read as arriving, not enough to read as zooming. */
private const val ARRIVE_SCALE = 0.96f
