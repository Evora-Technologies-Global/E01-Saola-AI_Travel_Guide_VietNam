package com.evora.technologies.saola.core.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Carries a volume-key press from the window to whoever is holding the camera.
 *
 * The keys only reach an `Activity`, and the shutter only exists on one screen, so
 * something has to sit between them. Arming is explicit rather than always-on:
 * swallowing the volume keys app-wide would leave the traveller unable to turn a
 * guide's narration down while reading a discovery.
 */
class VolumeShutterBus {

    private val _presses = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        // A press that arrives while the last one is still being handled is a
        // double-tap on a physical button — dropped, not queued into two photos.
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )
    val presses: SharedFlow<Unit> = _presses.asSharedFlow()

    /**
     * True only while a viewfinder is on screen and listening.
     *
     * No `@Volatile`: it is JVM-only, and both writers are the main thread — the Compose
     * effect that arms the bus and the key dispatch that reads it.
     */
    var isArmed: Boolean = false
        private set

    fun arm() {
        isArmed = true
    }

    fun disarm() {
        isArmed = false
    }

    fun press() {
        if (isArmed) _presses.tryEmit(Unit)
    }
}
