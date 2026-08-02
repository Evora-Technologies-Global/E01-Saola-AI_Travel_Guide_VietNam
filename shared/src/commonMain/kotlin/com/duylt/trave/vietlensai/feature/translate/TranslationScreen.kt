package com.duylt.trave.vietlensai.feature.translate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.VisibleForTesting
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_back
import com.duylt.trave.vietlensai.resources.action_retry
import com.duylt.trave.vietlensai.resources.camera_translate_auto_detect
import com.duylt.trave.vietlensai.resources.translate_copied
import com.duylt.trave.vietlensai.resources.translate_copy
import com.duylt.trave.vietlensai.resources.translate_direction
import com.duylt.trave.vietlensai.resources.translate_leave_body
import com.duylt.trave.vietlensai.resources.translate_leave_confirm
import com.duylt.trave.vietlensai.resources.translate_leave_stay
import com.duylt.trave.vietlensai.resources.translate_leave_title
import com.duylt.trave.vietlensai.resources.translate_show_original
import com.duylt.trave.vietlensai.resources.translate_speak
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.showMessage
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.domain.model.TranslationBlock
import kotlinx.coroutines.launch
import androidx.compose.ui.backhandler.BackHandler
import com.duylt.trave.vietlensai.platform.rememberTextCopier

/**
 * The photo, with each translation laid over the words it replaces.
 *
 * A list of lines was easier to build and worse to use: a menu is read by pointing
 * at a row, and a translation in a separate column has to be matched back to the
 * page by counting. Drawn in place, the answer is where the question was — and the
 * original is still on the page around it, ready to be pointed at when the waiter
 * arrives.
 */
@Composable
fun TranslationRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranslationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaving is an effect rather than a button's own business: whether the arrow
    // closes the screen or asks a question first depends on work only the ViewModel
    // can see.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TranslationEffect.Close -> onBack()
            }
        }
    }

    TranslationScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/** Internal rather than private so an instrumented test can drive the gestures. */
@VisibleForTesting
@Composable
internal fun TranslationScreen(
    state: TranslationState,
    onIntent: (TranslationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copyText = rememberTextCopier()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(Res.string.translate_copied)

    // Claimed only while there is an unfinished translation to lose. Left enabled
    // the rest of the time, every back press would take a detour through the
    // ViewModel to arrive at the same pop the navigator would have done itself.
    BackHandler(enabled = state.isLoading) { onIntent(TranslationIntent.BackPressed) }

    // Black rather than the theme surface: the photo is the screen, and any other
    // colour around it reads as a frame somebody forgot to remove.
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        TranslationCanvas(
            state = state,
            onSelectBlock = { onIntent(TranslationIntent.SelectBlock(it)) },
            modifier = Modifier.fillMaxSize(),
        )

        TranslationTopBar(
            state = state,
            onBack = { onIntent(TranslationIntent.BackPressed) },
            onCopy = {
                state.translation?.let { translation ->
                    copyText(translation.translatedText)
                    // Tapped twice in a row, the second "copied" replaces the first
                    // rather than queueing behind it and confirming a copy the
                    // traveller made two taps ago.
                    scope.launch { snackbarHostState.showMessage(copiedMessage) }
                }
            },
            onSpeak = { onIntent(TranslationIntent.ToggleSpeech) },
            onShowOriginal = { onIntent(TranslationIntent.ShowOriginal(it)) },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        state.error?.let { error ->
            TranslationError(
                message = error.toUserMessage(),
                onRetry = { onIntent(TranslationIntent.Retry) },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        TranslationFooter(state = state, modifier = Modifier.align(Alignment.BottomCenter))

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    val selected = state.selectedBlock?.let { state.translation?.blocks?.getOrNull(it) }
    if (selected != null) {
        BlockDetailSheet(
            block = selected,
            onDismiss = { onIntent(TranslationIntent.DismissBlock) },
        )
    }

    if (state.confirmingExit) {
        LeaveConfirmationDialog(
            onConfirm = { onIntent(TranslationIntent.ConfirmExit) },
            onDismiss = { onIntent(TranslationIntent.DismissExitPrompt) },
        )
    }
}

/**
 * Asked only when leaving would throw away a translation still being fetched.
 *
 * Staying is the default action because that is the answer to an accidental back
 * swipe, which is what most of these presses are — the deliberate ones will find
 * "leave" either way.
 */
@Composable
private fun LeaveConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.translate_leave_title)) },
        text = { Text(stringResource(Res.string.translate_leave_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.translate_leave_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.translate_leave_stay))
            }
        },
    )
}

