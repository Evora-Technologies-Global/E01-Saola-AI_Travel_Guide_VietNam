package com.duylt.trave.vietlensai.di

import com.duylt.trave.vietlensai.voice.AndroidSpeechRecognizerManager
import com.duylt.trave.vietlensai.voice.AndroidTextToSpeechManager
import com.duylt.trave.vietlensai.voice.SpeechRecognizerManager
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformUiModule: Module = module {
    // Singletons, not per-screen: a TextToSpeech engine costs the better part of a second to
    // start, and the recogniser holds a microphone session that must not be duplicated.
    single<TextToSpeechManager> { AndroidTextToSpeechManager(androidContext()) }
    single<SpeechRecognizerManager> { AndroidSpeechRecognizerManager(androidContext()) }
}
