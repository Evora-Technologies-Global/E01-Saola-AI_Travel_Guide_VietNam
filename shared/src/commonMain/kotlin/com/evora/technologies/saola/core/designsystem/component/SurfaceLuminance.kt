package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Whether something drawn on [color] should be treated as sitting on a light surface.
 *
 * Alpha counts as well as luminance: a nearly transparent white laid over the black of
 * the photo viewer is a dark surface in every way that matters to the eye, and reading
 * only its luminance would call it light and put black ink on black.
 *
 * A file of its own rather than a private helper inside either caller: both [ShimmerBox]
 * and [AppAsyncImage] pick a highlight against a colour they were handed, and a copy in
 * each is a pair of answers that agree until one of them is tuned.
 */
internal fun isLightSurface(color: Color): Boolean =
    color.luminance() > 0.5f && color.alpha > 0.5f
