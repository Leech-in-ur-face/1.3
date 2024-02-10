package com.rian.osu.beatmap.timings

import com.rian.osu.beatmap.constants.SampleBank

/**
 * A manager for sample control points.
 */
class SampleControlPointManager : ControlPointManager<SampleControlPoint>(
    SampleControlPoint(0.0, SampleBank.Normal, 100, 0)
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time, controlPoints.getOrNull(0) ?: defaultControlPoint)

    override fun clone() = super.clone() as SampleControlPointManager
}