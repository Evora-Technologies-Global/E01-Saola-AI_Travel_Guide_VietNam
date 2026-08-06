package com.evora.technologies.saola.data.local.db

import com.evora.technologies.saola.data.local.db.entity.NearbyJson
import com.evora.technologies.saola.data.local.db.entity.SectionJson
import com.evora.technologies.saola.data.local.db.entity.TranslationBlockJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * JSON codecs for the list-shaped columns.
 *
 * Every entity column is a primitive or a String, so Room needs no `@TypeConverter`
 * registration — mappers call these directly on the way in and out. Decoding is
 * deliberately lenient: a schema tweak in a future version should degrade an old
 * row to an empty list, never crash the journal on launch.
 */
internal object Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stringListSerializer = ListSerializer(String.serializer())
    private val sectionListSerializer = ListSerializer(SectionJson.serializer())
    private val nearbyListSerializer = ListSerializer(NearbyJson.serializer())
    private val blockListSerializer = ListSerializer(TranslationBlockJson.serializer())

    fun encodeStrings(value: List<String>): String =
        json.encodeToString(stringListSerializer, value)

    fun decodeStrings(value: String): List<String> =
        runCatching { json.decodeFromString(stringListSerializer, value) }.getOrDefault(emptyList())

    fun encodeSections(value: List<SectionJson>): String =
        json.encodeToString(sectionListSerializer, value)

    fun decodeSections(value: String): List<SectionJson> =
        runCatching { json.decodeFromString(sectionListSerializer, value) }.getOrDefault(emptyList())

    fun encodeNearby(value: List<NearbyJson>): String =
        json.encodeToString(nearbyListSerializer, value)

    fun decodeNearby(value: String): List<NearbyJson> =
        runCatching { json.decodeFromString(nearbyListSerializer, value) }.getOrDefault(emptyList())

    fun encodeBlocks(value: List<TranslationBlockJson>): String =
        json.encodeToString(blockListSerializer, value)

    fun decodeBlocks(value: String): List<TranslationBlockJson> =
        runCatching { json.decodeFromString(blockListSerializer, value) }.getOrDefault(emptyList())
}
