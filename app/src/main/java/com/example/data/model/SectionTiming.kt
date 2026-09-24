package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SectionTiming(
    val section: Int,
    val startTime: String,
    val endTime: String
)
