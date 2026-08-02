package com.duylt.trave.vietlensai.data.local.asset

/**
 * Reads a file that ships inside the app.
 *
 * Both platforms package the same bytes from `data/src/androidMain/assets`: Android
 * because that is where the asset packager looks, iOS because the `copySharedAssetsToIos`
 * Gradle task syncs the folder into the Xcode project before every framework link. One
 * copy in version control, so the 34 province outlines cannot drift apart.
 *
 * Returns null rather than throwing when the file is missing — a corrupt or absent asset
 * must leave the map empty, not take the whole app down.
 */
internal fun interface BundledAssets {
    suspend fun readText(name: String): String?
}
