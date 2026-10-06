package com.example.drivertracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_TRACKING_INTERVAL_SEC = intPreferencesKey("tracking_interval_sec")
        val KEY_KONSUMSI_BBM = floatPreferencesKey("konsumsi_bbm")
        val KEY_HARGA_BENSIN = doublePreferencesKey("harga_bensin")
        val KEY_IS_KOMISI_AKTIF = booleanPreferencesKey("is_komisi_aktif")
        val KEY_PERSEN_KOMISI = floatPreferencesKey("persen_komisi")
        val KEY_IS_ESTIMASI_BENSIN_AKTIF = booleanPreferencesKey("is_estimasi_bensin_aktif")
        val KEY_DRIVER_NAME = stringPreferencesKey("driver_name")
        val KEY_RADAR_SHOW_HISTORY = booleanPreferencesKey("radar_show_history")
        val KEY_RADAR_SHOW_AI = booleanPreferencesKey("radar_show_ai")
        val KEY_CUSTOM_ONNX_MODEL_NAME = stringPreferencesKey("custom_onnx_model_name")
        val KEY_CUSTOM_ONNX_MODEL_PATH = stringPreferencesKey("custom_onnx_model_path")

        val KEY_POSTER_SHOW_APP_NAME = booleanPreferencesKey("poster_show_app_name")
        val KEY_POSTER_SHOW_DRIVER_NAME = booleanPreferencesKey("poster_show_driver_name")
        val KEY_POSTER_SHOW_DISTANCE = booleanPreferencesKey("poster_show_distance")
        val KEY_POSTER_SHOW_INCOME = booleanPreferencesKey("poster_show_income")
        val KEY_POSTER_SHOW_ROUTE_LINE = booleanPreferencesKey("poster_show_route_line")
        val KEY_POSTER_SHOW_DURATION = booleanPreferencesKey("poster_show_duration")
        val KEY_POSTER_SHOW_AVG_SPEED = booleanPreferencesKey("poster_show_avg_speed")
        val KEY_POSTER_SHOW_MAX_SPEED = booleanPreferencesKey("poster_show_max_speed")
        val KEY_POSTER_ROUTE_LINE_COLOR = stringPreferencesKey("poster_route_line_color")
        val KEY_POSTER_TEXT_OUTLINE_ENABLED = booleanPreferencesKey("poster_text_outline_enabled")
        val KEY_POSTER_TEXT_OUTLINE_COLOR = stringPreferencesKey("poster_text_outline_color")
        val KEY_POSTER_TEXT_OUTLINE_SIZE = floatPreferencesKey("poster_text_outline_size")
        val KEY_POSTER_TEXT_SHADOW_ENABLED = booleanPreferencesKey("poster_text_shadow_enabled")
        val KEY_POSTER_TEXT_SHADOW_COLOR = stringPreferencesKey("poster_text_shadow_color")
        val KEY_POSTER_TEXT_SHADOW_SIZE = floatPreferencesKey("poster_text_shadow_size")
        val KEY_POSTER_ROUTE_OUTLINE_ENABLED = booleanPreferencesKey("poster_route_outline_enabled")
        val KEY_POSTER_ROUTE_OUTLINE_COLOR = stringPreferencesKey("poster_route_outline_color")
        val KEY_POSTER_ROUTE_OUTLINE_SIZE = floatPreferencesKey("poster_route_outline_size")
        val KEY_POSTER_ROUTE_SHADOW_ENABLED = booleanPreferencesKey("poster_route_shadow_enabled")
        val KEY_POSTER_ROUTE_SHADOW_COLOR = stringPreferencesKey("poster_route_shadow_color")
        val KEY_POSTER_ROUTE_SHADOW_SIZE = floatPreferencesKey("poster_route_shadow_size")

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_DARK_MODE] ?: true
    }

    val trackingIntervalSec: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_TRACKING_INTERVAL_SEC] ?: 5
    }

    val konsumsiBbm: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_KONSUMSI_BBM] ?: 45.0f
    }

    val hargaBensin: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[KEY_HARGA_BENSIN] ?: 10000.0
    }

    val isKomisiAktif: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_KOMISI_AKTIF] ?: false
    }

    val persenKomisi: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_PERSEN_KOMISI] ?: 20.0f
    }

    val isEstimasiBensinAktif: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_ESTIMASI_BENSIN_AKTIF] ?: true
    }

    val driverName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DRIVER_NAME] ?: ""
    }

    val radarShowHistory: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RADAR_SHOW_HISTORY] ?: true
    }

    val radarShowAi: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_RADAR_SHOW_AI] ?: true
    }

    val customOnnxModelName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_ONNX_MODEL_NAME] ?: ""
    }

    val customOnnxModelPath: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_ONNX_MODEL_PATH] ?: ""
    }

    val posterShowAppName: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_APP_NAME] ?: true
    }

    val posterShowDriverName: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_DRIVER_NAME] ?: true
    }

    val posterShowDistance: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_DISTANCE] ?: true
    }

    val posterShowIncome: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_INCOME] ?: true
    }

    val posterShowRouteLine: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_ROUTE_LINE] ?: true
    }

    val posterShowDuration: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_DURATION] ?: false
    }

    val posterShowAvgSpeed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_AVG_SPEED] ?: false
    }

    val posterShowMaxSpeed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_SHOW_MAX_SPEED] ?: false
    }

    val posterRouteLineColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_POSTER_ROUTE_LINE_COLOR] ?: "AUTO"
    }

    val posterTextOutlineEnabled = context.dataStore.data.map { it[KEY_POSTER_TEXT_OUTLINE_ENABLED] ?: false }
    val posterTextOutlineColor = context.dataStore.data.map { it[KEY_POSTER_TEXT_OUTLINE_COLOR] ?: "#000000" }
    val posterTextOutlineSize = context.dataStore.data.map { it[KEY_POSTER_TEXT_OUTLINE_SIZE] ?: 1.5f }
    val posterTextShadowEnabled = context.dataStore.data.map { it[KEY_POSTER_TEXT_SHADOW_ENABLED] ?: true }
    val posterTextShadowColor = context.dataStore.data.map { it[KEY_POSTER_TEXT_SHADOW_COLOR] ?: "#000000" }
    val posterTextShadowSize = context.dataStore.data.map { it[KEY_POSTER_TEXT_SHADOW_SIZE] ?: 2f }
    val posterRouteOutlineEnabled = context.dataStore.data.map { it[KEY_POSTER_ROUTE_OUTLINE_ENABLED] ?: false }
    val posterRouteOutlineColor = context.dataStore.data.map { it[KEY_POSTER_ROUTE_OUTLINE_COLOR] ?: "#FFFFFF" }
    val posterRouteOutlineSize = context.dataStore.data.map { it[KEY_POSTER_ROUTE_OUTLINE_SIZE] ?: 2f }
    val posterRouteShadowEnabled = context.dataStore.data.map { it[KEY_POSTER_ROUTE_SHADOW_ENABLED] ?: true }
    val posterRouteShadowColor = context.dataStore.data.map { it[KEY_POSTER_ROUTE_SHADOW_COLOR] ?: "#000000" }
    val posterRouteShadowSize = context.dataStore.data.map { it[KEY_POSTER_ROUTE_SHADOW_SIZE] ?: 2f }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_MODE] = enabled
        }
    }

    suspend fun setTrackingIntervalSec(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TRACKING_INTERVAL_SEC] = seconds
        }
    }

    suspend fun setKonsumsiBbm(kmPerLiter: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KONSUMSI_BBM] = kmPerLiter
        }
    }

    suspend fun setHargaBensin(rupiah: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HARGA_BENSIN] = rupiah
        }
    }

    suspend fun setKomisiAktif(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_KOMISI_AKTIF] = enabled
        }
    }

    suspend fun setPersenKomisi(percent: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PERSEN_KOMISI] = percent
        }
    }

    suspend fun setEstimasiBensinAktif(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_ESTIMASI_BENSIN_AKTIF] = enabled
        }
    }

    suspend fun setDriverName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DRIVER_NAME] = name
        }
    }

    suspend fun setRadarShowHistory(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RADAR_SHOW_HISTORY] = show
        }
    }

    suspend fun setRadarShowAi(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RADAR_SHOW_AI] = show
        }
    }

    suspend fun setCustomOnnxModel(name: String, path: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_ONNX_MODEL_NAME] = name
            preferences[KEY_CUSTOM_ONNX_MODEL_PATH] = path
        }
    }

    suspend fun clearCustomOnnxModel() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_CUSTOM_ONNX_MODEL_NAME)
            preferences.remove(KEY_CUSTOM_ONNX_MODEL_PATH)
        }
    }

    suspend fun setPosterShowAppName(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_APP_NAME] = show
        }
    }

    suspend fun setPosterShowDriverName(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_DRIVER_NAME] = show
        }
    }

    suspend fun setPosterShowDistance(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_DISTANCE] = show
        }
    }

    suspend fun setPosterShowIncome(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_INCOME] = show
        }
    }

    suspend fun setPosterShowRouteLine(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_ROUTE_LINE] = show
        }
    }

    suspend fun setPosterShowDuration(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_DURATION] = show
        }
    }

    suspend fun setPosterShowAvgSpeed(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_AVG_SPEED] = show
        }
    }

    suspend fun setPosterShowMaxSpeed(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_SHOW_MAX_SPEED] = show
        }
    }

    suspend fun setPosterRouteLineColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_POSTER_ROUTE_LINE_COLOR] = color
        }
    }

    suspend fun setPosterTextOutlineEnabled(value: Boolean) = context.dataStore.edit { it[KEY_POSTER_TEXT_OUTLINE_ENABLED] = value }
    suspend fun setPosterTextOutlineColor(value: String) = context.dataStore.edit { it[KEY_POSTER_TEXT_OUTLINE_COLOR] = value }
    suspend fun setPosterTextOutlineSize(value: Float) = context.dataStore.edit { it[KEY_POSTER_TEXT_OUTLINE_SIZE] = value }
    suspend fun setPosterTextShadowEnabled(value: Boolean) = context.dataStore.edit { it[KEY_POSTER_TEXT_SHADOW_ENABLED] = value }
    suspend fun setPosterTextShadowColor(value: String) = context.dataStore.edit { it[KEY_POSTER_TEXT_SHADOW_COLOR] = value }
    suspend fun setPosterTextShadowSize(value: Float) = context.dataStore.edit { it[KEY_POSTER_TEXT_SHADOW_SIZE] = value }
    suspend fun setPosterRouteOutlineEnabled(value: Boolean) = context.dataStore.edit { it[KEY_POSTER_ROUTE_OUTLINE_ENABLED] = value }
    suspend fun setPosterRouteOutlineColor(value: String) = context.dataStore.edit { it[KEY_POSTER_ROUTE_OUTLINE_COLOR] = value }
    suspend fun setPosterRouteOutlineSize(value: Float) = context.dataStore.edit { it[KEY_POSTER_ROUTE_OUTLINE_SIZE] = value }
    suspend fun setPosterRouteShadowEnabled(value: Boolean) = context.dataStore.edit { it[KEY_POSTER_ROUTE_SHADOW_ENABLED] = value }
    suspend fun setPosterRouteShadowColor(value: String) = context.dataStore.edit { it[KEY_POSTER_ROUTE_SHADOW_COLOR] = value }
    suspend fun setPosterRouteShadowSize(value: Float) = context.dataStore.edit { it[KEY_POSTER_ROUTE_SHADOW_SIZE] = value }
}
