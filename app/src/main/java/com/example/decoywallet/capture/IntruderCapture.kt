package com.example.decoywallet.capture

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class DualCapture(
    val front: File?,
    val back: File?
)

/**
 * Silent dual-camera capture (front then rear). No PreviewView — no preview,
 * no shutter sound, no flash.
 */
object IntruderCapture {

    suspend fun captureBoth(context: Context, owner: LifecycleOwner): DualCapture =
        withContext(Dispatchers.Main) {
            val provider = runCatching {
                ProcessCameraProvider.getInstance(context).await()
            }.getOrNull() ?: return@withContext DualCapture(null, null)

            val front = captureOne(context, owner, provider, CameraSelector.DEFAULT_FRONT_CAMERA, "front")
            val back = captureOne(context, owner, provider, CameraSelector.DEFAULT_BACK_CAMERA, "back")
            runCatching { provider.unbindAll() }
            DualCapture(front, back)
        }

    private suspend fun captureOne(
        context: Context,
        owner: LifecycleOwner,
        provider: ProcessCameraProvider,
        selector: CameraSelector,
        label: String
    ): File? {
        val available = runCatching { provider.hasCamera(selector) }.getOrDefault(false)
        if (!available) return null
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
        return try {
            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, imageCapture)
            delay(700)
            val file = File(context.cacheDir, "intruder_${label}_${System.currentTimeMillis()}.jpg")
            val ok = takePicture(context, imageCapture, file)
            if (ok && file.exists() && file.length() > 0) file else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun takePicture(context: Context, capture: ImageCapture, file: File): Boolean =
        suspendCancellableCoroutine { cont ->
            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        if (cont.isActive) cont.resume(false)
                    }
                }
            )
        }
}
