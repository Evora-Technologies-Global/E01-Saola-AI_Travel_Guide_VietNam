package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.evora.technologies.saola.core.designsystem.component.AppAsyncImage
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.component.OverlayHeader
import com.evora.technologies.saola.core.designsystem.component.OverlayHeaderStyle
import com.evora.technologies.saola.core.designsystem.component.OverlayIconButton
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_close
import com.evora.technologies.saola.resources.discovery_note_photo_position
import org.jetbrains.compose.resources.stringResource

/**
 * A note's photo strip, frozen at the moment one of its photos was tapped.
 *
 * Snapshotted rather than read live while open, because the viewer covers the page it came
 * from: nothing can add or remove a photo behind it, and a list that cannot change is one less
 * thing for the pager to be paging through.
 *
 * @param index which of [paths] to open on.
 */
internal data class PhotoAlbum(val paths: List<String>, val index: Int)

/**
 * A note's photos at full size, the way they were meant to be looked at.
 *
 * The strip is a set of 104dp thumbnails — enough to know which photo is which, not enough to
 * see anything in one. This is where a face, a menu or a plaque actually becomes legible, so
 * it is a viewer and nothing else: no delete, no share, no edit. Removing a photo stays with
 * the badge on the strip, where the traveller can see what is left of the note while they
 * do it.
 */
@Composable
internal fun NotePhotoViewer(
    album: PhotoAlbum,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = album.index) { album.paths.size }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Room to breathe between photos, so a half-swipe shows black rather than two
            // images touching — nothing else on this screen tells the eye where one ends.
            pageSpacing = Spacing.md,
        ) { page ->
            ZoomablePhoto(
                path = album.paths[page],
                // Swiping away from a photo leaves it magnified underneath, and coming back
                // to it later at 3× on some corner is disorienting. Each one is put back the
                // way it was found.
                isSettled = pagerState.currentPage == page,
            )
        }

        OverlayHeader(
            style = OverlayHeaderStyle.Plain,
            modifier = Modifier.align(Alignment.TopCenter),
            leading = {
                OverlayIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onClose,
                )
            },
        )

        // Shown even for a note with a single photo, where it says nothing new. On a screen
        // that is otherwise edge-to-edge photograph it is the one mark that this is the app
        // and not the phone's own gallery — and with more than one it is also the only clue
        // that swiping does anything at all.
        Kicker(
            text = stringResource(
                Res.string.discovery_note_photo_position,
                pagerState.currentPage + 1,
                album.paths.size,
            ),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Spacing.xxl)
                .clip(Pill)
                // Dark enough to hold white text over a white photo, which the strip is full
                // of: half of what a traveller keeps is a screenshot or a menu.
                .background(Color.Black.copy(alpha = COUNTER_ALPHA))
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

/**
 * One photo, pinchable and pannable, that still lets the pager have its swipe.
 *
 * `canPan` is the whole trick: at rest a one-finger drag never passes this gesture's touch
 * slop, so it is never consumed here and reaches the pager. Once the photo is magnified the
 * same drag becomes a pan, which is right — there is now somewhere to drag *to*, and moving
 * to the next photo while looking closely at this one is not what the finger meant.
 */
@Composable
private fun ZoomablePhoto(path: String, isSettled: Boolean) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var frame by remember { mutableStateOf(IntSize.Zero) }

    /** Keeps the magnified photo's edges from being dragged inside the frame. */
    fun clamp(candidate: Offset, atScale: Float): Offset {
        if (atScale <= 1f) return Offset.Zero
        val limitX = frame.width * (atScale - 1f) / 2f
        val limitY = frame.height * (atScale - 1f) / 2f
        return Offset(
            x = candidate.x.coerceIn(-limitX, limitX),
            y = candidate.y.coerceIn(-limitY, limitY),
        )
    }

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(1f, MAX_PHOTO_ZOOM)
        // Zoom about the point between the fingers, not the middle of the screen: pinching
        // on a face in the corner should bring that face closer, not push it off the edge.
        val fromCentre = centroid - Offset(frame.width / 2f, frame.height / 2f)
        val anchored = fromCentre - (fromCentre - offset) * (next / scale)
        offset = clamp(anchored + panChange, next)
        scale = next
    }

    LaunchedEffect(isSettled) {
        if (!isSettled) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    // Snapped while the fingers are down and animated the rest of the time. Both matter:
    // a pinch that lags behind the hand feels broken, and a double-tap that arrives at 2.5×
    // in one frame is a jump cut rather than a zoom.
    val live = transformState.isTransformInProgress
    val zoom by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (live) snap() else Motion.morph(Motion.QUICK_MILLIS),
        label = "photoZoom",
    )
    val pan by animateOffsetAsState(
        targetValue = offset,
        animationSpec = if (live) snap() else Motion.morph(Motion.QUICK_MILLIS),
        label = "photoPan",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { frame = it }
            .transformable(
                state = transformState,
                canPan = { scale > 1f },
                lockRotationOnZoomPan = true,
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    // The shortcut for people who never pinch: one tap in, one tap back.
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_ZOOM
                            offset = Offset.Zero
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AppAsyncImage(
            model = path,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // Barely-there white rather than the surface a card would use: this photo
            // hangs on black, and a sand-coloured rectangle filling the display while
            // the file is read would be the brightest thing the viewer ever shows.
            placeholderColor = Color.White.copy(alpha = VIEWER_PLACEHOLDER_ALPHA),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                },
        )
    }
}

/** The ground the viewer's shimmer sweeps across, laid over its black. */
private const val VIEWER_PLACEHOLDER_ALPHA = 0.07f

/** Dark enough to hold the counter over a photograph of a white wall. */
private const val COUNTER_ALPHA = 0.62f

/**
 * How far into a note photo the viewer will go.
 *
 * Four is past what a phone camera has detail for, which is the point: the ceiling should be
 * reached because the photo ran out, not because the app said so.
 */
private const val MAX_PHOTO_ZOOM = 4f

/** Close enough to read a sign, far enough that a second tap is still worth having. */
private const val DOUBLE_TAP_ZOOM = 2.5f
