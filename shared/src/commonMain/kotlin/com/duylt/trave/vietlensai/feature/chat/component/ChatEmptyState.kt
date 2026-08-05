package com.duylt.trave.vietlensai.feature.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.duylt.trave.vietlensai.core.designsystem.theme.GuidePalette
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.chat_empty_body
import com.duylt.trave.vietlensai.resources.chat_empty_title
import org.jetbrains.compose.resources.stringResource

/**
 * Shown only when the discovery carried no suggested questions of its own.
 *
 * With questions there is nothing empty about the thread — the three pills *are* the content,
 * and this would be a notice sitting on top of them saying there is nothing here.
 */
@Composable
internal fun ChatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = ScreenGutter),
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
            color = GuidePalette.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
