package com.duylt.trave.vietlensai.data.remote

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Which transport Ktor uses.
 *
 * OkHttp on Android and Darwin (`NSURLSession`) on iOS. Both honour the system proxy
 * and the platform trust store, which matters for a traveller on a hotel network that
 * intercepts TLS: the request fails the same way it would in any other app on the
 * device, rather than in some way only this app produces.
 */
internal expect fun httpClientEngine(): HttpClientEngineFactory<*>
