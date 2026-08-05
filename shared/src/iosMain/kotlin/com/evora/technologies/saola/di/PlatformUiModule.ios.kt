package com.evora.technologies.saola.di

import com.evora.technologies.saola.voice.IosTextToSpeechManager
import com.evora.technologies.saola.voice.TextToSpeechManager
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformUiModule: Module = module {
    single<TextToSpeechManager> { IosTextToSpeechManager() }
}