/**
 * The photo and its overlay, sharing one transform.
 *
 * Both sit inside a single zoomable box rather than being scaled separately: a
 * translation that drifts off its line under a pinch is worse than no zoom at all,
 * and the only way to guarantee it cannot is to make them one object.
 */
@Composable
private fun TranslationCanvas(
    state: TranslationState,
    onSelectBlock: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // Learned from the decoded photo rather than asked for up front: the frame has
    // to exist before the image can report its shape, so it starts at the sensor's
    // ratio and corrects itself the moment the file is read.
    var ratio by remember(state.imagePath) { mutableFloatStateOf(DEFAULT_RATIO) }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                    // Panning only means something once there is something
                    // off-screen to pan to; at 1× the photo snaps back to centred.
                    offset = if (scale <= 1f) {
                        Offset.Zero
                    } else {
                        val limitX = size.width * (scale - 1f) / 2f
                        val limitY = size.height * (scale - 1f) / 2f
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-limitX, limitX),
                            y = (offset.y + pan.y).coerceIn(-limitY, limitY),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The frame is measured, not filled: the boxes are fractions of the photo,
        // so they are only correct against the exact rectangle the photo occupies.
        // Letting the image letterbox itself inside a larger box would leave every
        // translation offset by however much black there was above it.
        val frameWidth: Dp
        val frameHeight: Dp
        if (maxWidth / maxHeight > ratio) {
            frameHeight = maxHeight
            frameWidth = maxHeight * ratio
        } else {
            frameWidth = maxWidth
            frameHeight = maxWidth / ratio
        }

        Box(
            modifier = Modifier
                .size(width = frameWidth, height = frameHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .clip(RoundedCornerShape(PHOTO_CORNER)),
        ) {
            AppAsyncImage(
                model = state.imagePath,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                // Repeated from the frame above rather than relied on: the parent clips
                // because the translation boxes have to be clipped too, but the shimmer
                // owning its own corners is what makes it survive being moved.
                shape = RoundedCornerShape(PHOTO_CORNER),
                onSuccess = { success ->
                    val size = success.painter.intrinsicSize
                    if (size.isSpecified && size.height > 0f) ratio = size.width / size.height
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isLoading) {
                ShimmerScrim(modifier = Modifier.fillMaxSize())
            }

            // Hidden rather than dropped while the original is being read: the
            // boxes come straight back on release, in exactly the same places.
            if (!state.showingOriginal) {
                state.translation?.blocks?.forEachIndexed { index, block ->
                    val box = block.box ?: return@forEachIndexed
                    // Bled outwards by a fraction of the line's own height: the
                    // recogniser's box stops at the glyphs it was sure of, which
                    // leaves the first and last letter of the original poking out
                    // from under its replacement.
                    val bleed = frameHeight * box.height * BOX_BLEED
                    val left = (frameWidth * box.left - bleed).coerceAtLeast(0.dp)
                    val top = (frameHeight * box.top - bleed).coerceAtLeast(0.dp)
                    TranslatedBlock(
                        block = block,
                        left = left,
                        top = top,
                        width = frameWidth * box.width + bleed * 2,
                        height = frameHeight * box.height + bleed * 2,
                        // What the line may grow into if the translation runs longer
                        // than the words it covers, which English usually does.
                        maxWidth = frameWidth - left,
                        onClick = { onSelectBlock(index) },
                    )
                }
            }
        }
    }
}

/**
 * One translated line, sitting on the pixels the original occupied.
 *
 * Painted over rather than beside: leaving the source showing through would put
 * two languages in the same few millimetres and make neither readable. The
 * original is one tap — or one press of the eye — away.
 */
@Composable
private fun TranslatedBlock(
    block: TranslationBlock,
    left: Dp,
    top: Dp,
    width: Dp,
    height: Dp,
    maxWidth: Dp,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    // The height the printed line had is the size to aim for, so the replacement
    // reads at the scale of the page. Clamped because a recogniser box a few pixels
    // tall would otherwise ask for type nobody can read.
    val preferred = with(density) { (height.toPx() * FONT_FILL_RATIO).toSp() }
        .value
        .coerceIn(MIN_FONT_SP, MAX_FONT_SP)
        .sp

    Box(
        modifier = Modifier
            .offset { IntOffset(left.roundToPx(), top.roundToPx()) }
            // Allowed to outgrow the line it covers: a box pinned to the original's
            // width would have to set "Fried rice with beef and pickled mustard
            // greens" in the space "Cơm rang dưa bò" took, and it would lose.
            .height(height)
            .widthIn(min = width, max = maxWidth)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = block.translated,
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            // Shrunk to fit rather than truncated: English runs longer than the
            // Vietnamese it covers, and "Hanoi-style grilled pork…" answers less
            // than the same words set two points smaller.
            autoSize = TextAutoSize.StepBased(
                minFontSize = MIN_FONT_SP.sp,
                maxFontSize = preferred,
                stepSize = 0.5.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/**
 * The wait, drawn over the traveller's own photo.
 *
 * A sweep across the picture rather than a spinner in a grey box: the photo is
 * already there and already the point, so the loading state only has to say "still
 * working" — it has no missing content to stand in for.
 */
@Composable
private fun ShimmerScrim(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "translationShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "translationShimmerSweep",
    )

    BoxWithConstraints(modifier = modifier.background(Color.Black.copy(alpha = 0.35f))) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        // Starts off the left edge and ends off the right, so the band enters and
        // leaves rather than appearing in the middle of the frame.
        val head = widthPx * (progress * 2f - 0.5f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        startX = head - widthPx * SHIMMER_BAND,
                        endX = head + widthPx * SHIMMER_BAND,
                    ),
                ),
        )
    }
}

