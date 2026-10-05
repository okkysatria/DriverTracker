package com.example.drivertracker.data.repository

import android.location.Location
import com.example.drivertracker.data.model.GpsPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TrackingState {
    IDLE,
    STARTED,
    PICKED_UP
}

object TrackingRepository {

    private val _trackingState = MutableStateFlow(TrackingState.IDLE)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _jarakKePickup = MutableStateFlow(0.0)
    val jarakKePickup: StateFlow<Double> = _jarakKePickup.asStateFlow()

    private val _jarakKeTujuan = MutableStateFlow(0.0)
    val jarakKeTujuan: StateFlow<Double> = _jarakKeTujuan.asStateFlow()

    private val _totalJarak = MutableStateFlow(0.0)
    val totalJarak: StateFlow<Double> = _totalJarak.asStateFlow()

    private val _currentSpeedKmH = MutableStateFlow(0f)
    val currentSpeedKmH: StateFlow<Float> = _currentSpeedKmH.asStateFlow()

    private val _gpsPoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val gpsPoints: StateFlow<List<GpsPoint>> = _gpsPoints.asStateFlow()

    private val _jenisOrder = MutableStateFlow("Penumpang")
    val jenisOrder: StateFlow<String> = _jenisOrder.asStateFlow()

    private val _pendapatanKotor = MutableStateFlow(0.0)
    val pendapatanKotor: StateFlow<Double> = _pendapatanKotor.asStateFlow()

    private val _pendapatanBersih = MutableStateFlow(0.0)
    val pendapatanBersih: StateFlow<Double> = _pendapatanBersih.asStateFlow()

    private val _catatan = MutableStateFlow("")
    val catatan: StateFlow<String> = _catatan.asStateFlow()

    private val _alamatAwal = MutableStateFlow("")
    val alamatAwal: StateFlow<String> = _alamatAwal.asStateFlow()

    private val _alamatPickup = MutableStateFlow("")
    val alamatPickup: StateFlow<String> = _alamatPickup.asStateFlow()

    private val _alamatAkhir = MutableStateFlow("")
    val alamatAkhir: StateFlow<String> = _alamatAkhir.asStateFlow()

    private val _jamMulai = MutableStateFlow(0L)
    val jamMulai: StateFlow<Long> = _jamMulai.asStateFlow()

    private val _jamPickup = MutableStateFlow(0L)
    val jamPickup: StateFlow<Long> = _jamPickup.asStateFlow()

    private val _jamSelesai = MutableStateFlow(0L)
    val jamSelesai: StateFlow<Long> = _jamSelesai.asStateFlow()

    private val _latitudeAwal = MutableStateFlow(0.0)
    val latitudeAwal: StateFlow<Double> = _latitudeAwal.asStateFlow()

    private val _longitudeAwal = MutableStateFlow(0.0)
    val longitudeAwal: StateFlow<Double> = _longitudeAwal.asStateFlow()

    private val _latitudePickup = MutableStateFlow(0.0)
    val latitudePickup: StateFlow<Double> = _latitudePickup.asStateFlow()

    private val _longitudePickup = MutableStateFlow(0.0)
    val longitudePickup: StateFlow<Double> = _longitudePickup.asStateFlow()

    private val _latitudeAkhir = MutableStateFlow(0.0)
    val latitudeAkhir: StateFlow<Double> = _latitudeAkhir.asStateFlow()

    private val _longitudeAkhir = MutableStateFlow(0.0)
    val longitudeAkhir: StateFlow<Double> = _longitudeAkhir.asStateFlow()

    fun updateState(state: TrackingState) {
        _trackingState.value = state
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location

        val speedKmH = if (location.hasSpeed()) location.speed * 3.6f else 0f
        _currentSpeedKmH.value = speedKmH
    }

    fun addGpsPoint(point: GpsPoint) {
        val list = _gpsPoints.value.toMutableList()
        list.add(point)
        _gpsPoints.value = list
    }

    fun updateDuration(seconds: Long) {
        _durationSeconds.value = seconds
    }

    fun addJarakKePickup(deltaKm: Double) {
        _jarakKePickup.value += deltaKm
        _totalJarak.value = _jarakKePickup.value + _jarakKeTujuan.value
    }

    fun addJarakKeTujuan(deltaKm: Double) {
        _jarakKeTujuan.value += deltaKm
        _totalJarak.value = _jarakKePickup.value + _jarakKeTujuan.value
    }

    fun setJenisOrder(jenis: String) {
        _jenisOrder.value = jenis
    }

    fun setPendapatanKotor(nilai: Double) {
        _pendapatanKotor.value = nilai
    }

    fun setPendapatanBersih(nilai: Double) {
        _pendapatanBersih.value = nilai
    }

    fun setCatatan(text: String) {
        _catatan.value = text
    }

    fun setAlamatAwal(alamat: String) {
        _alamatAwal.value = alamat
    }

    fun setAlamatPickup(alamat: String) {
        _alamatPickup.value = alamat
    }

    fun setAlamatAkhir(alamat: String) {
        _alamatAkhir.value = alamat
    }

    fun setInitialLocation(lat: Double, lng: Double, time: Long) {
        _latitudeAwal.value = lat
        _longitudeAwal.value = lng
        _jamMulai.value = time
    }

    fun setPickupLocation(lat: Double, lng: Double, time: Long) {
        _latitudePickup.value = lat
        _longitudePickup.value = lng
        _jamPickup.value = time
    }

    fun setFinalLocation(lat: Double, lng: Double, time: Long) {
        _latitudeAkhir.value = lat
        _longitudeAkhir.value = lng
        _jamSelesai.value = time
    }

    fun reset() {
        _trackingState.value = TrackingState.IDLE
        _currentLocation.value = null
        _durationSeconds.value = 0L
        _jarakKePickup.value = 0.0
        _jarakKeTujuan.value = 0.0
        _totalJarak.value = 0.0
        _currentSpeedKmH.value = 0f
        _gpsPoints.value = emptyList()
        _jenisOrder.value = "Penumpang"
        _pendapatanKotor.value = 0.0
        _pendapatanBersih.value = 0.0
        _catatan.value = ""
        _alamatAwal.value = ""
        _alamatPickup.value = ""
        _alamatAkhir.value = ""
        _jamMulai.value = 0L
        _jamPickup.value = 0L
        _jamSelesai.value = 0L
        _latitudeAwal.value = 0.0
        _longitudeAwal.value = 0.0
        _latitudePickup.value = 0.0
        _longitudePickup.value = 0.0
        _latitudeAkhir.value = 0.0
        _longitudeAkhir.value = 0.0
    }
}
