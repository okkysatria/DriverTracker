package com.example.drivertracker.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.example.drivertracker.data.local.AppDatabase
import com.example.drivertracker.data.local.entity.OrderRecord
import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.data.preferences.AppPreferences
import com.example.drivertracker.data.repository.TrackingRepository
import com.example.drivertracker.data.repository.TrackingState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null

    private var lastLocation: Location? = null
    private var trackingIntervalMs: Long = 5000L
    private var isCompletingOrder = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    handleNewLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ServiceActionReceiver.ACTION_START_TRACKING -> {
                val jenisOrder = intent.getStringExtra("jenisOrder") ?: "Penumpang"
                val pendapatanKotor = intent.getDoubleExtra("pendapatanKotor", 0.0)
                val pendapatanBersih = intent.getDoubleExtra("pendapatanBersih", 0.0)
                val catatan = intent.getStringExtra("catatan") ?: ""
                val alamatAwal = intent.getStringExtra("alamatAwal") ?: ""
                val alamatPickup = intent.getStringExtra("alamatPickup") ?: ""
                val alamatAkhir = intent.getStringExtra("alamatAkhir") ?: ""

                startTracking(
                    jenisOrder = jenisOrder,
                    pendapatanKotor = pendapatanKotor,
                    pendapatanBersih = pendapatanBersih,
                    catatan = catatan,
                    alamatAwal = alamatAwal,
                    alamatPickup = alamatPickup,
                    alamatAkhir = alamatAkhir
                )
            }
            ServiceActionReceiver.ACTION_PICKUP_CONFIRMED -> {
                confirmPickup(intent.getStringExtra("alamatPickup").orEmpty())
            }
            ServiceActionReceiver.ACTION_COMPLETE_ORDER -> {
                completeOrder()
            }
            ServiceActionReceiver.ACTION_CANCEL_ORDER -> {
                cancelOrder()
            }
        }

        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun startTracking(
        jenisOrder: String,
        pendapatanKotor: Double,
        pendapatanBersih: Double,
        catatan: String,
        alamatAwal: String,
        alamatPickup: String,
        alamatAkhir: String
    ) {
        if (TrackingRepository.trackingState.value != TrackingState.IDLE) return
        lastLocation = null
        isCompletingOrder = false
        TrackingRepository.reset()
        TrackingRepository.updateState(TrackingState.STARTED)
        TrackingRepository.setJenisOrder(jenisOrder)
        TrackingRepository.setPendapatanKotor(pendapatanKotor)
        TrackingRepository.setPendapatanBersih(pendapatanBersih)
        TrackingRepository.setCatatan(catatan)
        TrackingRepository.setAlamatAwal(alamatAwal)
        TrackingRepository.setAlamatPickup(alamatPickup)
        TrackingRepository.setAlamatAkhir(alamatAkhir)

        val now = System.currentTimeMillis()
        TrackingRepository.setInitialLocation(0.0, 0.0, now)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DriverTracker::TrackingWakeLock").apply {
            acquire()
        }

        val notification = TrackingNotificationManager.buildNotification(
            this,
            TrackingState.STARTED,
            0L,
            0.0,
            0f
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                TrackingNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(TrackingNotificationManager.NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            val appPrefs = AppPreferences.getInstance(applicationContext)
            val intervalSec = appPrefs.trackingIntervalSec.first()
            trackingIntervalMs = (intervalSec * 1000).toLong().coerceAtLeast(2000L)
            requestLocationUpdates()
        }

        startTimer()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, trackingIntervalMs)
            .setMinUpdateIntervalMillis(trackingIntervalMs / 2)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    handleNewLocation(loc)
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun handleNewLocation(location: Location) {
        val speedKmH = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val gpsPoint = GpsPoint(
            lat = location.latitude,
            lng = location.longitude,
            time = location.time,
            speed = speedKmH
        )

        if (TrackingRepository.latitudeAwal.value == 0.0 && TrackingRepository.longitudeAwal.value == 0.0) {
            TrackingRepository.setInitialLocation(location.latitude, location.longitude, System.currentTimeMillis())
        }

        val state = TrackingRepository.trackingState.value
        lastLocation?.let { prev ->
            val distanceMeters = prev.distanceTo(location)

            if (distanceMeters in 3.0..2000.0) {
                val deltaKm = distanceMeters / 1000.0
                when (state) {
                    TrackingState.STARTED -> TrackingRepository.addJarakKePickup(deltaKm)
                    TrackingState.PICKED_UP -> TrackingRepository.addJarakKeTujuan(deltaKm)
                    TrackingState.IDLE -> {}
                }
            }
        }

        lastLocation = location
        TrackingRepository.updateLocation(location)
        TrackingRepository.addGpsPoint(gpsPoint)

        updateNotification()
    }

    private fun confirmPickup(address: String) {
        if (TrackingRepository.trackingState.value == TrackingState.STARTED) {
            val now = System.currentTimeMillis()
            val loc = TrackingRepository.currentLocation.value
            TrackingRepository.setPickupLocation(
                loc?.latitude ?: 0.0,
                loc?.longitude ?: 0.0,
                now
            )
            if (address.isNotBlank()) TrackingRepository.setAlamatPickup(address)
            TrackingRepository.updateState(TrackingState.PICKED_UP)
            updateNotification()
        }
    }

    private fun completeOrder() {
        if (TrackingRepository.trackingState.value != TrackingState.PICKED_UP || isCompletingOrder) return
        isCompletingOrder = true
        val now = System.currentTimeMillis()
        val loc = TrackingRepository.currentLocation.value
        TrackingRepository.setFinalLocation(
            loc?.latitude ?: 0.0,
            loc?.longitude ?: 0.0,
            now
        )

        serviceScope.launch {
            try {
            val appPrefs = AppPreferences.getInstance(applicationContext)
            val isEstimasiBensin = appPrefs.isEstimasiBensinAktif.first()
            val konsumsi = appPrefs.konsumsiBbm.first()
            val hargaBensin = appPrefs.hargaBensin.first()
            val isKomisiAktif = appPrefs.isKomisiAktif.first()
            val persenKomisi = appPrefs.persenKomisi.first()

            val totalKm = TrackingRepository.totalJarak.value
            val kotor = TrackingRepository.pendapatanKotor.value

            val bersih = if (isKomisiAktif && kotor > 0) {
                kotor * (1.0 - (persenKomisi / 100.0))
            } else if (TrackingRepository.pendapatanBersih.value > 0) {
                TrackingRepository.pendapatanBersih.value
            } else {
                kotor
            }

            val estimasiBensin = if (isEstimasiBensin && konsumsi > 0) {
                (totalKm / konsumsi) * hargaBensin
            } else {
                0.0
            }

            val jamMulaiVal = TrackingRepository.jamMulai.value.let { if (it == 0L) now else it }
            val tanggalStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(jamMulaiVal))
            val hariStr = SimpleDateFormat("EEEE", Locale.forLanguageTag("id-ID")).format(Date(jamMulaiVal))

            val gpsJson = try {
                val jsonArray = JSONArray()
                for (point in TrackingRepository.gpsPoints.value) {
                    val obj = JSONObject().apply {
                        put("lat", point.lat)
                        put("lng", point.lng)
                        put("time", point.time)
                        put("speed", point.speed)
                    }
                    jsonArray.put(obj)
                }
                jsonArray.toString()
            } catch (_: Exception) {
                "[]"
            }

            val record = OrderRecord(
                tanggal = tanggalStr,
                hari = hariStr,
                jamMulai = jamMulaiVal,
                jamPickup = TrackingRepository.jamPickup.value,
                jamSelesai = now,
                jenisOrder = TrackingRepository.jenisOrder.value,
                pendapatanBersih = bersih,
                pendapatanKotor = kotor,
                durasi = TrackingRepository.durationSeconds.value,
                jarakTempuh = totalKm,
                jarakKePickup = TrackingRepository.jarakKePickup.value,
                jarakKeTujuan = TrackingRepository.jarakKeTujuan.value,
                latitudeAwal = TrackingRepository.latitudeAwal.value,
                longitudeAwal = TrackingRepository.longitudeAwal.value,
                alamatAwal = TrackingRepository.alamatAwal.value,
                latitudePickup = TrackingRepository.latitudePickup.value,
                longitudePickup = TrackingRepository.longitudePickup.value,
                alamatPickup = TrackingRepository.alamatPickup.value,
                latitudeAkhir = TrackingRepository.latitudeAkhir.value,
                longitudeAkhir = TrackingRepository.longitudeAkhir.value,
                alamatAkhir = TrackingRepository.alamatAkhir.value,
                catatan = TrackingRepository.catatan.value,
                trackGpsJson = gpsJson,
                biayaBensin = estimasiBensin
            )

            AppDatabase.getDatabase(applicationContext).orderDao().insertOrder(record)

            stopTracking()
            } finally {
                isCompletingOrder = false
            }
        }
    }

    private fun cancelOrder() {
        stopTracking()
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()

        TrackingRepository.reset()
        TrackingNotificationManager.showNotification(applicationContext, state = TrackingState.IDLE)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1000L)
                val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1000L
                TrackingRepository.updateDuration(elapsed)
                if (elapsed % 3L == 0L) {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        val state = TrackingRepository.trackingState.value
        if (state == TrackingState.IDLE) return

        val notification = TrackingNotificationManager.buildNotification(
            this,
            state,
            TrackingRepository.durationSeconds.value,
            TrackingRepository.totalJarak.value,
            TrackingRepository.currentSpeedKmH.value
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(TrackingNotificationManager.NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        serviceScope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }
}