@Composable
private fun TranslationTopBar(
    state: TranslationState,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onShowOriginal: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)),
            )
            .screenInsetsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.action_back),
                tint = Color.White,
            )
        }

        Spacer(Modifier.weight(1f))

        if (state.translation == null) return@Row

        // Press and hold rather than a toggle: peeking at the original is a glance,
        // and a toggle leaves the traveller in a state they have to remember to get
        // out of. Driven by the button's own press state rather than by a second
        // gesture detector, which would be competing with its click for the same
        // finger.
        val peekInteraction = remember { MutableInteractionSource() }
        val peeking by peekInteraction.collectIsPressedAsState()
        LaunchedEffect(peeking) { onShowOriginal(peeking) }

        GlassButton(onClick = {}, interactionSource = peekInteraction) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = stringResource(Res.string.translate_show_original),
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(8.dp))
        GlassButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(Res.string.translate_copy),
                tint = Color.White,
            )
        }
        Spacer(Modifier.width(8.dp))
        GlassButton(onClick = onSpeak) {
            Icon(
                imageVector = if (state.isSpeaking) {
                    Icons.Filled.Stop
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = stringResource(Res.string.translate_speak),
                tint = Color.White,
            )
        }
    }
}

/**
 * What was read, and which way round.
 *
 * Under the photo rather than in the title bar: it answers a question that only
 * comes up once the translation is on screen — "is this even the right language?"
 * — so it belongs where the eye ends up, not where it started.
 *
 * The model's summary of what the text is used to sit under this line and has been
 * dropped: by the time it could be read the traveller was already looking at the
 * thing it described, and two ellipsised lines of prose earned none of the space
 * they took from the photo. It is still fetched, and still stored with the result.
 */
@Composable
private fun TranslationFooter(state: TranslationState, modifier: Modifier = Modifier) {
    val detected = state.translation?.detectedLanguage.orEmpty()
    val fallback = stringResource(Res.string.camera_translate_auto_detect)

    AnimatedVisibility(
        visible = state.translation != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                    ),
                )
                .navigationBarsPadding()
                .padding(horizontal = ScreenGutter, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(
                    Res.string.translate_direction,
                    detected.ifBlank { fallback },
                    state.targetLanguage.displayName,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TranslationError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(ScreenGutter)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(role = Role.Button, onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.action_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/** The original line, its translation, and anything the model wanted to add. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockDetailSheet(block: TranslationBlock, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = block.original,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = block.translated,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            block.price?.let { price ->
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            block.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A round control that stays legible over whatever part of the photo it lands on. */
@Composable
private fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}



/** How much of the original line's height the replacement type fills. */
private const val FONT_FILL_RATIO = 0.62f

/** How far a block reaches past the recogniser's box, as a fraction of its height. */
private const val BOX_BLEED = 0.14f
private const val MIN_FONT_SP = 8f
private const val MAX_FONT_SP = 26f

/** Half the width of the shimmer band, as a fraction of the photo. */
private const val SHIMMER_BAND = 0.35f

private const val MAX_ZOOM = 4f

/** Used until the photo reports its own shape — the sensor's 4:3, portrait. */
private const val DEFAULT_RATIO = 3f / 4f

/** Softened just enough to read as a photo on a screen rather than a raw frame. */
private val PHOTO_CORNER = 20.dp
