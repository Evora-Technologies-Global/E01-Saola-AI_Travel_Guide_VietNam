package com.evora.technologies.saola.feature.chat.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.GuidePalette
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.core.util.toUserMessage
import com.evora.technologies.saola.feature.chat.ChatIntent
import com.evora.technologies.saola.feature.chat.ChatState
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.chat_suggestions_kicker
import org.jetbrains.compose.resources.stringResource

/**
 * The conversation itself: what has been said, what is being waited for, what could be asked.
 *
 * The whole of it, rather than the bubbles alone, because the *order* is the part both
 * arrangements have to agree on — suggestions only before the first question, the thinking
 * card one row past the last message, the failure banner over the foot of the list. A phone
 * page and a 352 dp column beside a story are two amounts of room for one thread, and nothing
 * above this composable has an opinion about how a thread is built.
 *
 * It owns its own `LazyListState`, and that is deliberate: the scroll position belongs to the
 * list, the auto-scroll below is the only thing that reads it, and lifting it out would hand
 * every caller a value it has no use for.
 *
 * @param contentPadding what the arrangement gives the thread to breathe. The phone has a page
 *   to spend; the guide column is narrow and keeps a tighter gutter of its own.
 */
@Composable
internal fun ChatThread(
    state: ChatState,
    onIntent: (ChatIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = ScreenGutter, vertical = Spacing.xl),
) {
    val listState = rememberLazyListState()

    // Driven by message count rather than by a one-shot effect so the scroll happens after the
    // new row has actually been laid out — an effect sent from the ViewModel arrives while the
    // list is still the length it was. That is why `ChatEffect` carries no scroll event for a
    // route to collect. While a question is in flight the thinking card is the last row, one
    // past the final message.
    LaunchedEffect(state.messages.size, state.isSending) {
        if (state.messages.isEmpty()) return@LaunchedEffect
        val target = if (state.isSending) state.messages.size else state.messages.lastIndex
        listState.animateScrollToItem(target)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.messages.isEmpty() && state.suggestions.isEmpty()) {
            ChatEmptyState()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (state.messages.isEmpty()) {
                    item(key = SUGGESTIONS_KICKER_KEY) {
                        Kicker(
                            text = stringResource(Res.string.chat_suggestions_kicker),
                            color = GuidePalette.inkMuted,
                            modifier = Modifier.padding(bottom = Spacing.xs),
                        )
                    }
                    items(state.suggestions, key = { it }) { suggestion ->
                        SuggestionPill(
                            text = suggestion,
                            onClick = { onIntent(ChatIntent.SubmitQuestion(suggestion)) },
                        )
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        isSpeaking = state.speakingMessageId == message.id,
                        onSpeak = {
                            if (state.speakingMessageId == message.id) {
                                onIntent(ChatIntent.StopSpeaking)
                            } else {
                                onIntent(ChatIntent.SpeakMessage(message.id))
                            }
                        },
                    )
                }

                if (state.isSending) {
                    item(key = THINKING_KEY) { ThinkingCard() }
                }
            }
        }

        // Drawn from state rather than raised as an effect, and `ChatContract` says why: the
        // banner is on screen until it is dismissed, which makes it something the screen
        // renders rather than something that happens to it once.
        state.error?.let { error ->
            Surface(
                color = Vermilion,
                contentColor = PaperCream,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(ScreenGutter)
                    .clickable { onIntent(ChatIntent.DismissError) },
            ) {
                Text(
                    text = error.toUserMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }
    }
}

private const val SUGGESTIONS_KICKER_KEY = "suggestions-kicker"
private const val THINKING_KEY = "thinking"
