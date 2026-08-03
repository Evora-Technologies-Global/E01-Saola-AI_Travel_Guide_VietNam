import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/*
 * A thin Android host.
 *
 * Every screen, ViewModel and design-system token lives in `:shared` and is written once
 * against Compose Multiplatform. What is left here is the part iOS has its own version of:
 * an `Application` that starts Koin, and an `Activity` that owns the window — edge-to-edge,
 * the splash screen, immersive system bars and the volume-key shutter.
 */

/**
 * Signing material, read from the git-ignored `local.properties`.
 *
 * Read through the provider API rather than a raw `FileInputStream` so the
 * configuration cache stays valid and invalidates when the file changes.
 */
val localProperties: Map<String, String> = providers
    .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText
    .map { text ->
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
    }
    .orElse(emptyMap())
    .get()

/**
 * A release build is only signed when the keystore and its credentials are
 * actually present.
 *
 * The alternative — always declaring the signing config — turns a fresh clone
 * without the keystore into a confusing `validateSigningRelease` failure. This
 * way a teammate still gets a working (unsigned) release APK and a clear reason.
 */
val releaseKeystore: File? = localProperties["RELEASE_STORE_FILE"]
    ?.let { layout.projectDirectory.file(it).asFile }
    ?.takeIf { it.exists() }

val canSignRelease: Boolean = releaseKeystore != null &&
    !localProperties["RELEASE_STORE_PASSWORD"].isNullOrBlank() &&
    !localProperties["RELEASE_KEY_ALIAS"].isNullOrBlank() &&
    !localProperties["RELEASE_KEY_PASSWORD"].isNullOrBlank()

/**
 * The app version, declared once in the root `gradle.properties` and read by `:shared` too.
 *
 * Through the provider API, so an override on the command line invalidates the configuration
 * cache the way editing the file does.
 */
val vietlensVersionName: String = providers.gradleProperty("vietlens.versionName").get()
val vietlensVersionCode: Int = providers.gradleProperty("vietlens.versionCode").get().toInt()

