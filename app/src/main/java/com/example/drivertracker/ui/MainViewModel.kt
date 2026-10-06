package com.example.drivertracker.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.drivertracker.ml.OnnxModelInfo
import com.example.drivertracker.ml.OnnxModelManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.drivertracker.data.local.AppDatabase
import com.example.drivertracker.data.local.dao.OrderDao
import com.example.drivertracker.data.local.entity.OrderRecord
import com.example.drivertracker.data.model.GpsPoint
import com.example.drivertracker.data.preferences.AppPreferences
import com.example.drivertracker.data.repository.TrackingRepository
import com.example.drivertracker.data.repository.TrackingState
import com.example.drivertracker.service.ServiceActionReceiver
import com.example.drivertracker.ui.screens.routeposter.PosterDraftState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    application: Application,
    private val orderDao: OrderDao,
    private val appPreferences: AppPreferences
) : AndroidViewModel(application) {

    private val _posterDraft = MutableStateFlow(PosterDraftState())
    val posterDraft = _posterDraft.asStateFlow()

    fun updatePosterDraft(update: (PosterDraftState) -> PosterDraftState) {
        _posterDraft.update(update)
    }

    private val saveOrderMutex = Mutex()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: result.locations.lastOrNull()
            loc?.let {
                TrackingRepository.updateLocation(it)
            }
        }
    }

    private var isLocationClientRunning = false

    fun startLocationUpdates() {
        val context = getApplication<Application>()
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) return

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    TrackingRepository.updateLocation(loc)
                }
            }
        } catch (_: SecurityException) {}

        try {
            val locMgr = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            val gpsLoc = locMgr?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val netLoc = locMgr?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val bestLoc = when {
                gpsLoc != null && netLoc != null -> if (gpsLoc.time >= netLoc.time) gpsLoc else netLoc
                gpsLoc != null -> gpsLoc
                else -> netLoc
            }
            if (bestLoc != null && TrackingRepository.currentLocation.value == null) {
                TrackingRepository.updateLocation(bestLoc)
            }
        } catch (_: SecurityException) {}

        if (!isLocationClientRunning) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .setMinUpdateDistanceMeters(1.0f)
                .setWaitForAccurateLocation(false)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                isLocationClientRunning = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
    }

    val trackingState: StateFlow<TrackingState> = TrackingRepository.trackingState
    val currentLocation: StateFlow<Location?> = TrackingRepository.currentLocation
    val durationSeconds: StateFlow<Long> = TrackingRepository.durationSeconds
    val jarakKePickup: StateFlow<Double> = TrackingRepository.jarakKePickup
    val jarakKeTujuan: StateFlow<Double> = TrackingRepository.jarakKeTujuan
    val totalJarak: StateFlow<Double> = TrackingRepository.totalJarak
    val gpsPoints: StateFlow<List<GpsPoint>> = TrackingRepository.gpsPoints

    val isDarkMode: StateFlow<Boolean> = appPreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val trackingIntervalSec: StateFlow<Int> = appPreferences.trackingIntervalSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val konsumsiBbm: StateFlow<Float> = appPreferences.konsumsiBbm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 45.0f)

    val hargaBensin: StateFlow<Double> = appPreferences.hargaBensin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000.0)

    val isKomisiAktif: StateFlow<Boolean> = appPreferences.isKomisiAktif
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val persenKomisi: StateFlow<Float> = appPreferences.persenKomisi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20.0f)

    val isEstimasiBensinAktif: StateFlow<Boolean> = appPreferences.isEstimasiBensinAktif
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val driverName: StateFlow<String> = appPreferences.driverName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val showRadarHistory: StateFlow<Boolean> = appPreferences.radarShowHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showRadarAi: StateFlow<Boolean> = appPreferences.radarShowAi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customOnnxModelName: StateFlow<String> = appPreferences.customOnnxModelName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val posterShowAppName: StateFlow<Boolean> = appPreferences.posterShowAppName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val posterShowDriverName: StateFlow<Boolean> = appPreferences.posterShowDriverName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val posterShowDistance: StateFlow<Boolean> = appPreferences.posterShowDistance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val posterShowIncome: StateFlow<Boolean> = appPreferences.posterShowIncome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val posterShowRouteLine: StateFlow<Boolean> = appPreferences.posterShowRouteLine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val posterShowDuration: StateFlow<Boolean> = appPreferences.posterShowDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val posterShowAvgSpeed: StateFlow<Boolean> = appPreferences.posterShowAvgSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val posterShowMaxSpeed: StateFlow<Boolean> = appPreferences.posterShowMaxSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val posterRouteLineColor: StateFlow<String> = appPreferences.posterRouteLineColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AUTO")

    val posterTextOutlineEnabled = appPreferences.posterTextOutlineEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val posterTextOutlineColor = appPreferences.posterTextOutlineColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000")
    val posterTextOutlineSize = appPreferences.posterTextOutlineSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.5f)
    val posterTextShadowEnabled = appPreferences.posterTextShadowEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val posterTextShadowColor = appPreferences.posterTextShadowColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000")
    val posterTextShadowSize = appPreferences.posterTextShadowSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2f)
    val posterRouteOutlineEnabled = appPreferences.posterRouteOutlineEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val posterRouteOutlineColor = appPreferences.posterRouteOutlineColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#FFFFFF")
    val posterRouteOutlineSize = appPreferences.posterRouteOutlineSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2f)
    val posterRouteShadowEnabled = appPreferences.posterRouteShadowEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val posterRouteShadowColor = appPreferences.posterRouteShadowColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000")
    val posterRouteShadowSize = appPreferences.posterRouteShadowSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2f)

    fun setPosterShowAppName(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowAppName(show) }
    fun setPosterShowDriverName(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowDriverName(show) }
    fun setPosterShowDistance(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowDistance(show) }
    fun setPosterShowIncome(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowIncome(show) }
    fun setPosterShowRouteLine(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowRouteLine(show) }
    fun setPosterShowDuration(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowDuration(show) }
    fun setPosterShowAvgSpeed(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowAvgSpeed(show) }
    fun setPosterShowMaxSpeed(show: Boolean) = viewModelScope.launch { appPreferences.setPosterShowMaxSpeed(show) }
    fun setPosterRouteLineColor(color: String) = viewModelScope.launch { appPreferences.setPosterRouteLineColor(color) }
    fun setPosterTextOutlineEnabled(value: Boolean) = viewModelScope.launch { appPreferences.setPosterTextOutlineEnabled(value) }
    fun setPosterTextOutlineColor(value: String) = viewModelScope.launch { appPreferences.setPosterTextOutlineColor(value) }
    fun setPosterTextOutlineSize(value: Float) = viewModelScope.launch { appPreferences.setPosterTextOutlineSize(value) }
    fun setPosterTextShadowEnabled(value: Boolean) = viewModelScope.launch { appPreferences.setPosterTextShadowEnabled(value) }
    fun setPosterTextShadowColor(value: String) = viewModelScope.launch { appPreferences.setPosterTextShadowColor(value) }
    fun setPosterTextShadowSize(value: Float) = viewModelScope.launch { appPreferences.setPosterTextShadowSize(value) }
    fun setPosterRouteOutlineEnabled(value: Boolean) = viewModelScope.launch { appPreferences.setPosterRouteOutlineEnabled(value) }
    fun setPosterRouteOutlineColor(value: String) = viewModelScope.launch { appPreferences.setPosterRouteOutlineColor(value) }
    fun setPosterRouteOutlineSize(value: Float) = viewModelScope.launch { appPreferences.setPosterRouteOutlineSize(value) }
    fun setPosterRouteShadowEnabled(value: Boolean) = viewModelScope.launch { appPreferences.setPosterRouteShadowEnabled(value) }
    fun setPosterRouteShadowColor(value: String) = viewModelScope.launch { appPreferences.setPosterRouteShadowColor(value) }
    fun setPosterRouteShadowSize(value: Float) = viewModelScope.launch { appPreferences.setPosterRouteShadowSize(value) }

    fun setRadarShowHistory(show: Boolean) {
        viewModelScope.launch {
            appPreferences.setRadarShowHistory(show)
        }
    }

    fun setRadarShowAi(show: Boolean) {
        viewModelScope.launch {
            appPreferences.setRadarShowAi(show)
        }
    }

    suspend fun importOnnxModel(context: Context, uri: Uri, originalName: String): Result<OnnxModelInfo> {
        val result = OnnxModelManager.saveModelFromUri(context, uri, originalName)
        if (result.isSuccess) {
            val info = result.getOrThrow()
            appPreferences.setCustomOnnxModel(info.fileName, context.filesDir.resolve("models/custom_driver_model.onnx").absolutePath)
        }
        return result
    }

    suspend fun removeOnnxModel(context: Context): Boolean {
        val deleted = OnnxModelManager.deleteModel(context)
        appPreferences.clearCustomOnnxModel()
        return deleted
    }

    private val _currentAddress = MutableStateFlow("Mencari lokasi GPS...")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    private val _shouldOpenSaveDialog = MutableStateFlow(false)
    val shouldOpenSaveDialog: StateFlow<Boolean> = _shouldOpenSaveDialog.asStateFlow()

    fun triggerSaveDialog() {
        _shouldOpenSaveDialog.value = true
    }

    fun dismissSaveDialog() {
        _shouldOpenSaveDialog.value = false
    }

    val allOrders: StateFlow<List<OrderRecord>> = orderDao.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todayStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayOrders: Flow<List<OrderRecord>> = orderDao.getAllOrders().map { list ->
        val currentToday = todayStr
        list.filter { it.tanggal == currentToday }
    }

    val todayNetIncome: StateFlow<Double> = todayOrders.map { list ->
        list.sumOf { it.pendapatanBersih }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayOrderCount: StateFlow<Int> = todayOrders.map { list ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTotalDistance: StateFlow<Double> = todayOrders.map { list ->
        list.sumOf { it.jarakTempuh }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayPickupDistance: StateFlow<Double> = todayOrders.map { list ->
        list.sumOf { it.jarakKePickup }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayDeliveryDistance: StateFlow<Double> = todayOrders.map { list ->
        list.sumOf { it.jarakKeTujuan }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        startLocationUpdates()

        viewModelScope.launch {
            currentLocation.collect { loc ->
                if (loc != null) {
                    resolveAddress(loc.latitude, loc.longitude)
                } else {
                    _currentAddress.value = "Mencari lokasi GPS..."
                }
            }
        }

        viewModelScope.launch {
            trackingState.collect { state ->
                com.example.drivertracker.service.TrackingNotificationManager.showNotification(
                    getApplication(),
                    state = state,
                    durationSeconds = TrackingRepository.durationSeconds.value,
                    totalKm = TrackingRepository.totalJarak.value,
                    speedKmH = TrackingRepository.currentSpeedKmH.value
                )
            }
        }
    }

    private fun resolveAddress(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.forLanguageTag("id-ID"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (addresses.isNotEmpty()) {
                                _currentAddress.value = formatAddress(addresses[0])
                            } else {
                                _currentAddress.value = String.format(Locale.US, "%.5f, %.5f", lat, lng)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            _currentAddress.value = String.format(Locale.US, "%.5f, %.5f", lat, lng)
                        }
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        _currentAddress.value = formatAddress(addresses[0])
                    } else {
                        _currentAddress.value = String.format(Locale.US, "%.5f, %.5f", lat, lng)
                    }
                }
            } catch (_: Exception) {
                _currentAddress.value = String.format(Locale.US, "%.5f, %.5f", lat, lng)
            }
        }
    }

    private fun formatAddress(address: Address): String {
        val thoroughfare = address.thoroughfare ?: address.featureName
        val subLocality = address.subLocality
        val locality = address.locality
        val parts = listOfNotNull(thoroughfare, subLocality, locality).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: String.format(Locale.US, "%.5f, %.5f", address.latitude, address.longitude)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDarkMode(enabled)
        }
    }

    fun setTrackingIntervalSec(seconds: Int) {
        viewModelScope.launch {
            appPreferences.setTrackingIntervalSec(seconds)
        }
    }

    fun setKonsumsiBbm(kmPerLiter: Float) {
        viewModelScope.launch {
            appPreferences.setKonsumsiBbm(kmPerLiter)
        }
    }

    fun setHargaBensin(rupiah: Double) {
        viewModelScope.launch {
            appPreferences.setHargaBensin(rupiah)
        }
    }

    fun setKomisiAktif(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setKomisiAktif(enabled)
        }
    }

    fun setPersenKomisi(percent: Float) {
        viewModelScope.launch {
            appPreferences.setPersenKomisi(percent)
        }
    }

    fun setEstimasiBensinAktif(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setEstimasiBensinAktif(enabled)
        }
    }

    fun setDriverName(name: String) {
        viewModelScope.launch {
            appPreferences.setDriverName(name)
        }
    }

    fun startOrder(context: Context, jenisOrder: String = "Penumpang") {
        val intent = Intent(context, ServiceActionReceiver::class.java).apply {
            action = ServiceActionReceiver.ACTION_START_TRACKING
            putExtra("jenisOrder", jenisOrder)
            putExtra("alamatAwal", _currentAddress.value)
        }
        context.sendBroadcast(intent)
    }

    fun confirmPickup(context: Context) {
        val intent = Intent(context, ServiceActionReceiver::class.java).apply {
            action = ServiceActionReceiver.ACTION_PICKUP_CONFIRMED
            putExtra("alamatPickup", _currentAddress.value)
        }
        context.sendBroadcast(intent)
    }

    fun cancelOrder(context: Context) {
        val intent = Intent(context, ServiceActionReceiver::class.java).apply {
            action = ServiceActionReceiver.ACTION_CANCEL_ORDER
        }
        context.sendBroadcast(intent)
    }

    fun saveAndCompleteOrder(
        context: Context,
        jenisOrder: String,
        pendapatanKotor: Double,
        catatan: String,
        biayaBensin: Double,
        onComplete: (String?) -> Unit
    ) {
        if (!saveOrderMutex.tryLock()) return
        viewModelScope.launch(Dispatchers.IO) {
            var failureMessage: String? = null
            try {
            val now = System.currentTimeMillis()
            val grossIncome = pendapatanKotor.coerceAtLeast(0.0)
            val commissionEnabled = appPreferences.isKomisiAktif.first()
            val commissionPercent = appPreferences.persenKomisi.first().coerceIn(0f, 100f)
            val netIncome = if (commissionEnabled) {
                grossIncome * (1.0 - commissionPercent / 100.0)
            } else {
                grossIncome
            }
            val jamMulaiVal = TrackingRepository.jamMulai.value.let { if (it == 0L) now else it }
            val tanggalFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(jamMulaiVal))
            val hariFormatted = SimpleDateFormat("EEEE", Locale.forLanguageTag("id-ID")).format(Date(jamMulaiVal))

            val gpsJson = try {
                val orgJsonArray = JSONArray()
                for (point in TrackingRepository.gpsPoints.value) {
                    val obj = JSONObject().apply {
                        put("lat", point.lat)
                        put("lng", point.lng)
                        put("time", point.time)
                        put("speed", point.speed)
                    }
                    orgJsonArray.put(obj)
                }
                orgJsonArray.toString()
            } catch (_: Exception) {
                "[]"
            }

            val record = OrderRecord(
                tanggal = tanggalFormatted,
                hari = hariFormatted,
                jamMulai = jamMulaiVal,
                jamPickup = TrackingRepository.jamPickup.value,
                jamSelesai = now,
                jenisOrder = jenisOrder,
                pendapatanBersih = netIncome,
                pendapatanKotor = grossIncome,
                durasi = TrackingRepository.durationSeconds.value,
                jarakTempuh = TrackingRepository.totalJarak.value,
                jarakKePickup = TrackingRepository.jarakKePickup.value,
                jarakKeTujuan = TrackingRepository.jarakKeTujuan.value,
                latitudeAwal = TrackingRepository.latitudeAwal.value,
                longitudeAwal = TrackingRepository.longitudeAwal.value,
                alamatAwal = TrackingRepository.alamatAwal.value,
                latitudePickup = TrackingRepository.latitudePickup.value,
                longitudePickup = TrackingRepository.longitudePickup.value,
                alamatPickup = TrackingRepository.alamatPickup.value,
                latitudeAkhir = TrackingRepository.currentLocation.value?.latitude ?: 0.0,
                longitudeAkhir = TrackingRepository.currentLocation.value?.longitude ?: 0.0,
                alamatAkhir = _currentAddress.value,
                catatan = catatan,
                trackGpsJson = gpsJson,
                biayaBensin = if (appPreferences.isEstimasiBensinAktif.first()) biayaBensin else 0.0
            )

            orderDao.insertOrder(record)

            val intent = Intent(context, ServiceActionReceiver::class.java).apply {
                action = ServiceActionReceiver.ACTION_CANCEL_ORDER
            }
            context.sendBroadcast(intent)
            } catch (e: Exception) {
                failureMessage = e.message ?: "Gagal menyimpan pesanan. Silakan coba lagi."
            } finally {
                saveOrderMutex.unlock()
            }
            withContext(Dispatchers.Main) { onComplete(failureMessage) }
        }
    }

    fun deleteOrder(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            orderDao.deleteOrder(id)
        }
    }

    fun deleteAllOrders() {
        viewModelScope.launch(Dispatchers.IO) {
            orderDao.deleteAllOrders()
        }
    }

    fun updateOrder(order: OrderRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            orderDao.updateOrder(order)
        }
    }

    suspend fun exportJsonData(): String = withContext(Dispatchers.IO) {
        val orders = orderDao.getAllOrdersList()
        val jsonArray = JSONArray()
        for (order in orders) {
            val obj = JSONObject().apply {
                put("id", order.id)
                put("tanggal", order.tanggal)
                put("hari", order.hari)
                put("jamMulai", order.jamMulai)
                put("jamPickup", order.jamPickup)
                put("jamSelesai", order.jamSelesai)
                put("jenisOrder", order.jenisOrder)
                put("pendapatanBersih", order.pendapatanBersih)
                put("pendapatanKotor", order.pendapatanKotor)
                put("durasi", order.durasi)
                put("jarakTempuh", order.jarakTempuh)
                put("jarakKePickup", order.jarakKePickup)
                put("jarakKeTujuan", order.jarakKeTujuan)
                put("latitudeAwal", order.latitudeAwal)
                put("longitudeAwal", order.longitudeAwal)
                put("alamatAwal", order.alamatAwal)
                put("latitudePickup", order.latitudePickup)
                put("longitudePickup", order.longitudePickup)
                put("alamatPickup", order.alamatPickup)
                put("latitudeAkhir", order.latitudeAkhir)
                put("longitudeAkhir", order.longitudeAkhir)
                put("alamatAkhir", order.alamatAkhir)
                put("catatan", order.catatan)
                put("trackGpsJson", order.trackGpsJson)
                put("biayaBensin", order.biayaBensin)
            }
            jsonArray.put(obj)
        }
        jsonArray.toString(2)
    }

    suspend fun exportCsvData(): String = withContext(Dispatchers.IO) {
        val orders = orderDao.getAllOrdersList()
        val sb = StringBuilder()
        sb.append("ID,Tanggal,Hari,JamMulai,JamPickup,JamSelesai,JenisOrder,PendapatanBersih,PendapatanKotor,Durasi,JarakTempuh,JarakKePickup,JarakKeTujuan,AlamatAwal,AlamatPickup,AlamatAkhir,BiayaBensin,Catatan\n")
        for (o in orders) {
            sb.append(o.id).append(",")
                .append(o.tanggal).append(",")
                .append(o.hari).append(",")
                .append(o.jamMulai).append(",")
                .append(o.jamPickup).append(",")
                .append(o.jamSelesai).append(",")
                .append("\"").append(o.jenisOrder.replace("\"", "\"\"")).append("\",")
                .append(o.pendapatanBersih).append(",")
                .append(o.pendapatanKotor).append(",")
                .append(o.durasi).append(",")
                .append(o.jarakTempuh).append(",")
                .append(o.jarakKePickup).append(",")
                .append(o.jarakKeTujuan).append(",")
                .append("\"").append(o.alamatAwal.replace("\"", "\"\"")).append("\",")
                .append("\"").append(o.alamatPickup.replace("\"", "\"\"")).append("\",")
                .append("\"").append(o.alamatAkhir.replace("\"", "\"\"")).append("\",")
                .append(o.biayaBensin).append(",")
                .append("\"").append(o.catatan.replace("\"", "\"\"")).append("\"\n")
        }
        sb.toString()
    }

    suspend fun importJsonData(jsonString: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val currentOrders = orderDao.getAllOrdersList()
        val jsonArray = JSONArray(jsonString)
        var importedCount = 0
        var skippedCount = 0
        val newRecords = mutableListOf<OrderRecord>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val jamMulai = obj.optLong("jamMulai", 0L)
            val tanggal = obj.optString("tanggal", "")
            val pendapatanBersih = obj.optDouble("pendapatanBersih", 0.0)
            val jarakTempuh = obj.optDouble("jarakTempuh", 0.0)

            fun matchesExisting(existing: OrderRecord): Boolean {
                if (jamMulai > 0L && existing.jamMulai > 0L) {
                    return existing.jamMulai == jamMulai
                }
                return existing.tanggal == tanggal &&
                        existing.jamPickup == obj.optLong("jamPickup", 0L) &&
                        existing.jamSelesai == obj.optLong("jamSelesai", 0L) &&
                        existing.jenisOrder == obj.optString("jenisOrder", "Penumpang") &&
                        Math.abs(existing.pendapatanBersih - pendapatanBersih) < 0.01 &&
                        Math.abs(existing.jarakTempuh - jarakTempuh) < 0.01 &&
                        existing.latitudePickup == obj.optDouble("latitudePickup", 0.0) &&
                        existing.longitudePickup == obj.optDouble("longitudePickup", 0.0)
            }
            val isDuplicate = (currentOrders.asSequence() + newRecords.asSequence()).any(::matchesExisting)

            if (isDuplicate) {
                skippedCount++
            } else {
                val record = OrderRecord(
                    id = 0,
                    tanggal = tanggal,
                    hari = obj.optString("hari", ""),
                    jamMulai = jamMulai,
                    jamPickup = obj.optLong("jamPickup", 0L),
                    jamSelesai = obj.optLong("jamSelesai", 0L),
                    jenisOrder = obj.optString("jenisOrder", "Penumpang"),
                    pendapatanBersih = pendapatanBersih,
                    pendapatanKotor = obj.optDouble("pendapatanKotor", 0.0),
                    durasi = obj.optLong("durasi", 0L),
                    jarakTempuh = jarakTempuh,
                    jarakKePickup = obj.optDouble("jarakKePickup", 0.0),
                    jarakKeTujuan = obj.optDouble("jarakKeTujuan", 0.0),
                    latitudeAwal = obj.optDouble("latitudeAwal", 0.0),
                    longitudeAwal = obj.optDouble("longitudeAwal", 0.0),
                    alamatAwal = obj.optString("alamatAwal", ""),
                    latitudePickup = obj.optDouble("latitudePickup", 0.0),
                    longitudePickup = obj.optDouble("longitudePickup", 0.0),
                    alamatPickup = obj.optString("alamatPickup", ""),
                    latitudeAkhir = obj.optDouble("latitudeAkhir", 0.0),
                    longitudeAkhir = obj.optDouble("longitudeAkhir", 0.0),
                    alamatAkhir = obj.optString("alamatAkhir", ""),
                    catatan = obj.optString("catatan", ""),
                    trackGpsJson = obj.optString("trackGpsJson", "[]"),
                    biayaBensin = obj.optDouble("biayaBensin", 0.0)
                )
                newRecords.add(record)
                importedCount++
            }
        }

        if (newRecords.isNotEmpty()) {
            orderDao.insertOrders(newRecords)
        }

        Pair(importedCount, skippedCount)
    }
}

class MainViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val prefs = AppPreferences.getInstance(application)
            return MainViewModel(application, db.orderDao(), prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
