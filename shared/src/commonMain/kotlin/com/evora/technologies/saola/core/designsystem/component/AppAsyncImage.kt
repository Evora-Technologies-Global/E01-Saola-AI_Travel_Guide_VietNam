package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.domain.util.SAOLA_USER_AGENT

/**
 * Every photograph in the app, with its own wait and its own failure drawn in.
 *
 * The reason this exists rather than each screen calling Coil directly is the frame. A
 * photograph here is almost never a bare rectangle — it is a rounded tile in the journal,
 * a square in the passport strip, a bordered card in the lens pile — and the shape lives
 * in the *container*, so a placeholder drawn as a sibling of the image inherits it for
 * free. [shape] and [border] are applied once, to the box both the shimmer and the
 * photograph sit inside, which is what keeps the corners and the border identical across
 * the moment the image lands. There is nothing for a call site to keep in sync, because
 * there is only one of each value.
 *
 * The three states are deliberately distinct. Loading sweeps, because something is coming.
 * Failure stops and says so, because nothing is: a tile that shimmers forever over a photo
 * the traveller deleted from their gallery is a lie the app keeps telling. And success is
 * simply the photograph, with the placeholder fading out over it rather than cutting.
 *
 * @param model anything Coil accepts — here, almost always a local file path. A null one
 *   is not a special case: it resolves to [Phase.Failed] like any other unreadable
 *   source, which is right, because to the traveller a photo that never saved and a photo
 *   that will not open are the same photo.
 * @param shape carried by the container, so the shimmer, the failure state and the
 *   photograph are clipped to one outline.
 * @param border drawn on the same container as [shape], for the same reason.
 * @param placeholderColor what the shimmer sweeps across and the failure state rests on.
 *   Defaults to the surface a card would use; screens with their own ground — the lens
 *   pile's lacquer, the viewer's black — pass their own so the wait looks like part of
 *   the design instead of a hole punched in it.
 * @param onClick taken here rather than by the caller's modifier so the ripple lands
 *   inside [shape]. A tap target clipped to a square on a rounded tile is visible.
 * @param onSuccess for the one screen that has to measure the photograph it just loaded.
 */
@Composable
fun AppAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    clickEnabled: Boolean = true,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
) {
    // Keyed on the model: a recycled tile in a scrolling row is the same composable
    // showing a different photograph, and inheriting the previous one's "loaded" would
    // leave the new one as a blank frame with no shimmer to explain it.
    var phase by remember(model) { mutableStateOf(Phase.Loading) }
    val platformContext = LocalPlatformContext.current

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = clickEnabled,
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        AsyncImage(
            model = remember(model, platformContext) {
                ImageRequest.Builder(platformContext)
                    .data(model)
                    .httpHeaders(REMOTE_IMAGE_HEADERS)
                    .build()
            },
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            onLoading = { phase = Phase.Loading },
            onSuccess = {
                phase = Phase.Loaded
                onSuccess?.invoke(it)
            },
            onError = { phase = Phase.Failed },
            modifier = Modifier.fillMaxSize(),
        )

        // Over the image rather than under it, and faded out rather than switched off:
        // a photo read from disk can arrive in a single frame, and a placeholder that
        // vanishes on that frame is a flash. Fading covers the swap either way.
        AnimatedVisibility(
            visible = phase == Phase.Loading,
            enter = fadeIn(Motion.enter()),
            exit = fadeOut(Motion.exit()),
            modifier = Modifier.matchParentSize(),
        ) {
            ShimmerBox(
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                baseColor = placeholderColor,
            )
        }

        AnimatedVisibility(
            visible = phase == Phase.Failed,
            enter = fadeIn(Motion.enter()),
            exit = fadeOut(Motion.exit()),
            modifier = Modifier.matchParentSize(),
        ) {
            // Measured rather than fractioned in the modifier chain: these frames run
            // from a 46dp card in the lens pile to the whole display, one fixed size is
            // either a smudge on the small end or a road sign on the large, and a
            // `fillMaxSize(fraction)` cannot be capped afterwards — it hands down a fixed
            // constraint, which any later `sizeIn` is obliged to honour over its own
            // maximum. Composed only while the failure is showing, so the subcomposition
            // costs nothing on the path where the photograph loads.
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().background(placeholderColor),
                contentAlignment = Alignment.Center,
            ) {
                val side = (minOf(maxWidth, maxHeight) * FAILED_ICON_FRACTION)
                    .coerceAtMost(FAILED_ICON_MAX)
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    // The photograph's own description is still on the image beneath, so
                    // a second one here would have a screen reader announce the same
                    // tile twice — once as itself and once as its own error mark.
                    contentDescription = null,
                    tint = failedIconTint(placeholderColor),
                    modifier = Modifier.size(side),
                )
            }
        }
    }
}

/**
 * Headers every image request carries, because one host insists on them.
 *
 * Wikimedia — which serves every photograph on the Explore map — enforces its user-agent
 * policy on `upload.wikimedia.org` and answers **403** to a request with no agent *or*
 * with a generic library one. Measured against a real thumbnail: `200` with
 * [SAOLA_USER_AGENT], `403` with `okhttp/4.12.0`, which is what the image loader sends by
 * default. The symptom is every photograph on the screen rendering as the broken-image
 * mark, with nothing in the logs to say why.
 *
 * The agent string itself is in `:domain` — the two Explore clients send the same one, and
 * the contact address in it has to change in one place.
 *
 * Applied to every request rather than only to remote ones. A local file path never
 * reaches the network layer, so the header costs nothing there, and a rule with an
 * exception is a rule somebody forgets at the next call site.
 */
private val REMOTE_IMAGE_HEADERS = NetworkHeaders.Builder()
    .set("User-Agent", SAOLA_USER_AGENT)
    .build()

/** What the frame is doing, which is not always what the image is doing. */
private enum class Phase { Loading, Loaded, Failed }

/** Present enough to read as a mark, faint enough not to read as content. */
private fun failedIconTint(placeholderColor: Color): Color =
    if (isLightSurface(placeholderColor)) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }

private const val FAILED_ICON_FRACTION = 0.34f

private val FAILED_ICON_MAX = 40.dp
