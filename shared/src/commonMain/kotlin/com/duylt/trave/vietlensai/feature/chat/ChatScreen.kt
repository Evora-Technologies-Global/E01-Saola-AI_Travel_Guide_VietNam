package com.duylt.trave.vietlensai.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.chat_clear
import com.duylt.trave.vietlensai.resources.chat_empty_body
import com.duylt.trave.vietlensai.resources.chat_empty_title
import com.duylt.trave.vietlensai.resources.chat_input_hint
import com.duylt.trave.vietlensai.resources.chat_send
import com.duylt.trave.vietlensai.resources.chat_subtitle
import com.duylt.trave.vietlensai.resources.chat_suggestions_kicker
import com.duylt.trave.vietlensai.resources.chat_thinking
import com.duylt.trave.vietlensai.resources.chat_title
import com.duylt.trave.vietlensai.resources.discovery_listen
import com.duylt.trave.vietlensai.resources.discovery_stop
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeaderDefaults
import com.duylt.trave.vietlensai.core.designsystem.component.PinSystemBarIcons
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Corner
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.domain.model.ChatMessage
import com.duylt.trave.vietlensai.domain.model.ChatRole

/**
 * The conversation about one place, drawn on paper rather than on a chat surface.
 *
 * Colours are fixed to the lens palette instead of taken from the colour scheme:
 * this screen is a page in the same notebook as the passport and the journal
 * cards, and a dark variant would make it a different object rather than a dimmer
 * one. Because of that the screen also pins the system bars to dark icons for as
 * long as it is on top — the cream band behind them never changes.
 */
private val PageBackground = PaperCream
private val HeaderBackground = Marigold.copy(alpha = 0.10f).compositeOver(PaperCream)
private val ComposerBackground = Vermilion.copy(alpha = 0.07f).compositeOver(PaperCream)
private val GuideCard = Color(0xFFFFFCF6)
private val GuideCardBorder = Marigold.copy(alpha = 0.35f)
private val InkMuted = InkBrown.copy(alpha = 0.60f)
private val Hairline = InkBrown.copy(alpha = 0.10f)
private const val BACK_CHIP_ALPHA = 0.07f

/** `VietLensShapes.large` and `extraSmall`, as numbers so one bubble can carry both. */
private val BubbleCorner = Corner.large
private val BubbleTail = Corner.extraSmall

