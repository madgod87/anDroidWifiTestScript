package com.example.wifitest.network

import kotlin.math.max
import kotlin.math.min

object NetworkQualityAnalyzer {

    data class QualityResult(
        val score: Int,
        val label: String
    )

    /**
     * Calculates reliability score based on:
     * score = 100 - (loss * 8) - (latency / 10) - (jitter * 2)
     */
    fun calculateReliability(
        latencyMs: Long,
        jitterMs: Long,
        packetLossPercent: Int
    ): QualityResult {
        val calculatedScore = 100 - 
                             (packetLossPercent * 8) - 
                             (latencyMs / 10).toInt() - 
                             (jitterMs * 2).toInt()

        val clampedScore = max(0, min(100, calculatedScore))
        
        val label = when {
            clampedScore >= 85 -> "Excellent"
            clampedScore >= 65 -> "Good"
            clampedScore >= 40 -> "Fair"
            else -> "Poor"
        }

        return QualityResult(clampedScore, label)
    }
}
