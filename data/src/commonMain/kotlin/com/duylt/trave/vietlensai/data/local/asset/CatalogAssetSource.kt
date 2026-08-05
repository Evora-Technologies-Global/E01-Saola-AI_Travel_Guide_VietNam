package com.duylt.trave.vietlensai.data.local.asset

import com.duylt.trave.vietlensai.data.platform.deviceLanguage
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.CatalogItem
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The collection catalogue, read once from `assets/catalog.json`.
 *
 * Shipped rather than generated, and shipped rather than fetched: the board has to be
 * the same on every device so two travellers can compare it, it has to open on a
 * mountain with no signal, and every entry has to be something a person could actually
 * walk up to and photograph. A model asked to invent things to collect satisfies none
 * of the three.
 *
 * Follows [ProvinceAssetSource] exactly — same lazy parse behind a mutex, same refusal
 * to throw. A catalogue that will not parse costs this one screen; it must not take the
 * camera down with it.
 */
internal class CatalogAssetSource(
    private val assets: BundledAssets,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val mutex = Mutex()

    private var cached: List<CatalogItem>? = null

    /** Parsed on first use and held for the process lifetime; the file never changes. */
    suspend fun items(): List<CatalogItem> {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: load().also { cached = it }
        }
    }

    private suspend fun load(): List<CatalogItem> {
        val text = assets.readText(ASSET_NAME) ?: return emptyList()
        return withContext(ioDispatcher) {
            try {
                parseCatalog(text)
            } catch (e: Exception) {
                log.e(e) { "Could not parse $ASSET_NAME" }
                emptyList()
            }
        }
    }

    private companion object {
        const val ASSET_NAME = "catalog.json"
    }
}

/**
 * Parses the asset's contents. Split from the IO above so a test can run the real
 * shipped file through the real parser without a platform asset manager.
 *
 * Entries with no aliases are dropped rather than kept: one could never be collected,
 * so it would sit on the board permanently locked, and the traveller would have no way
 * to tell that from something they simply have not found yet.
 */
internal fun parseCatalog(
    text: String,
    language: AppLanguage = deviceLanguage(),
): List<CatalogItem> =
    catalogJson.decodeFromString(CatalogFileDto.serializer(), text)
        .items
        .map { it.toDomain(language) }
        .filter { it.aliases.isNotEmpty() && it.id.isNotBlank() }

private val catalogJson = Json { ignoreUnknownKeys = true }

@Serializable
private class CatalogFileDto(
    val version: Int = 1,
    val items: List<CatalogItemDto> = emptyList(),
)

/**
 * @param name and [hint] are the Vietnamese originals; [names] and [hints] are keyed by
 *   language code for the other seven. Vietnamese stays out of the maps because it is
 *   also what the aliases are written in — one entry, not two that could disagree.
 */
@Serializable
private class CatalogItemDto(
    val id: String = "",
    val category: String = "",
    val name: String = "",
    val hint: String = "",
    val names: Map<String, String> = emptyMap(),
    val hints: Map<String, String> = emptyMap(),
    val aliases: List<String> = emptyList(),
)

private fun CatalogItemDto.toDomain(language: AppLanguage): CatalogItem = CatalogItem(
    id = id,
    category = DiscoveryCategory.fromWire(category),
    name = names.pick(language, fallback = name),
    hint = hints.pick(language, fallback = hint),
    aliases = aliases.filter { it.isNotBlank() },
)

/**
 * The language asked for, then English, then the Vietnamese original.
 *
 * Vietnamese short-circuits to [fallback] because it is not *in* the map — it is the
 * `name`/`hint` field itself. Without that first line the map lookup missed, English won
 * the second, and a phone set to Vietnamese read its own catalogue in English. Caught by
 * `CatalogMatcherTest`, which parses the shipped asset once per language and requires the
 * eight hints to come out different.
 *
 * The remaining two fallbacks answer different failures: a translation missing for Thai
 * should still read as English rather than as Vietnamese, and an entry added to the asset
 * with no translations at all should still show *something*.
 */
private fun Map<String, String>.pick(language: AppLanguage, fallback: String): String {
    if (language == AppLanguage.VIETNAMESE) return fallback
    return this[language.code]?.takeIf { it.isNotBlank() }
        ?: this[AppLanguage.ENGLISH.code]?.takeIf { it.isNotBlank() }
        ?: fallback
}
