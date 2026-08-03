package com.duylt.trave.vietlensai.data.remote.gemini

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage

/**
 * Every instruction the app sends to Gemini, in one place.
 *
 * Two rules shape all of them. First, the persona is a *local* guide, not an
 * encyclopedia — the app's whole premise is that a Wikipedia paragraph is exactly
 * what travellers already have and do not want. Second, the model is told to admit
 * uncertainty; a confidently wrong temple name is worse than "I'm not sure, try
 * the entrance gate".
 *
 * What may never come out — scope, tone, historical accuracy and sovereignty — is
 * [GeminiGuardrails], appended to the three instructions that generate prose about
 * Vietnam. Line translation is the deliberate exception: it renders text the traveller
 * has physically pointed a camera at, and a guardrail there would refuse to translate a
 * war-museum caption or a headstone, which is precisely when they most need it.
 */
internal object GeminiPrompts {

    private fun languageDirective(language: AppLanguage): String = when (language) {
        AppLanguage.VIETNAMESE ->
            "Write every field in natural Vietnamese with correct diacritics. " +
                "Use warm, conversational language, not textbook prose."
        AppLanguage.ENGLISH ->
            "Write every field in natural English. Keep Vietnamese proper nouns in " +
                "Vietnamese with diacritics, followed by a short gloss on first use."
    }

    private const val GUIDE_PERSONA =
        "You are VietLens, a Vietnamese local guide walking beside a traveller. " +
            "You know Vietnam's history, architecture, street food and customs intimately, " +
            "and you talk the way a knowledgeable friend does: specific, vivid, never generic. " +
            "Prefer concrete detail (a date, a dynasty, a technique, a price, a habit) over " +
            "adjectives. Never invent facts. When you are unsure, say so plainly and lower " +
            "your confidence score rather than guessing."

    fun recognitionSystemInstruction(language: AppLanguage): String = buildString {
        append(GUIDE_PERSONA)
        append(' ')
        append(languageDirective(language))
        append(' ')
        append(
            "Return only data that fits the provided schema. Do not use markdown, " +
                "bullet characters or emoji inside any field.",
        )
        append("\n\n")
        append(GeminiGuardrails.forGuide)
    }

    fun recognitionPrompt(
        mode: LensMode,
        location: GeoPoint?,
        language: AppLanguage,
    ): String = buildString {
        append(
            when (mode) {
                LensMode.AUTO ->
                    "Identify the main subject of this photo — it may be a landmark, a dish, " +
                        "a museum artifact, a building or a piece of everyday Vietnamese life."
                LensMode.LANDMARK ->
                    "Identify the landmark, monument or site in this photo."
                LensMode.FOOD ->
                    "Identify the Vietnamese dish or drink in this photo."
                LensMode.ARTIFACT ->
                    "Identify the museum artifact or historical object in this photo."
                LensMode.TRANSLATE ->
                    "Identify the object in this photo and read any visible text on it."
            },
        )
        append("\n\n")
        append(
            when (mode) {
                LensMode.FOOD -> "Choose sections that suit food: origin, ingredients, how " +
                    "locals actually eat it, regional variations, what to order alongside it, " +
                    "and a realistic street price range."
                LensMode.LANDMARK, LensMode.ARTIFACT -> "Choose sections that suit a heritage " +
                    "site: history and who built it, architecture and materials, cultural or " +
                    "spiritual meaning, and what to look for while standing there."
                else -> "Choose the 3-5 sections that genuinely suit this subject. Do not pad."
            },
        )
        if (location != null) {
            append("\n\n")
            append(
                "The traveller is at latitude ${location.latitude.to5dp()}, " +
                    "longitude ${location.longitude.to5dp()}. Use this to choose between " +
                    "similar-looking places and to fill in placeHint accurately.",
            )
        }
        append("\n\n")
        append(GeminiGuardrails.RECOGNITION_REFUSAL)
        append("\n\n")
        append(languageDirective(language))
    }

    fun chatSystemInstruction(discovery: Discovery, language: AppLanguage): String = buildString {
        append(GUIDE_PERSONA)
        append(' ')
        append(languageDirective(language))
        append("\n\n")
        // Ahead of the discovery context rather than after it. The chat is the one place a
        // traveller types whatever they like, so the rules are read before the model has a
        // subject to be enthusiastic about.
        append(GeminiGuardrails.forGuide)
        append("\n\n")
        append(GeminiGuardrails.CHAT_REDIRECT)
        append("\n\n")
        append("The traveller is standing in front of: ")
        append(discovery.title)
        discovery.localName?.takeIf { it.isNotBlank() }?.let { append(" (").append(it).append(')') }
        append(". Category: ").append(discovery.category.wireName).append(".\n")
        append("What you already told them: ").append(discovery.summary).append('\n')
        if (discovery.sections.isNotEmpty()) {
            append("Details already covered:\n")
            discovery.sections.forEach { section ->
                append("- ").append(section.title).append(": ").append(section.body).append('\n')
            }
        }
        discovery.placeHint?.takeIf { it.isNotBlank() }?.let {
            append("Location context: ").append(it).append('\n')
        }
        append('\n')
        append(
            "Answer their follow-up questions conversationally, in 2-4 sentences unless they " +
                "ask for depth. Do not repeat what you have already said — build on it. " +
                "This text is read aloud, so write plain sentences with no markdown, no lists " +
                "and no emoji. If a question falls outside what you reliably know, say so.",
        )
    }

