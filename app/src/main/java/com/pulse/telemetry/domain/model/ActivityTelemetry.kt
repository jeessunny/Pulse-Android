package com.pulse.telemetry.domain.model

import java.util.Locale

data class ActivityTelemetry(
    val stepsCount: Int = 0,
    val stepGoal: Int = 10000,
    val motionState: String = "Stationary",
    val distanceKm: Float = 0f,
    val caloriesBurned: Int = 0
) {
    val progressPercentage: Int
        get() = if (stepGoal > 0) ((stepsCount.toFloat() / stepGoal) * 100).toInt().coerceIn(0, 100) else 0

    val formattedSteps: String
        get() = String.format(Locale.US, "%,d", stepsCount)

    val formattedDistance: String
        get() = String.format(Locale.US, "%.2f km", distanceKm)
}