android {
    namespace = "com.duylt.trave.vietlensai"
    compileSdk {
        version = release(37)
    }

    // Must be declared before buildTypes: the Kotlin DSL runs top-down, so
    // signingConfigs.getByName("release") below would otherwise not resolve.
    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = localProperties["RELEASE_STORE_PASSWORD"]
                keyAlias = localProperties["RELEASE_KEY_ALIAS"]
                keyPassword = localProperties["RELEASE_KEY_PASSWORD"]
                // minSdk is 26, so v1 (JAR) signing is never used by the platform
                // and asking for it only creates the illusion of coverage.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.duylt.trave.vietlensai"
        minSdk = 26
        targetSdk = 36
        // Read from gradle.properties rather than written here, because the same two values
        // are also compiled into `:shared` (the Settings screen shows the version) and that
        // module cannot see this block. Hard-coding both had already let them drift: the APK
        // said 1.0 while the footer said 1.0.0. Overridable per invocation with
        // `-Pvietlens.versionName=…`, which is what a CI job that stamps builds would use.
        versionCode = vietlensVersionCode
        versionName = vietlensVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The Maps SDK reads its key from a manifest meta-data entry rather than from
        // code, so it cannot go through the generated `DataBuildConfig` the way the
        // Gemini and Places keys do. It is substituted into `${MAPS_API_KEY}` in
        // AndroidManifest.xml from the git-ignored local.properties.
        //
        // Empty is a deliberate, working default: the app still builds and every other
        // screen still runs — the Explore map is the one thing that comes up blank, and
        // it says so rather than crashing.
        manifestPlaceholders["MAPS_API_KEY"] = localProperties["MAPS_API_KEY"].orEmpty()

        // Date-stamped artifact names, so the APK handed round during a demo says
        // which build it is. Note: with the configuration cache on, this date is
        // computed once and frozen into the cache entry, so it only rolls over
        // when something else invalidates configuration.
        val buildDate = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
        base.archivesName.set("VietLensAI_v${versionName}_build${versionCode}_$buildDate")
    }

    buildTypes {
        debug {
            // Fast iteration: never shrink the build you are actively editing.
            isMinifyEnabled = false
            isShrinkResources = false
            // Its own package, so the dev build installs *beside* the signed demo
            // build instead of refusing to (signatures differ) or replacing it.
            // The APK handed round at a demo keeps its data while development
            // carries on against a separate database on the same phone.
            applicationIdSuffix = ".dev"
            // Surfaces on the Settings screen, so it is obvious at a glance
            // whether a device is running the dev build or the demo build.
            versionNameSuffix = "-dev"
        }
        release {
            /*
             * AGP 9 note: code shrinking is still driven by `isMinifyEnabled`.
             * The `optimization { enable = ... }` block the project template
             * generated here is a *different*, experimental "gradual R8" feature
             * that hard-fails without `android.r8.gradual.support=true` — it is
             * not the migration path for isMinifyEnabled, so it has been removed.
             */
            isMinifyEnabled = true
            isShrinkResources = true

            // "proguard-android.txt" is rejected outright by AGP 9 because it
            // implies -dontoptimize; the -optimize variant is now the only choice.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        /*
         * The APK handed round at a demo: release in every respect that the app can
         * observe — signed with the release key, `BuildConfig.DEBUG` false, so Ktor's
         * `Logging` plugin stays off and the same applicationId installs over the real
         * thing — except that R8 does not run.
         *
         * That exception is the entire point. A profiled `:app:assembleRelease` spends
         * 44-61s in `minifyReleaseWithR8` and under 12s in everything else combined,
         * so shrinking is not a part of the build, it *is* the build. Producing an APK
         * for a demo through it means waiting a minute for an optimisation nobody
         * watching the demo can perceive.
         *
         * Named for what it *is* rather than what it is for: `demo` would have read as
         * "the build with the seeded demo data" next to `tools/seed_demo.py`, which is
         * a different thing entirely and orthogonal to this one.
         *
         * `release` is still what ships. Anything that only reproduces under
         * obfuscation — a missing keep rule, a serializer resolved by name — is
         * invisible here by construction, so a build meant for Play must go through
         * `assembleRelease` and be smoke-tested there.
         */
        create("fastRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            // AGP rejects resource shrinking without code shrinking.
            isShrinkResources = false
            // Distinguishes it in Android's app info. Not in the Settings footer:
            // that reads `vietlens.versionName` from :shared, which has no build type.
            versionNameSuffix = "-fast"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    bundle {
        language {
            // The app ships Vietnamese and English strings and lets the traveller
            // switch between them in Settings. Per-language splits would have Play
            // install only the device locale, so the in-app switch would find the
            // other language missing.
            enableSplit = false
        }
    }

    lint {
        // Lint still runs and still reports, but a newly-introduced warning must
        // never be the reason a demo build cannot be produced.
        abortOnError = false

        // `abortOnError = false` only stops lint *failing* the build; `lintVital`
        // still runs on every non-debuggable variant, and it was measured at 12.7s.
        // Under `assembleRelease` that hid behind R8, which runs longer and in
        // parallel — but `fastRelease` has no R8 to hide behind, so lint would become
        // the whole build and the variant would miss its point.
        //
        // Nothing is lost that `abortOnError = false` had not already given up:
        // lint could not block a release either way. It is still there on demand,
        // as `./gradlew :app:lint`.
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    // The Application sets the log threshold, so the host needs the logger directly
    // rather than only transitively through `:shared`.
    implementation(libs.kermit)

    // Compose is pulled in transitively at the versions the Compose Multiplatform
    // plugin pins in `:shared`; only the Android-only tooling is named here.
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // The end-to-end suite resolves repositories from the running app's Koin graph.
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
