package com.evora.technologies.saola.data.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * On Kotlin/Native `Dispatchers.IO` is a public *extension* property in
 * `kotlinx.coroutines` rather than a member of the `Dispatchers` object — the member of
 * that name is internal — so it has to be imported explicitly. Same dispatcher, same
 * elastic pool; only the import differs from the Android actual.
 */
internal actual fun platformIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
