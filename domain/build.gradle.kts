plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

/*
 * :domain is the pure-Kotlin core. It has no Android, no Apple and no framework
 * dependencies, which keeps business rules testable on any target and impossible to
 * couple to the UI. Everything lives in commonMain — there is deliberately no
 * androidMain or iosMain here; a rule that needs a platform belongs in :data.
 */
kotlin {
    jvmToolchain(17)

    // AGP 9 drives the Android target of a multiplatform module through this block
    // rather than through the top-level `android { }` of `com.android.library`.
    // (`androidLibrary { }` was the AGP 8 spelling and is deprecated.)
    android {
        namespace = "com.evora.technologies.saola.domain"
        compileSdk = 37
        minSdk = 26

        // Runs commonTest on the JVM. Without this the Android target contributes no
        // test compilation at all and `check` would only exercise the Apple targets.
        withHostTest {}
    }

    // `:app` is what assembles the framework Xcode links against; these targets exist
    // so the module can be compiled and tested for iOS on its own.
    // iosArm64 for devices, iosSimulatorArm64 for the simulator. iosX64 is deliberately
    // absent: Xcode 26 ships no Intel iOS simulator, and androidx.sqlite:sqlite-bundled
    // — which Room needs on Native — no longer publishes an ios_x64 artifact either.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            // `api`, not `implementation`: domain models carry Instant and LocalDate
            // in their public signatures, so every consumer needs the same types.
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
