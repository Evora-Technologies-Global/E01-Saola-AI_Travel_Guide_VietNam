package com.evora.technologies.saola.data.local.db.entity

import kotlinx.serialization.Serializable

/**
 * The on-disk shapes for JSON columns.
 *
 * Separate from both the domain models and the Gemini DTOs on purpose: the
 * persisted format has to stay readable by future versions, so it must not move
 * every time a prompt or a UI model is reshaped.
 */
@Serializable
data class SectionJson(
    val title: String = "",
    val body: String = "",
)

@Serializable
data class NearbyJson(
    val name: String = "",
    val reason: String = "",
    val category: String = "OTHER",
    val walkingMinutes: Int? = null,
)

@Serializable
data class TranslationBlockJson(
    val original: String = "",
    val translated: String = "",
    val note: String? = null,
    val price: String? = null,
    /**
     * Where the line sat in the photo, as fractions of its size.
     *
     * Nullable with a default so rows written before the overlay existed still
     * decode — the column format has to outlive the feature that added to it.
     */
    val box: TextBoxJson? = null,
)

@Serializable
data class TextBoxJson(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)
