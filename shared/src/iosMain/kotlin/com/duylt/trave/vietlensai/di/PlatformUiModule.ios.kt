package com.duylt.trave.vietlensai.di

import com.duylt.trave.vietlensai.voice.IosTextToSpeechManager
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformUiModule: Module = module {
    single<TextToSpeechManager> { IosTextToSpeechManager() }
}
