# =============================================================================
#  :data — consumer keep rules
#
#  Empty, and currently INERT. Read this before adding a rule here.
#
#  This file is a leftover from when `:data` was a `com.android.library`, where
#  the template wires it with `consumerProguardFiles("consumer-rules.pro")`. The
#  module is now built by `com.android.kotlin.multiplatform.library`, and that
#  plugin publishes consumer rules only when asked: `ConsumerKeepRules.publish`
#  defaults to false. Nothing in `data/build.gradle.kts` names this file, so R8
#  never sees it — it does not appear in
#  app/build/outputs/mapping/release/configuration.txt.
#
#  It is left in place rather than wired, because there is no rule to put in it:
#  a minified `:app:assembleRelease` produces no missing_rules.txt, and the one
#  reflective path in this module (Ktor's ContentNegotiation resolving a
#  serializer from a KType) is already covered by kotlinx-serialization-core's
#  own shipped rules. See app/proguard-rules.pro for the full reasoning.
#
#  If a rule ever does have to travel with `:data` rather than live in the app,
#  add it below AND wire the file, inside the `android { }` block of
#  data/build.gradle.kts:
#
#      optimization {
#          consumerKeepRules {
#              files.from(layout.projectDirectory.file("consumer-rules.pro"))
#              publish = true
#          }
#      }
#
#  Then confirm it took effect: the file must show up as its own section in
#  app/build/outputs/mapping/release/configuration.txt after a release build.
#  (The DSL is @Incubating in AGP 9.2.1, so expect it to move.)
# =============================================================================
