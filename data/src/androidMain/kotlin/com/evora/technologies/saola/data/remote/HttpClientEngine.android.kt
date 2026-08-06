package com.evora.technologies.saola.data.remote

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun httpClientEngine(): HttpClientEngineFactory<*> = OkHttp
