package com.duylt.trave.vietlensai.domain.model

/**
 * One thing worth collecting — a dish, a roof detail, a kind of boat.
 *
 * The catalogue is written by hand and shipped as an asset rather than generated,
 * for the same reason the province outlines are: it has to be identical on every
 * device, readable with no signal, and above all *possible*. A model asked to
 * invent things to look for will cheerfully suggest a Lý-dynasty stone dragon to
 * someone standing in Cà Mau.
 *
 * @param hint how to recognise it, which is the part that makes this a guide rather
 *   than a checklist. "Sợi bánh dẹt, nước dùng trong" teaches the traveller to look;
 *   "phở" only tells them a word they already knew.
 * @param aliases what the model might call it. Matched as whole words against a
 *   discovery's title, local name and tags — see the matcher for why diacritics are
 *   kept rather than folded.
 */
data class CatalogItem(
    val id: String,
    val category: DiscoveryCategory,
    val name: String,
    val nameEn: String,
    val hint: String,
    val hintEn: String,
    val aliases: List<String>,
) {
    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.VIETNAMESE -> name
        AppLanguage.ENGLISH -> nameEn.ifBlank { name }
    }

    fun displayHint(language: AppLanguage): String = when (language) {
        AppLanguage.VIETNAMESE -> hint
        AppLanguage.ENGLISH -> hintEn.ifBlank { hint }
    }
}

/**
 * A catalogue entry as the traveller sees it: the thing, and their own photograph of
 * it if they have one.
 *
 * [discovery] being the whole record rather than an id is what lets a collected tile
 * show the picture they took — the point of the feature is that the collection fills
 * with their trip, not with stock photography the app shipped.
 */
data class CollectionEntry(
    val item: CatalogItem,
    val discovery: Discovery?,
) {
    val isCollected: Boolean get() = discovery != null
}

/** One category's worth of entries, in the order the catalogue lists them. */
data class CollectionSection(
    val category: DiscoveryCategory,
    val entries: List<CollectionEntry>,
) {
    val collectedCount: Int get() = entries.count { it.isCollected }
    val total: Int get() = entries.size
}

/**
 * The whole board.
 *
 * Derived at read time from the catalogue and the journal rather than stored: there is
 * no state here that the discoveries do not already contain, and a table of "unlocked"
 * rows would be a second source of truth that could disagree with the photographs. It
 * also means the collection is retroactive — someone who has been using the app for a
 * week opens this screen and finds it already part-filled, rather than being told to
 * start over.
 */
data class CultureCollection(
    val sections: List<CollectionSection>,
) {
    val total: Int get() = sections.sumOf { it.total }
    val collectedCount: Int get() = sections.sumOf { it.collectedCount }

    /** 0f..1f, and 0f rather than a division by zero when the asset failed to load. */
    val progress: Float get() = if (total == 0) 0f else collectedCount.toFloat() / total

    companion object {
        val EMPTY = CultureCollection(emptyList())
    }
}