@Composable
fun ChatRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // The screen is cream in every theme, so the bar icons have to be dark whatever
    // the phone's own preference is.
    PinSystemBarIcons(darkIcons = true)

    // Driven by message count rather than by a one-shot effect so the scroll happens
    // after the new row has actually been laid out — an effect sent from the ViewModel
    // arrives while the list is still the length it was. That is why ChatEffect carries no
    // scroll event for this route to collect. While a question is in flight the thinking
    // card is the last row, one past the final message.
    LaunchedEffect(state.messages.size, state.isSending) {
        if (state.messages.isEmpty()) return@LaunchedEffect
        val target = if (state.isSending) state.messages.size else state.messages.lastIndex
        listState.animateScrollToItem(target)
    }

    ChatScreen(
        state = state,
        listState = listState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ChatScreen(
    state: ChatState,
    listState: LazyListState,
    onIntent: (ChatIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PageBackground,
        // The shell applies no insets to pushed screens; the header and the
        // composer each take the edge they touch.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ChatHeader(
                title = state.discovery?.title ?: stringResource(Res.string.chat_title),
                canClear = state.messages.isNotEmpty(),
                onBack = onBack,
                onClear = { onIntent(ChatIntent.ClearThread) },
            )
        },
        bottomBar = {
            ChatComposer(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.imePadding().navigationBarsPadding(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.messages.isEmpty() && state.suggestions.isEmpty()) {
                ChatEmptyState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = ScreenGutter,
                        vertical = Spacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (state.messages.isEmpty()) {
                        item(key = "suggestions-kicker") {
                            Kicker(
                                text = stringResource(Res.string.chat_suggestions_kicker),
                                color = InkMuted,
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
                        MessageBubble(
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
                        item(key = "thinking") { ThinkingCard() }
                    }
                }
            }

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
}

@Composable
private fun ChatHeader(
    title: String,
    canClear: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    // The inset goes on `PageHeader`, NOT on the `Surface`. Material3 chains the caller's
    // modifier ahead of its own `.background(...)`, so an inset passed to the Surface would
    // shrink the painted band rather than the content inside it — leaving a bare cream
    // strip above a tinted header, seamed across the top of the screen. The band has to run
    // under the notch; only the title stops short of it. This screen takes the inset at all
    // because it is a `Scaffold` with its own window insets switched off.
    Surface(color = HeaderBackground) {
        PageHeader(
            modifier = Modifier.screenInsetsPadding(),
            title = title,
            subtitle = stringResource(Res.string.chat_subtitle),
            onBack = onBack,
            // The lens palette, not the scheme: this screen is a page in the same
            // notebook as the passport, and it is cream in both themes by design —
            // see the note on `PageBackground` above and `LLM.md` §12.
            colors = PageHeaderDefaults.colors(
                title = InkBrown,
                subtitle = InkMuted,
                kicker = InkMuted,
                backContainer = InkBrown.copy(alpha = BACK_CHIP_ALPHA),
                backContent = InkBrown,
            ),
            // Null rather than a lambda that draws nothing: `PageHeader` spaces the slot on
            // the parameter being present, so an always-supplied trailing would hold an 8 dp
            // gap open on every empty thread.
            trailing = if (canClear) {
                {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(Res.string.chat_clear),
                            tint = InkMuted,
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}

/** Shown only when the discovery carried no suggested questions of its own. */
@Composable
private fun ChatEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = ScreenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.chat_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = InkBrown,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(Res.string.chat_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                color = if (isUser) Vermilion else GuideCard,
                contentColor = if (isUser) PaperCream else InkBrown,
                border = if (isUser) null else BorderStroke(1.dp, GuideCardBorder),
                // The app's `large` corner on three sides and its `extraSmall` on the
                // one nearest the speaker: the clipped corner is what points a bubble at
                // whoever said it, and it is the only place these two radii ever meet.
                shape = RoundedCornerShape(
                    topStart = BubbleCorner,
                    topEnd = BubbleCorner,
                    bottomStart = if (isUser) BubbleCorner else BubbleTail,
                    bottomEnd = if (isUser) BubbleTail else BubbleCorner,
                ),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(
                        horizontal = Spacing.lg,
                        vertical = Spacing.md,
                    ),
                )
            }
            // Only the guide's answers are worth reading aloud.
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .clip(Pill)
                        .clickable(onClick = onSpeak)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isSpeaking) {
                            Icons.Filled.Stop
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = null,
                        tint = Vermilion,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(
                            if (isSpeaking) Res.string.discovery_stop else Res.string.discovery_listen,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Vermilion,
                    )
                }
            }
        }
    }
}

/**
 * A suggested question: an outlined pill that hugs its own text.
 *
 * Left-aligned and content-width rather than a full-bleed row, so the three of
 * them read as things the traveller could say, not as a settings list.
 */
@Composable
private fun SuggestionPill(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = Pill,
        color = Color.Transparent,
        contentColor = Vermilion,
        border = BorderStroke(1.dp, Vermilion.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}

@Composable
private fun ThinkingCard() {
    Surface(
        shape = RoundedCornerShape(
            topStart = BubbleCorner,
            topEnd = BubbleCorner,
            bottomStart = BubbleTail,
            bottomEnd = BubbleCorner,
        ),
        color = GuideCard,
        contentColor = InkMuted,
        border = BorderStroke(1.dp, GuideCardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Vermilion,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(Res.string.chat_thinking),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ChatComposer(
    state: ChatState,
    onIntent: (ChatIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = ComposerBackground) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = { onIntent(ChatIntent.InputChanged(it)) },
                placeholder = {
                    Text(
                        text = stringResource(Res.string.chat_input_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PageBackground,
                    unfocusedContainerColor = PageBackground,
                    focusedIndicatorColor = Vermilion.copy(alpha = 0.45f),
                    unfocusedIndicatorColor = Hairline,
                    cursorColor = Vermilion,
                    focusedTextColor = InkBrown,
                    unfocusedTextColor = InkBrown,
                    focusedPlaceholderColor = InkMuted,
                    unfocusedPlaceholderColor = InkMuted,
                ),
            )
            Spacer(Modifier.width(Spacing.md))
            SendButton(
                enabled = state.canSend,
                onClick = { onIntent(ChatIntent.SubmitQuestion()) },
            )
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    FilledCircleButton(
        icon = Icons.AutoMirrored.Filled.Send,
        contentDescription = stringResource(Res.string.chat_send),
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun FilledCircleButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // Dimmed rather than hidden while there is nothing to send: the control keeps
    // its place in the row, and a tap that would do nothing does not invite one.
    val background = if (enabled) Vermilion else Vermilion.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PaperCream,
            modifier = Modifier.size(22.dp),
        )
    }
}
