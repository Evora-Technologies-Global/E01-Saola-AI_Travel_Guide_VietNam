package com.duylt.trave.vietlensai.data.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual fun platformIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
