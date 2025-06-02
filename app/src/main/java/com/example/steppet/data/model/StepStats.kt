package com.example.steppet.data.model

data class StepStats(
    val totalSteps: Int,
    val averageSteps: Int,
    val bestDay: String?,
    val bestCount: Int,
    val worstDay: String?,
    val worstCount: Int,
    val currentStreak: Int
)
