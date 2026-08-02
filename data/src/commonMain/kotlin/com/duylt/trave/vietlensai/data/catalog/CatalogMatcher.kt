package com.duylt.trave.vietlensai.data.catalog

import com.duylt.trave.vietlensai.domain.model.CatalogItem
import com.duylt.trave.vietlensai.domain.model.CollectionEntry
import com.duylt.trave.vietlensai.domain.model.CollectionSection
import com.duylt.trave.vietlensai.domain.model.CultureCollection
import com.duylt.trave.vietlensai.domain.model.Discovery

/**
 * Crosses the shipped catalogue with the traveller's own photographs.
 *
 * Pure and synchronous so it can be tested against the real asset and real-looking
 * recognition output — the matching rule below is the whole feature, and a rule that
 * is only exercised through a repository is a rule nobody checks.
 *
 * ### Diacritics are kept, not folded
 *
 * The obvious implementation lowercases and strips Vietnamese tone marks so that
 * "Phở" and "pho" both match. It is wrong, and quietly so: folded, `phở` and `phố`
 * are the same string, so photographing Phố cổ Hội An would collect Phở. `đình`
 * (a communal house) collapses onto `định`, `mỹ` onto `mỳ`, `cửa` onto `của`. Gemini
 * writes Vietnamese with its diacritics, and the catalogue is written the same way,
 * so keeping them costs nothing and removes a whole class of wrong unlocks. Aliases
 * that also list an unaccented spelling ("banh mi") still work — they are simply
 * matched as the distinct strings they are.
 *
 * ### Whole words, in sequence
 *
 * A plain `contains` would let the alias `chè` match `chèo`, and `thác` match
 * `thách`. Both sides are split into words on anything that is not a letter or a
 * digit, and a multi-word alias has to appear as consecutive words — `bún bò` must
 * not be satisfied by "bún riêu và bò kho".
 */
internal fun buildCollection(
    items: List<CatalogItem>,
    discoveries: List<Discovery>,
): CultureCollection {
    // Tokenised once each rather than once per alias: the board is ~60 items with a
    // handful of aliases apiece, and re-splitting every discovery's tags for each of
    // them is the one part of this that would actually be felt on a long journal.
    val haystacks = discoveries.map { discovery -> discovery to discovery.searchTokens() }

    val entries = items.map { item ->
        val needles = item.aliases.map { tokenize(it) }.filter { it.isNotEmpty() }
        CollectionEntry(
            item = item,
            // Newest first, because that is the order the journal hands them over: a
            // tile shows the most recent photograph of the thing rather than the first,
            // which is almost always the better picture and always the one the
            // traveller remembers taking.
            discovery = haystacks.firstOrNull { (_, tokens) ->
                needles.any { needle -> tokens.containsSequence(needle) }
            }?.first,
        )
    }

    // Grouped in the order the asset lists them, so the file alone decides how the
    // board reads and reordering it needs no code change. Stable rather than
    // collected-first on purpose: a board that reshuffles every time something is
    // found stops being a place the traveller can learn their way around.
    val sections = entries
        .groupBy { it.item.category }
        .map { (category, sectionEntries) -> CollectionSection(category, sectionEntries) }

    return CultureCollection(sections)
}

/**
 * Everything about a discovery that might name what it is.
 *
 * The summary and section bodies are deliberately left out. They are paragraphs of
 * prose that mention neighbouring dishes, nearby temples and regional variants, and
 * matching against them would collect half the catalogue from one photograph of a
 * bowl of phở.
 */
private fun Discovery.searchTokens(): List<String> =
    tokenize(buildString {
        append(title)
        localName?.let { append(' ').append(it) }
        tags.forEach { append(' ').append(it) }
    })

/** Lowercased words, split on anything that is not a letter or a digit. */
internal fun tokenize(text: String): List<String> {
    val tokens = mutableListOf<String>()
    val word = StringBuilder()
    for (char in text.lowercase()) {
        if (char.isLetter() || char.isDigit()) {
            word.append(char)
        } else if (word.isNotEmpty()) {
            tokens += word.toString()
            word.clear()
        }
    }
    if (word.isNotEmpty()) tokens += word.toString()
    return tokens
}

/** Whether [needle] appears as consecutive words in this list. */
private fun List<String>.containsSequence(needle: List<String>): Boolean {
    if (needle.isEmpty() || needle.size > size) return false
    outer@ for (start in 0..size - needle.size) {
        for (offset in needle.indices) {
            if (this[start + offset] != needle[offset]) continue@outer
        }
        return true
    }
    return false
}
