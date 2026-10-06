package com.example.drivertracker.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class RadarPrediction(
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val predictedOrders: Float,
    val forecastTime: LocalDateTime
)

object RadarOnnxManager {
    private const val MODEL_FILE_NAME = "radar_model.onnx"
    private const val INPUT_FEATURE_COUNT = 5
    private const val GRID_SPACING_KM = 1.0
    private const val MAX_CANDIDATE_POINTS = 160
    private const val MAX_RESULT_PINS = 30

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    @Volatile private var cachedSession: OrtSession? = null
    @Volatile private var cachedPath: String? = null

    private fun modelFile(context: Context) = File(context.filesDir, MODEL_FILE_NAME)

    fun hasModel(context: Context): Boolean = modelFile(context).let { it.exists() && it.length() > 0L }

    @Synchronized
    fun importModel(context: Context, uri: Uri): Result<Unit> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("File model tidak bisa dibaca.")
        require(bytes.size >= 16) { "File terlalu kecil untuk menjadi model ONNX." }
        val testSession = env.createSession(bytes)
        try {
            require(validate(testSession) == null) {
                validate(testSession) ?: "Format model tidak sesuai."
            }
        } finally {
            testSession.close()
        }
        closeSession()
        val target = modelFile(context)
        val temporary = File(context.filesDir, "$MODEL_FILE_NAME.tmp")
        temporary.writeBytes(bytes)
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    @Synchronized
    fun deleteModel(context: Context): Boolean {
        closeSession()
        val file = modelFile(context)
        return !file.exists() || file.delete()
    }

    fun predictAround(
        context: Context,
        latitude: Double,
        longitude: Double,
        forecastTime: LocalDateTime,
        categories: List<String>,
        radiusKm: Double = 5.0
    ): List<RadarPrediction> {
        if (!latitude.isFinite() || !longitude.isFinite() || categories.isEmpty() || radiusKm <= 0.0) return emptyList()
        val session = openSession(context) ?: return emptyList()
        return runCatching {
            val points = candidatePoints(latitude, longitude, radiusKm)
            val serviceTypes = categories.mapNotNull { name ->
                when (name) {
                    "Penumpang" -> name to 0f
                    "Makanan" -> name to 1f
                    "Paket" -> name to 2f
                    else -> null
                }
            }
            val rows = ArrayList<FloatArray>(points.size * serviceTypes.size)
            val metadata = ArrayList<Pair<Pair<Double, Double>, String>>(points.size * serviceTypes.size)
            points.forEach { point ->
                serviceTypes.forEach { (category, code) ->
                    rows += floatArrayOf(
                        point.first.toFloat(),
                        point.second.toFloat(),
                        forecastTime.dayOfWeek.value.toFloat(),
                        forecastTime.hour.toFloat(),
                        code
                    )
                    metadata += point to category
                }
            }
            infer(session, rows).mapIndexedNotNull { index, value ->
                if (!value.isFinite() || value <= 0f) return@mapIndexedNotNull null
                val item = metadata.getOrNull(index) ?: return@mapIndexedNotNull null
                RadarPrediction(item.first.first, item.first.second, item.second, value, forecastTime)
            }
                .sortedByDescending { it.predictedOrders }
                .distinctBy { it.latitude to it.longitude }
                .take(MAX_RESULT_PINS)
        }.getOrDefault(emptyList())
    }

    @Synchronized
    private fun openSession(context: Context): OrtSession? {
        val file = modelFile(context)
        if (!file.exists() || file.length() == 0L) return null
        if (cachedSession != null && cachedPath == file.absolutePath) return cachedSession
        return runCatching {
            closeSession()
            env.createSession(file.absolutePath).also { session ->
                val validationError = validate(session)
                if (validationError != null) {
                    session.close()
                    throw IllegalArgumentException(validationError)
                }
                cachedSession = session
                cachedPath = file.absolutePath
            }
        }.getOrNull()
    }

    @Synchronized
    private fun closeSession() {
        runCatching { cachedSession?.close() }
        cachedSession = null
        cachedPath = null
    }

    private fun validate(session: OrtSession): String? {
        val inputName = session.inputNames.singleOrNull() ?: return "Model harus memiliki satu input."
        val input = session.inputInfo[inputName]?.info as? ai.onnxruntime.TensorInfo
            ?: return "Input model harus berupa tensor float."
        if (input.type != ai.onnxruntime.OnnxJavaType.FLOAT || input.shape.size != 2 ||
            input.shape[0] !in setOf(-1L, 1L) || input.shape[1] != INPUT_FEATURE_COUNT.toLong()) {
            return "Input model harus float [batch, 5]: latitude, longitude, hari, jam, jenis layanan."
        }
        val outputName = session.outputNames.singleOrNull() ?: return "Model harus memiliki satu output."
        val output = session.outputInfo[outputName]?.info as? ai.onnxruntime.TensorInfo
            ?: return "Output model harus berupa tensor float."
        if (output.type != ai.onnxruntime.OnnxJavaType.FLOAT || output.shape.size !in 1..2 ||
            output.shape.last() !in setOf(-1L, 1L)) {
            return "Output model harus float [batch] atau [batch, 1] berupa perkiraan jumlah order."
        }
        return null
    }

    private fun infer(session: OrtSession, rows: List<FloatArray>): List<Float> {
        if (rows.isEmpty()) return emptyList()
        val inputName = session.inputNames.single()
        val outputName = session.outputNames.single()
        val info = session.inputInfo[inputName]?.info as ai.onnxruntime.TensorInfo
        val batchLimit = info.shape[0].takeIf { it > 0 }?.toInt() ?: rows.size
        return rows.chunked(batchLimit).flatMap { batch ->
            val values = FloatArray(batch.size * INPUT_FEATURE_COUNT)
            batch.forEachIndexed { index, row -> row.copyInto(values, index * INPUT_FEATURE_COUNT) }
            val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(values), longArrayOf(batch.size.toLong(), INPUT_FEATURE_COUNT.toLong()))
            tensor.use { input ->
                session.run(mapOf(inputName to input)).use { result ->
                    result[outputName]?.get()?.value?.let { flatten(it).take(batch.size) }.orEmpty()
                }
            }
        }
    }

    private fun flatten(value: Any): List<Float> = when (value) {
        is FloatArray -> value.toList()
        is Array<*> -> value.flatMap { child -> if (child == null) emptyList() else flatten(child) }
        else -> emptyList()
    }

    private fun candidatePoints(latitude: Double, longitude: Double, radiusKm: Double): List<Pair<Double, Double>> {
        val steps = ceil(radiusKm / GRID_SPACING_KM).toInt()
        val longitudeScale = cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
        return buildList {
            for (north in -steps..steps) {
                for (east in -steps..steps) {
                    if (north == 0 && east == 0) continue
                    if (hypot(north.toDouble() * GRID_SPACING_KM, east.toDouble() * GRID_SPACING_KM) > radiusKm) continue
                    val pointLat = latitude + north * GRID_SPACING_KM / 111.32
                    val pointLon = longitude + east * GRID_SPACING_KM / (111.32 * longitudeScale)
                    add(pointLat to pointLon)
                }
            }
        }.take(MAX_CANDIDATE_POINTS)
    }
}
