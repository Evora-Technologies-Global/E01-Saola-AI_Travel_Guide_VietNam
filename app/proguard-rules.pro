# =============================================================================
#  VietLens AI — R8 keep rules
#
#  This file is almost empty on purpose, and that is a finding rather than an
#  oversight. A full minified `:app:assembleRelease` was run with this file
#  empty: it succeeded, and R8 emitted no missing_rules.txt at all — meaning
#  zero missing classes and zero `-dontwarn` needed at app level. Every
#  dependency in this project already ships its own consumer rules, which land
#  in app/build/outputs/mapping/release/configuration.txt and do the work.
#
#  Re-checked after the Koin migration and after ML Kit, Maps and Coil went in:
#  configuration.txt lists consumer rules from all of them (camera-*, koin-android,
#  koin-compose-viewmodel, text-recognition-*, play-services-maps, coil-core,
#  room-runtime, navigation-*, material3, the Compose artifacts), the build still
#  produces no missing_rules.txt, and the release APK installs and runs.
#
#  So this file is the only place app-level keep rules are read from. `:data` has
#  a consumer-rules.pro, but the multiplatform-library plugin does not publish
#  consumer rules unless asked and nothing asks — that file explains itself.
#
#  Notes on the reflective paths people usually add rules for, recorded here so
#  nobody "helpfully" pastes them back in later:
#
#  * kotlinx.serialization — nothing needed. The serialization compiler plugin
#    generates a $$serializer for every @Serializable class, so there is no
#    name-based lookup to protect. `deserializer = serializer()` in
#    GeminiRemoteDataSource and `decodeFromString<GeminiErrorEnvelope>` in
#    GeminiClient are reified and rewritten at compile time to direct
#    `Xxx.Companion.serializer()` calls. The one genuinely runtime-reflective
#    path is `response.body()` in GeminiClient, where Ktor's ContentNegotiation
#    resolves a serializer from the KType via getDeclaredField("Companion") —
#    which is exactly what kotlinx-serialization-core's own shipped rules keep.
#    Verified in mapping.txt: `Companion -> Companion` and
#    `serializer() -> serializer` survive while the DTOs themselves are freely
#    renamed. Adding `-keep class ...dto.** { *; }` would only inflate the APK.
#
#  * Room — nothing needed, and less than the usual advice suggests. In its
#    multiplatform mode Room does not load the implementation by name at all:
#    VietLensDatabase is `@ConstructedBy(VietLensDatabaseConstructor::class)`,
#    and KSP generates an `actual object` whose `initialize()` returns
#    `VietLensDatabase_Impl()` as a direct call, so R8 can rename the whole chain.
#    room-runtime's own `-keep class * extends androidx.room.RoomDatabase`
#    applies regardless. Entities and DAOs are only ever touched by generated code.
#
#  * Koin — nothing needed; this is where the Hilt note used to be, before DI
#    moved to Koin for the multiplatform build. Koin keys its definitions on
#    `KClass`, not on a class-name string: `single<HttpClient>` and
#    `get<VietLensDatabase>()` build the same qualifier from the same (renamed)
#    class, so definition and lookup move together under obfuscation. The
#    ViewModel factory path has consumer rules of its own in koin-android and
#    koin-compose-viewmodel-android. MainActivity and VietLensApplication are
#    kept by AGP's manifest-derived aapt_rules.txt.
#
#  * Ktor — the engine is bound explicitly via `HttpClient(OkHttp)`, so the
#    ServiceLoader path is never taken; ktor-utils ships that keep regardless.
#
#  * Enum names — SettingsDataStore persists `ThemePreference.name`. Safe: R8
#    renames the static fields but leaves the constant name strings in <clinit>.
#    Every other enum round-trips through an explicit `code`/`id`/`wireName`.
#
#  * java.time — minSdk 26, no desugaring, nothing to keep.
# =============================================================================


# -----------------------------------------------------------------------------
# Readable release crash traces.
#
# The only rules this app actually needs. AGP's bundled default file keeps
# Signature, InnerClasses, EnclosingMethod and the RuntimeVisible* annotations,
# but NOT SourceFile/LineNumberTable. Without these two lines R8 discards the
# real line table and stamps every class with an "r8-map-id-<hash>" marker, so a
# crash during a live demo reads `at tc1.a(r8-map-id-0331...:3)` and can only be
# decoded by running retrace against mapping.txt on another machine.
#
# With them the same frame reads `at tc1.a(GeminiClient.kt:172)` — true file and
# line, while classes and methods stay obfuscated. It is not even a size
# trade-off: the sparse real line table measured smaller than one synthetic
# entry per program counter.
#
# -renamesourcefileattribute replaces the original .kt names with the literal
# "SourceFile", so the attribute does not re-emit the project's source layout.
# -----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
