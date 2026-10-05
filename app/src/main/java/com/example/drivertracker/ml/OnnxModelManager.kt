package com.example.drivertracker.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.FloatBuffer

data class OnnxModelInfo(
    val fileName: String,
    val fileSizeFormatted: String,
    val inputCount: Int,
    val outputCount: Int,
    val isReady: Boolean
)

object OnnxModelManager {

    private const val MODEL_DIR_NAME = "models"
    private const val MODEL_FILE_NAME = "custom_driver_model.onnx"

    private var cachedSession: OrtSession? = null
    private var cachedSessionPath: String? = null

    private val ortEnv: OrtEnvironment? by lazy {
        runCatching { OrtEnvironment.getEnvironment() }.getOrNull()
    }

    private fun getModelFile(context: Context): File {
        val dir = File(context.filesDir, MODEL_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, MODEL_FILE_NAME)
    }

    fun hasCustomModel(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > 0
    }

    fun getModelInfo(context: Context, displayName: String = ""): OnnxModelInfo? {
        val file = getModelFile(context)
        if (!file.exists() || file.length() == 0L) return null

        val sizeKb = file.length() / 1024.0
        val sizeFormatted = if (sizeKb > 1024) {
            String.format(java.util.Locale.US, "%.1f MB", sizeKb / 1024.0)
        } else {
            String.format(java.util.Locale.US, "%.0f KB", sizeKb)
        }

        val session = getOrOpenSession(context)
        val inCount = session?.inputNames?.size ?: 1
        val outCount = session?.outputNames?.size ?: 1

        val finalName = displayName.ifBlank { file.name }
        return OnnxModelInfo(
            fileName = finalName,
            fileSizeFormatted = sizeFormatted,
            inputCount = inCount,
            outputCount = outCount,
            isReady = session != null
        )
    }

    @Synchronized
    fun saveModelFromUri(context: Context, uri: Uri, originalName: String): Result<OnnxModelInfo> {
        val env = ortEnv ?: return Result.failure(IllegalStateException("ONNX Runtime tidak didukung di perangkat ini."))
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Tidak dapat membaca file model dari perangkat.")

            if (bytes.size < 16) {
                throw IllegalArgumentException("File model terlalu kecil atau rusak.")
            }

            val testSession = env.createSession(bytes)
            val inCount = testSession.inputNames.size
            val outCount = testSession.outputNames.size
            testSession.close()

            val destFile = getModelFile(context)
            destFile.writeBytes(bytes)

            closeSession()

            val sizeKb = destFile.length() / 1024.0
            val sizeFormatted = if (sizeKb > 1024) {
                String.format(java.util.Locale.US, "%.1f MB", sizeKb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f KB", sizeKb)
            }

            OnnxModelInfo(
                fileName = originalName.ifBlank { MODEL_FILE_NAME },
                fileSizeFormatted = sizeFormatted,
                inputCount = inCount,
                outputCount = outCount,
                isReady = true
            )
        }
    }

    @Synchronized
    fun getOrOpenSession(context: Context): OrtSession? {
        val file = getModelFile(context)
        if (!file.exists() || file.length() == 0L) return null

        val env = ortEnv ?: return null
        if (cachedSession != null && cachedSessionPath == file.absolutePath) {
            return cachedSession
        }

        return runCatching {
            closeSession()
            val session = env.createSession(file.absolutePath)
            cachedSession = session
            cachedSessionPath = file.absolutePath
            session
        }.getOrNull()
    }

    @Synchronized
    fun deleteModel(context: Context): Boolean {
        closeSession()
        val file = getModelFile(context)
        return if (file.exists()) file.delete() else true
    }

    @Synchronized
    private fun closeSession() {
        runCatching { cachedSession?.close() }
        cachedSession = null
        cachedSessionPath = null
    }

    fun predictScore(
        context: Context,
        hour: Int,
        dayOfWeek: Int,
        lat: Double,
        lng: Double,
        distanceKm: Double,
        category: String,
        isWeekend: Boolean,
        orderCount: Int
    ): Int? {
        val env = ortEnv ?: return null
        val session = getOrOpenSession(context) ?: return null

        return runCatching {
            val inputName = session.inputNames.firstOrNull() ?: return null
            val inputInfo = session.inputInfo[inputName]?.info

            val catCode = when (category.lowercase()) {
                "penumpang" -> 0f
                "makanan" -> 1f
                "paket" -> 2f
                else -> 0f
            }

            val fullFeatures = floatArrayOf(
                hour.toFloat(),
                dayOfWeek.toFloat(),
                lat.toFloat(),
                lng.toFloat(),
                distanceKm.toFloat(),
                catCode,
                if (isWeekend) 1f else 0f,
                orderCount.toFloat()
            )

            val tensorInfo = inputInfo as? ai.onnxruntime.TensorInfo
            val shape = tensorInfo?.shape
            val numFeatures = if (shape != null && shape.isNotEmpty()) {
                val lastDim = shape.last()
                if (lastDim > 0) lastDim.toInt() else fullFeatures.size
            } else {
                fullFeatures.size
            }

            val truncatedFeatures = if (numFeatures <= fullFeatures.size) {
                fullFeatures.copyOf(numFeatures)
            } else {
                FloatArray(numFeatures) { idx ->
                    if (idx < fullFeatures.size) fullFeatures[idx] else 0f
                }
            }

            val inputShape = longArrayOf(1, truncatedFeatures.size.toLong())
            val buffer = FloatBuffer.wrap(truncatedFeatures)
            val tensor = OnnxTensor.createTensor(env, buffer, inputShape)

            val results = session.run(mapOf(inputName to tensor))
            val outputName = session.outputNames.firstOrNull() ?: return null
            val outputValue = results[outputName]?.get()?.value

            val rawScore: Float = when (outputValue) {
                is Array<*> -> {
                    val firstRow = outputValue.firstOrNull()
                    when (firstRow) {
                        is FloatArray -> firstRow.maxOrNull() ?: firstRow.firstOrNull() ?: 0.5f
                        is DoubleArray -> (firstRow.maxOrNull() ?: firstRow.firstOrNull() ?: 0.5).toFloat()
                        else -> 0.5f
                    }
                }
                is FloatArray -> outputValue.maxOrNull() ?: outputValue.firstOrNull() ?: 0.5f
                is DoubleArray -> (outputValue.maxOrNull() ?: outputValue.firstOrNull() ?: 0.5).toFloat()
                is Float -> outputValue
                is Double -> outputValue.toFloat()
                else -> 0.5f
            }

            tensor.close()
            results.close()

            val score = if (rawScore in 0.0f..1.0f) {
                (50 + rawScore * 49).toInt()
            } else {
                rawScore.toInt().coerceIn(50, 99)
            }
            score
        }.getOrNull()
    }
}
