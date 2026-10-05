package com.example.drivertracker

import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.data.repository.TrackingRepository
import com.example.drivertracker.data.repository.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackingRepositoryTest {

    @Before
    fun setUp() {
        TrackingRepository.reset()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(TrackingState.IDLE, TrackingRepository.trackingState.value)
        assertEquals(0L, TrackingRepository.durationSeconds.value)
        assertEquals(0.0, TrackingRepository.totalJarak.value, 0.001)
        assertEquals(0, TrackingRepository.gpsPoints.value.size)
    }

    @Test
    fun testStateTransitionAndDistanceTracking() {
        TrackingRepository.updateState(TrackingState.STARTED)
        assertEquals(TrackingState.STARTED, TrackingRepository.trackingState.value)

        TrackingRepository.addJarakKePickup(1.5)
        assertEquals(1.5, TrackingRepository.jarakKePickup.value, 0.001)
        assertEquals(1.5, TrackingRepository.totalJarak.value, 0.001)

        TrackingRepository.updateState(TrackingState.PICKED_UP)
        assertEquals(TrackingState.PICKED_UP, TrackingRepository.trackingState.value)

        TrackingRepository.addJarakKeTujuan(3.0)
        assertEquals(3.0, TrackingRepository.jarakKeTujuan.value, 0.001)
        assertEquals(4.5, TrackingRepository.totalJarak.value, 0.001)
    }

    @Test
    fun testGpsPointsAndReset() {
        val point1 = GpsPoint(-6.200000, 106.816666, System.currentTimeMillis(), 30f)
        TrackingRepository.addGpsPoint(point1)

        assertEquals(1, TrackingRepository.gpsPoints.value.size)
        assertEquals(point1, TrackingRepository.gpsPoints.value[0])

        TrackingRepository.reset()

        assertEquals(TrackingState.IDLE, TrackingRepository.trackingState.value)
        assertEquals(0, TrackingRepository.gpsPoints.value.size)
        assertEquals(0.0, TrackingRepository.totalJarak.value, 0.001)
    }
}
