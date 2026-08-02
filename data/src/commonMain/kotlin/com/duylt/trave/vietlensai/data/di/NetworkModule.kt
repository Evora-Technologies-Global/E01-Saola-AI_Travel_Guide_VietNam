package com.duylt.trave.vietlensai.data.di

import com.duylt.trave.vietlensai.data.remote.httpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.plugins.logging.Logger as KtorLogger

/**
 * Ktor's own traffic, under one tag so it can be filtered out on its own.
 *
 * Named for the transport rather than for Gemini: the same client now also carries the
 * Overpass, Wikipedia and Commons calls behind the Explore map, and a log line about
 * OpenStreetMap tagged "Gemini" sends whoever is reading it to the wrong subsystem.
 */
private val networkLog = KermitLogger.withTag("Network")

/**
 * The one [HttpClient] the whole app shares.
 *
 * @param isDebug true only for a development build. It is passed in rather than read from a
 *   generated constant because this module is compiled once and linked into both of `:app`'s
 *   build types — see the note in `data/build.gradle.kts`. The caller is the one place that
 *   can answer: `BuildConfig.DEBUG` on Android, `#if DEBUG` on iOS.
 */
fun networkModule(isDebug: Boolean): Module = module {

    single<HttpClient> {
        val json = get<Json>()

        HttpClient(httpClientEngine()) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                // Vision requests carry a base64 image up and a few kB of prose back;
                // Pro models under load genuinely take the better part of a minute.
                requestTimeoutMillis = 90_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 90_000
            }

            /*
             * One retry, transport failures only.
             *
             * HTTP-level retries are handled a layer up in GeminiClient, which can do
             * something smarter than sleeping — it moves to a different model. Retrying
             * here as well would multiply latency on exactly the requests that are
             * already slow.
             *
             * `kotlinx.io.IOException` rather than `java.io.IOException`: on the JVM it
             * is a typealias for exactly that class, and on Kotlin/Native it is what
             * Ktor's Darwin engine actually throws.
             */
            install(HttpRequestRetry) {
                maxRetries = 1
                retryOnExceptionIf { _, cause -> cause is IOException }
                exponentialDelay(base = 2.0, maxDelayMs = 4_000)
            }

            // Release installs no logger at all: with the plugin never referenced, R8 drops
            // Ktor's whole logging path out of the shipped APK rather than leaving it dormant.
            if (isDebug) {
                install(Logging) {
                    logger = object : KtorLogger {
                        override fun log(message: String) {
                            networkLog.v { message }
                        }
                    }
                    // HEADERS, not BODY: request bodies contain the base64 image and
                    // response bodies the full narrative — logging either floods the
                    // console and truncates the parts that are actually useful.
                    level = LogLevel.HEADERS
                    sanitizeHeader { header -> header.equals("x-goog-api-key", ignoreCase = true) }
                }
            }
        }
    }
}
