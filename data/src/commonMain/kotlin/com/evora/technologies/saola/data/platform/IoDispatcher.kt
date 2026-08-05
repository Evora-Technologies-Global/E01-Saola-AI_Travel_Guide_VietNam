package com.evora.technologies.saola.data.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher for disk and network work.
 *
 * `Dispatchers.IO` cannot be named from common code — it is `internal` there and only
 * published from each platform's own source set — so the choice is made per platform.
 * Both end up on a pool that may grow past the CPU count, which is what blocking file
 * reads and SQLite calls need; `Dispatchers.Default` would cap at core count and let a
 * few slow reads starve everything else.
 */
internal expect fun platformIoDispatcher(): CoroutineDispatcher
