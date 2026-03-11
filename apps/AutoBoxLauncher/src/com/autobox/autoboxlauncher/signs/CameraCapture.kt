package com.autobox.autoboxlauncher.signs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCapture(
    private val context: Context,
    private val onFrame: (Bitmap) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun start() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            val bitmap = imageProxy.toBitmapSafe()
            imageProxy.close()
            if (bitmap != null) onFrame(bitmap)
        }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                ProcessLifecycleOwner.get(),
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageAnalysis
            )
        } catch (e: Exception) {
            // Camera not available or permission denied
        }
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }

    fun release() {
        stop()
        executor.shutdown()
    }

    private fun ImageProxy.toBitmapSafe(): Bitmap? = try {
        val bmp = toBitmap()
        val degrees = imageInfo.rotationDegrees.toFloat()
        if (degrees == 0f) bmp
        else {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }
    } catch (_: Exception) {
        null
    }
}
