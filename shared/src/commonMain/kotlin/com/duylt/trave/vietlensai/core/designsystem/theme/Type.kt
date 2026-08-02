package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reading-first typography.
 *
 * The app's core content is 60-120 word passages of history read on a phone held
 * at arm's length, often one-handed while walking. Body sizes are therefore a step
 * larger than the Material default and line height is generous — and
 * [LineHeightStyle.Trim.None] keeps Vietnamese stacked diacritics (ề, ộ, ữ) from
 * being clipped, which the default trimming does at tight line heights.
 */
private val readableLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private val defaultTypography = Typography()

val VietLensTypography = Typography(
    displaySmall = defaultTypography.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = readableLineHeight,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.2.sp,
        lineHeightStyle = readableLineHeight,
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
)

val VietLensShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)
