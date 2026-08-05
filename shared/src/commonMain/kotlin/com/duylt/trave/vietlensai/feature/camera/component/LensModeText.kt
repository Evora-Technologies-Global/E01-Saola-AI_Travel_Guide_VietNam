package com.duylt.trave.vietlensai.feature.camera.component

import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.camera_hint_artifact
import com.duylt.trave.vietlensai.resources.camera_hint_auto
import com.duylt.trave.vietlensai.resources.camera_hint_food
import com.duylt.trave.vietlensai.resources.camera_hint_landmark
import com.duylt.trave.vietlensai.resources.camera_hint_translate
import com.duylt.trave.vietlensai.resources.mode_artifact
import com.duylt.trave.vietlensai.resources.mode_auto
import com.duylt.trave.vietlensai.resources.mode_food
import com.duylt.trave.vietlensai.resources.mode_landmark
import com.duylt.trave.vietlensai.resources.mode_translate
import org.jetbrains.compose.resources.StringResource

/**
 * The two things a lens mode says about itself: its name, and what to point at.
 *
 * Both branches draw both, but in different places — the phone puts the name on a chip and
 * the hint in a bubble over the frame, while the tablet's panel prints them one above the
 * other as a heading. Two `when` blocks over the same enum kept in two files is how one of
 * them ends up missing a mode after the next one is added, so they live together here rather
 * than beside either drawing.
 */
internal fun LensMode.labelRes(): StringResource = when (this) {
    LensMode.AUTO -> Res.string.mode_auto
    LensMode.LANDMARK -> Res.string.mode_landmark
    LensMode.FOOD -> Res.string.mode_food
    LensMode.ARTIFACT -> Res.string.mode_artifact
    LensMode.TRANSLATE -> Res.string.mode_translate
}

internal fun LensMode.hintRes(): StringResource = when (this) {
    LensMode.AUTO -> Res.string.camera_hint_auto
    LensMode.LANDMARK -> Res.string.camera_hint_landmark
    LensMode.FOOD -> Res.string.camera_hint_food
    LensMode.ARTIFACT -> Res.string.camera_hint_artifact
    LensMode.TRANSLATE -> Res.string.camera_hint_translate
}
