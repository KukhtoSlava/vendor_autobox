package com.polishsigns

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Classification result for a single sign.
 *
 * @param label      sign label with Polish code and description
 * @param confidence model confidence in [0.0, 1.0]
 */
data class SignResult(
    val label: SignLabel,
    val confidence: Float,
)

/**
 * Polish road sign classifier backed by a TFLite model (EfficientNet-B0, 43 classes).
 *
 * Usage:
 * ```kotlin
 * val classifier = SignClassifier(context)
 * val result = classifier.classify(bitmap)
 * println("${result.label.polishCode}: ${result.label.description} (${result.confidence})")
 * classifier.close()
 * ```
 *
 * Or via use:
 * ```kotlin
 * SignClassifier(context).use { result = it.classify(bitmap) }
 * ```
 */
class SignClassifier(context: Context) : Closeable {

    private val interpreter: Interpreter
    private val inputBuffer: ByteBuffer

    init {
        val model = loadModelFile(context)
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(model, options)

        // Input buffer: 1 x H x W x 3 x 4 bytes (float32)
        inputBuffer = ByteBuffer
            .allocateDirect(1 * IMG_SIZE * IMG_SIZE * 3 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
    }

    /**
     * Classifies the sign in the given Bitmap.
     *
     * The bitmap should contain a cropped sign region. Any size is accepted —
     * it will be scaled to [IMG_SIZE]x[IMG_SIZE] internally.
     */
    fun classify(bitmap: Bitmap): SignResult {
        val resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
        fillBuffer(resized)

        // Output: [1, 43] float32
        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(inputBuffer, output)

        val scores = output[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
        return SignResult(
            label = SignLabels.ALL[maxIdx],
            confidence = scores[maxIdx],
        )
    }

    /**
     * Returns the top-N results sorted by descending confidence.
     * Useful for displaying alternatives or computing confidence margin.
     */
    fun classifyTopN(bitmap: Bitmap, n: Int = 3): List<SignResult> {
        val resized = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
        fillBuffer(resized)

        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(inputBuffer, output)

        return output[0]
            .mapIndexed { idx, score -> SignResult(SignLabels.ALL[idx], score) }
            .sortedByDescending { it.confidence }
            .take(n)
    }

    // NHWC: [batch=1, height, width, channels=3]
    private fun fillBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        for (y in 0 until IMG_SIZE) {
            for (x in 0 until IMG_SIZE) {
                val pixel = bitmap.getPixel(x, y)
                inputBuffer.putFloat(((Color.red(pixel)   / 255f) - MEAN[0]) / STD[0])
                inputBuffer.putFloat(((Color.green(pixel) / 255f) - MEAN[1]) / STD[1])
                inputBuffer.putFloat(((Color.blue(pixel)  / 255f) - MEAN[2]) / STD[2])
            }
        }
    }

    override fun close() = interpreter.close()

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_FILE)
        return FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    companion object {
        private const val MODEL_FILE  = "sign_classifier.tflite"
        private const val IMG_SIZE    = 128
        private const val NUM_CLASSES = 43

        // ImageNet normalization constants (same values used during training)
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD  = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
