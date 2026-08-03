package com.duylt.trave.vietlensai.di

import com.duylt.trave.vietlensai.voice.AndroidTextToSpeechManager
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformUiModule: Module = module {
    // A singleton, not per-screen: a TextToSpeech engine costs the better part of a second to
    // start, and paying that again on every screen makes the speak button feel broken.
    single<TextToSpeechManager> { AndroidTextToSpeechManager(androidContext()) }
}