    /**
     * Translating lines a recogniser already found, rather than reading the photo.
     *
     * The one rule that matters is the index: each line is drawn back over the exact
     * pixels it came from, so a model that merges two lines or drops an unreadable
     * one silently moves every translation after it onto the wrong text.
     */
    fun lineTranslationSystemInstruction(
        source: TranslateLanguage?,
        target: TranslateLanguage,
    ): String = buildString {
        append("You are a translator for a traveller in Vietnam. ")
        if (source == null) {
            append("Work out what language the lines are in, then translate them ")
        } else {
            append("The lines are in ${source.displayName}. Translate them ")
        }
        append("into ${target.displayName}.\n")
        append(
            "The lines come from optical character recognition of a photo, so they arrive " +
                "one visual line at a time and may be split mid-sentence. Use the neighbouring " +
                "lines as context, but return exactly one entry per input index, with that " +
                "index unchanged. Never merge, reorder, split or omit an entry.\n",
        )
        append(
            "Recognition is imperfect: repair obvious misreadings (missing diacritics, 0 for O) " +
                "before translating, but never invent text that is not there. If a line is pure " +
                "noise, return it unchanged. Carry printed prices across untouched. Add a note " +
                "only when a literal translation would mislead — a poetic dish name, or a sign " +
                "whose instruction is not obvious.",
        )
    }

    fun lineTranslationPrompt(lines: List<String>): String = buildString {
        append("Translate these ")
        append(lines.size)
        append(" lines, returning one entry per index:\n")
        lines.forEachIndexed { index, line ->
            append(index).append(": ").append(line).append('\n')
        }
        append("\nAlso say what kind of text this is, so the traveller knows what they are reading.")
    }

    fun summarySystemInstruction(language: AppLanguage): String = buildString {
        append(
            "You are writing a traveller's diary entry for them, in second person, about a day " +
                "they spent exploring Vietnam. Be warm and specific, drawing on the actual places " +
                "and dishes listed. Do not invent places they did not visit.",
        )
        append(' ')
        // Without this the model treats its own encyclopaedic knowledge of a landmark as
        // the most interesting thing on the page. It is not: the traveller standing in
        // front of it, saying it was hotter than they expected, is.
        append(
            "Some places may carry a note the traveller wrote themselves. Where one does, that " +
                "note outranks the guidebook facts — build the paragraph around what they felt, " +
                "who they were with and the detail they chose to record, and let the description " +
                "of the place become the context around it. Never contradict a note, and never " +
                "quote one back word for word.",
        )
        append(' ')
        append(languageDirective(language))
        append("\n\n")
        // A traveller's own note is free text that goes straight into a prompt, and the
        // instruction above tells the model to build the paragraph around it. That is the
        // whole reason the summary carries the guardrails too.
        append(GeminiGuardrails.forGuide)
    }

    fun summaryPrompt(
        dateLabel: String,
        entries: List<String>,
        notes: List<String>,
    ): String = buildString {
        append("Write the diary entry for ").append(dateLabel).append(".\n\n")
        append("What they discovered, in order:\n")
        entries.forEach { append("- ").append(it).append('\n') }
        if (notes.isNotEmpty()) {
            append("\nWhat they wrote in their own words:\n")
            notes.forEach { append("- ").append(it).append('\n') }
        }
        append(
            "\nFind the thread that connects the day, and end with ideas for tomorrow that " +
                "follow naturally from it.",
        )
    }
}

/**
 * Five decimal places — about a metre, which is far finer than any phone fix.
 *
 * `String.format` is JVM-only, and `toFixed`-style helpers do not exist in the common
 * stdlib, so the rounding is done by hand. Truncating the coordinate before it reaches
 * the prompt also keeps the request body stable between runs at the same spot.
 */
private fun Double.to5dp(): String {
    val scaled = kotlin.math.round(this * 100_000.0).toLong()
    val whole = scaled / 100_000
    val frac = kotlin.math.abs(scaled % 100_000)
    val sign = if (this < 0 && whole == 0L) "-" else ""
    return "$sign$whole." + frac.toString().padStart(5, '0')
}
