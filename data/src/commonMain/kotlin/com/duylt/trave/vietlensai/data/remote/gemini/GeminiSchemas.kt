package com.duylt.trave.vietlensai.data.remote.gemini

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The response schemas handed to Gemini alongside every prompt.
 *
 * Constraining the model server-side is what makes this app's data layer boring:
 * there is no regex hunting for a JSON block, no retry-on-prose, and a field that
 * is declared here is guaranteed to be present in the reply. Descriptions are part
 * of the contract too — they steer content quality far more reliably than adding
 * another paragraph to the prompt.
 */
internal object GeminiSchemas {

    private const val TYPE = "type"
    private const val OBJECT = "OBJECT"
    private const val ARRAY = "ARRAY"
    private const val STRING = "STRING"
    private const val NUMBER = "NUMBER"
    private const val INTEGER = "INTEGER"
    private const val BOOLEAN = "BOOLEAN"

    private val categoryValues = listOf(
        "LANDMARK", "FOOD", "ARTIFACT", "ARCHITECTURE", "NATURE", "CULTURE", "OTHER",
    )

    /** Recognition of a landmark, dish, artifact or building. */
    val discovery: JsonObject = buildJsonObject {
        put(TYPE, OBJECT)
        putJsonObject("properties") {
            putJsonObject("recognized") {
                put(TYPE, BOOLEAN)
                put("description", "False only when the photo is too unclear to identify anything.")
            }
            putJsonObject("title") {
                put(TYPE, STRING)
                put("description", "Common name in the requested output language.")
            }
            putJsonObject("localName") {
                put(TYPE, STRING)
                put("description", "Vietnamese name with full diacritics, e.g. 'Văn Miếu - Quốc Tử Giám'.")
            }
            putJsonObject("category") {
                put(TYPE, STRING)
                putJsonArray("enum") { categoryValues.forEach { add(it) } }
            }
            putJsonObject("confidence") {
                put(TYPE, NUMBER)
                put("description", "0.0 to 1.0. Be honest: use below 0.5 when guessing.")
            }
            putJsonObject("summary") {
                put(TYPE, STRING)
                put("description", "Two or three warm sentences a guide would open with.")
            }
            putJsonObject("sections") {
                put(TYPE, ARRAY)
                put("description", "3 to 5 sections chosen to suit the subject.")
                putJsonObject("items") {
                    put(TYPE, OBJECT)
                    putJsonObject("properties") {
                        putJsonObject("title") { put(TYPE, STRING) }
                        putJsonObject("body") {
                            put(TYPE, STRING)
                            put("description", "60-120 words of vivid, concrete detail. No markdown.")
                        }
                    }
                    putJsonArray("required") { add("title"); add("body") }
                    putJsonArray("propertyOrdering") { add("title"); add("body") }
                }
            }
            putJsonObject("funFacts") {
                put(TYPE, ARRAY)
                put("description", "2 to 4 surprising, verifiable details.")
                putJsonObject("items") { put(TYPE, STRING) }
            }
            putJsonObject("tags") {
                put(TYPE, ARRAY)
                put("description", "3 to 6 short keywords.")
                putJsonObject("items") { put(TYPE, STRING) }
            }
            // `nearbySuggestions` and `suggestedQuestions` used to be asked for here.
            // They were dropped from the schema rather than from the app: both are
            // pure generation cost on the one request the traveller waits through
            // with the viewfinder scrimmed over, and neither is on screen until they
            // have already got their answer. Everything downstream — the entity
            // column, the domain field, `NearbyBlock`, the chat's opening chips —
            // is still in place and simply sees an empty list, so putting them back
            // (here, or in a second request made after the screen opens) is a change
            // to this file alone.
            putJsonObject("placeHint") {
                put(TYPE, STRING)
                put("description", "City or district, e.g. 'Hoàn Kiếm, Hà Nội'.")
            }
            putJsonObject("notRecognizedHint") {
                put(TYPE, STRING)
                put("description", "Only when recognized is false: what to photograph instead.")
            }
        }
        putJsonArray("required") {
            add("recognized"); add("title"); add("category"); add("confidence"); add("summary")
        }
        putJsonArray("propertyOrdering") {
            add("recognized"); add("title"); add("localName"); add("category"); add("confidence")
            add("summary"); add("sections"); add("funFacts"); add("tags")
            add("placeHint"); add("notRecognizedHint")
        }
    }

    /**
     * Translation of lines the on-device recogniser already located.
     *
     * The index is required and carries the whole contract: it is what pins each
     * translation back to the pixels its line came from.
     */
    val lineTranslation: JsonObject = buildJsonObject {
        put(TYPE, OBJECT)
        putJsonObject("properties") {
            putJsonObject("detectedLanguage") {
                put(TYPE, STRING)
                put("description", "Language name of the source text, e.g. 'Vietnamese'.")
            }
            putJsonObject("contextNote") {
                put(TYPE, STRING)
                put("description", "What this text is (restaurant menu, bus sign, museum label) and anything a visitor should know.")
            }
            putJsonObject("lines") {
                put(TYPE, ARRAY)
                put("description", "Exactly one entry per input line, same indices, same order.")
                putJsonObject("items") {
                    put(TYPE, OBJECT)
                    putJsonObject("properties") {
                        putJsonObject("index") {
                            put(TYPE, INTEGER)
                            put("description", "The index this line was given in the prompt.")
                        }
                        putJsonObject("original") {
                            put(TYPE, STRING)
                            put("description", "The source line, with obvious recognition errors repaired.")
                        }
                        putJsonObject("translated") { put(TYPE, STRING) }
                        putJsonObject("note") {
                            put(TYPE, STRING)
                            put("description", "Short cultural gloss when the literal translation would mislead.")
                        }
                        putJsonObject("price") {
                            put(TYPE, STRING)
                            put("description", "Price exactly as printed, e.g. '45.000đ'. Omit when absent.")
                        }
                    }
                    putJsonArray("required") { add("index"); add("original"); add("translated") }
                    putJsonArray("propertyOrdering") {
                        add("index"); add("original"); add("translated"); add("note"); add("price")
                    }
                }
            }
        }
        putJsonArray("required") { add("detectedLanguage"); add("lines") }
        putJsonArray("propertyOrdering") { add("detectedLanguage"); add("contextNote"); add("lines") }
    }

    /** The end-of-day journal write-up. */
    val tripSummary: JsonObject = buildJsonObject {
        put(TYPE, OBJECT)
        putJsonObject("properties") {
            putJsonObject("headline") {
                put(TYPE, STRING)
                put("description", "A short evocative title for the day, under 8 words.")
            }
            putJsonObject("narrative") {
                put(TYPE, STRING)
                put("description", "120-180 words in second person, as if writing the traveller's diary for them.")
            }
            putJsonObject("highlights") {
                put(TYPE, ARRAY)
                putJsonObject("items") { put(TYPE, STRING) }
            }
            putJsonObject("tomorrowIdeas") {
                put(TYPE, ARRAY)
                put("description", "2 to 3 concrete suggestions that build on today.")
                putJsonObject("items") { put(TYPE, STRING) }
            }
        }
        putJsonArray("required") { add("headline"); add("narrative") }
        putJsonArray("propertyOrdering") {
            add("headline"); add("narrative"); add("highlights"); add("tomorrowIdeas")
        }
    }
}
