package org.monogram.presentation.core.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.time.TimeSource

internal class RelativeVelocityTracker(
    private val timeProvider: CurrentTimeProvider = CurrentTimeProviderImpl()
) {
    private val tracker = VelocityTracker()
    private var lastY: Float? = null

    fun delta(delta: Float) {
        val new = (lastY ?: 0f) + delta

        tracker.addPosition(timeProvider.now(), Offset(0f, new))
        lastY = new
    }

    fun reset(): Float {
        lastY = null

        val velocity = tracker.calculateVelocity()
        tracker.resetTracking()

        return velocity.y
    }
}

internal fun RelativeVelocityTracker.deriveDelta(initial: Float) =
    initial - reset()

internal interface CurrentTimeProvider {
    fun now(): Long
}

internal class CurrentTimeProviderImpl : CurrentTimeProvider {
    private val timeSource = TimeSource.Monotonic
    private val startMark = timeSource.markNow()

    override fun now(): Long = startMark.elapsedNow().inWholeMilliseconds
}