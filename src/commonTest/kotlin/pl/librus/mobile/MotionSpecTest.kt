package pl.librus.mobile

import pl.librus.mobile.data.storage.MemoryPersistentStorage
import pl.librus.mobile.ui.animation.MotionSpec
import kotlin.test.*

class MotionSpecTest {

    @Test
    fun testDefaultMotionSpec() {
        val spec = MotionSpec()
        assertEquals(1.0f, spec.speedMultiplier)
        assertTrue(spec.isAnimationsEnabled)
        assertEquals(300, spec.duration(300))
    }

    @Test
    fun testDurationScalingAtVariousSpeeds() {
        // Fast 2.0x -> half duration
        val fastSpec = MotionSpec(2.0f)
        assertTrue(fastSpec.isAnimationsEnabled)
        assertEquals(150, fastSpec.duration(300))
        assertEquals(50, fastSpec.duration(100))

        // Snappy 1.5x -> 300 / 1.5 = 200ms
        val snappySpec = MotionSpec(1.5f)
        assertTrue(snappySpec.isAnimationsEnabled)
        assertEquals(200, snappySpec.duration(300))

        // Relaxed 0.5x -> double duration
        val relaxedSpec = MotionSpec(0.5f)
        assertTrue(relaxedSpec.isAnimationsEnabled)
        assertEquals(600, relaxedSpec.duration(300))
    }

    @Test
    fun testDisabledAnimationsReturnsZeroDuration() {
        val offSpec = MotionSpec(0.0f)
        assertFalse(offSpec.isAnimationsEnabled)
        assertEquals(0, offSpec.duration(300))
        assertEquals(0, offSpec.duration(1000))

        val nearZeroSpec = MotionSpec(0.02f)
        assertFalse(nearZeroSpec.isAnimationsEnabled)
        assertEquals(0, nearZeroSpec.duration(500))
    }

    @Test
    fun testTweenSpecScaling() {
        val spec = MotionSpec(2.0f)
        val tween = spec.tween<Float>(baseMillis = 400, delayMillis = 100)
        assertEquals(200, tween.durationMillis)
        assertEquals(50, tween.delay)

        val disabledSpec = MotionSpec(0.0f)
        val disabledTween = disabledSpec.tween<Float>(baseMillis = 400, delayMillis = 100)
        assertEquals(0, disabledTween.durationMillis)
        assertEquals(0, disabledTween.delay)
    }

    @Test
    fun testSpeedLabelsPolishAndEnglish() {
        assertEquals("Wyłączone (0x)", MotionSpec.getSpeedLabel(0.0f, isPl = true))
        assertEquals("Off (0x)", MotionSpec.getSpeedLabel(0.0f, isPl = false))

        assertEquals("Spokojne (0.5x)", MotionSpec.getSpeedLabel(0.5f, isPl = true))
        assertEquals("Relaxed (0.5x)", MotionSpec.getSpeedLabel(0.5f, isPl = false))

        assertEquals("Standardowe (1.0x)", MotionSpec.getSpeedLabel(1.0f, isPl = true))
        assertEquals("Standard (1.0x)", MotionSpec.getSpeedLabel(1.0f, isPl = false))

        assertEquals("Szybkie (1.5x)", MotionSpec.getSpeedLabel(1.5f, isPl = true))
        assertEquals("Fast (1.5x)", MotionSpec.getSpeedLabel(1.5f, isPl = false))

        assertEquals("Błyskawiczne (2.0x)", MotionSpec.getSpeedLabel(2.0f, isPl = true))
        assertEquals("Instant (2.0x)", MotionSpec.getSpeedLabel(2.0f, isPl = false))
    }

    @Test
    fun testPersistenceOfAnimationSpeed() {
        val storage = MemoryPersistentStorage()
        assertNull(storage.getString(MotionSpec.KEY_ANIMATION_SPEED))

        // Save customized speed
        storage.putString(MotionSpec.KEY_ANIMATION_SPEED, "1.5")
        val restored = storage.getString(MotionSpec.KEY_ANIMATION_SPEED)?.toFloatOrNull()
        assertNotNull(restored)
        assertEquals(1.5f, restored)

        val restoredSpec = MotionSpec(restored)
        assertEquals(200, restoredSpec.duration(300))
    }
}
