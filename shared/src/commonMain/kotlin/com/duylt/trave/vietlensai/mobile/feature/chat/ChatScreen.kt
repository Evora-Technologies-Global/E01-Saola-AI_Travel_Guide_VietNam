package com.duylt.trave.vietlensai.mobile.feature.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeaderDefaults
import com.duylt.trave.vietlensai.core.designsystem.component.PinSystemBarIcons
import com.duylt.trave.vietlensai.core.designsystem.theme.GuidePalette
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.feature.chat.ChatIntent
import com.duylt.trave.vietlensai.feature.chat.ChatState
import com.duylt.trave.vietlensai.feature.chat.ChatViewModel
import com.duylt.trave.vietlensai.feature.chat.component.ChatComposer
import com.duylt.trave.vietlensai.feature.chat.component.ChatThread
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.chat_clear
import com.duylt.trave.vietlensai.resources.chat_subtitle
import com.duylt.trave.vietlensai.resources.chat_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The conversation about one place, drawn on paper rather than on a chat surface.
 *
 * A screen of its own, because a phone has room for one thing at a time: reading the story and
 * questioning it are two visits to two screens. The large-window arrangement is the same
 * conversation as a column beside the story — see `tablet/feature/discovery/`, which places
 * these same components without a route of its own.
 *
 * Colours come from `GuidePalette` and are fixed in both schemes: this screen is a page in the
 * same notebook as the passport and the journal cards, and a dark variant would make it a
 * different object rather than a dimmer one. Because of that the screen also pins the system
 * bars to dark icons for as long as it is on top — the cream band behind them never changes.
 */
@Composable
fun ChatRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The screen is cream in every theme, so the bar icons have to be dark whatever
    // the phone's own preference is.
    PinSystemBarIcons(darkIcons = true)

    ChatScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ChatScreen(
    state: ChatState,
    onIntent: (ChatIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GuidePalette.page,
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
                input = state.input,
                canSend = state.canSend,
                onIntent = onIntent,
                modifier = Modifier.imePadding().navigationBarsPadding(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ChatThread(state = state, onIntent = onIntent)
        }
    }
}

/**
 * The phone's heading, which is where the two arrangements genuinely part.
 *
 * `PageHeader` is the component and it is the same one the guide column draws; what differs is
 * what goes in it. Here there is a back chip, because the story is a screen behind this one. In
 * the two-pane arrangement the story is *beside* the guide, so there is nowhere for a back
 * button to go and the slot is simply not filled. That is arrangement, and it is why each
 * branch composes its own header rather than sharing a `ChatHeader` component — a shared one
 * would need a parameter for every difference and would end up a second header with a mode.
 */
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
    Surface(color = GuidePalette.header) {
        PageHeader(
            modifier = Modifier.screenInsetsPadding(),
            title = title,
            subtitle = stringResource(Res.string.chat_subtitle),
            onBack = onBack,
            // The lens palette, not the scheme: this screen is a page in the same
            // notebook as the passport, and it is cream in both themes by design —
            // see `GuidePalette` in `Color.kt` and `LLM.md` §12.
            colors = PageHeaderDefaults.colors(
                title = InkBrown,
                subtitle = GuidePalette.inkMuted,
                kicker = GuidePalette.inkMuted,
                backContainer = GuidePalette.backChip,
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
                            tint = GuidePalette.inkMuted,
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}
