package com.example.drivertracker.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val time: Long,
    val speed: Float = 0f
)
