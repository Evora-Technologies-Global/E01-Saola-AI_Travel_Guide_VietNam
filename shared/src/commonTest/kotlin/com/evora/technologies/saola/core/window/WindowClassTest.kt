package com.evora.technologies.saola.core.window

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two window questions, at the sizes real devices actually report.
 *
 * Both classifiers are one comparison each, which is exactly why they are worth pinning: the
 * cost of getting one wrong is not a crash or a failing assertion somewhere else, it is a
 * device quietly drawing the other arrangement. An iPad in portrait misses the width gate by
 * six points and a phone in landscape misses the height gate by 216 — the first of those is
 * close enough that an inclusive `>=` turned into `>` would change a shipped device's branch
 * and nothing on screen would look broken enough for anyone to file it.
 *
 * The measurements below are from the four devices this app has been run on, named so a
 * future reading knows which numbers are real and which are boundary probes.
 */
class WindowClassTest {

    // ---- Which branch: mobile/ or tablet/ ----

    @Test
    fun `a phone upright is compact`() {
        // Galaxy A16, 1080 × 2340 at density 450.
        assertEquals(WindowClass.COMPACT, windowClassOf(384.dp, 832.dp))
    }

    @Test
    fun `a phone sideways is compact despite being wide enough`() {
        // The case the height gate exists for: 832 clears 840 on nothing but it is the width
        // that would have passed on a larger phone, and the master-detail layout it would have
        // been handed assumes a page's worth of vertical room.
        assertEquals(WindowClass.COMPACT, windowClassOf(891.dp, 411.dp))
    }

    @Test
    fun `a tablet in landscape is expanded`() {
        // Pixel Tablet, 1280 × 800 dp.
        assertEquals(WindowClass.EXPANDED, windowClassOf(1280.dp, 800.dp))
    }

    @Test
    fun `an iPad in portrait falls to the phone arrangement by six points`() {
        // iPad Pro 11-inch, 834 × 1210 pt. Verified on the simulator on 04.08.2026: portrait
        // draws the phone's bottom bar, landscape draws the rail and the panel.
        assertEquals(WindowClass.COMPACT, windowClassOf(834.dp, 1210.dp))
        assertEquals(WindowClass.EXPANDED, windowClassOf(1210.dp, 834.dp))
    }

    @Test
    fun `both gates are inclusive at exactly their own value`() {
        assertEquals(WindowClass.EXPANDED, windowClassOf(840.dp, 600.dp))
        assertEquals(WindowClass.COMPACT, windowClassOf(839.dp, 600.dp))
        assertEquals(WindowClass.COMPACT, windowClassOf(840.dp, 599.dp))
    }

    // ---- Which arrangement inside mobile/: stacked or side by side ----

    @Test
    fun `a phone upright can stack`() {
        assertTrue(canStackVertically(832.dp))
    }

    @Test
    fun `a phone sideways cannot stack`() {
        // 384dp against 214dp of lens chrome. This is the whole reason
        // `mobile/feature/camera/LensScreen.kt` has a second arrangement.
        assertFalse(canStackVertically(384.dp))
        // And the iPhone's landscape height, which is the taller of the two and still short.
        assertFalse(canStackVertically(411.dp))
    }

    @Test
    fun `an unfolded fold can still stack`() {
        // ~673dp tall, which is under the tablet's height gate and over this one — the two
        // thresholds answering different questions about the same window, as intended.
        assertTrue(canStackVertically(673.dp))
        assertEquals(WindowClass.COMPACT, windowClassOf(673.dp, 673.dp))
    }

    @Test
    fun `the stacking gate is inclusive at exactly its own value`() {
        assertTrue(canStackVertically(500.dp))
        assertFalse(canStackVertically(499.dp))
    }
}
