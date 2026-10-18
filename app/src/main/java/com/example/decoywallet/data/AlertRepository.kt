package com.example.decoywallet.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.decoywallet.BuildConfig
import com.example.decoywallet.alert.EmailSender
import com.example.decoywallet.capture.CapturedLocation
import com.example.decoywallet.capture.DualCapture
import com.example.decoywallet.capture.LocationHelper
import com.example.decoywallet.model.Intruder
import com.example.decoywallet.prefs.SettingsStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max

object AlertRepository {

    private const val TAG = "AlertRepository"
    private const val COLLECTION = "intruder_alerts"
    private const val MAX_PHOTO_DIM = 600
    private const val JPEG_QUALITY = 50

    private val attemptCounter = AtomicInteger(0)

    fun resetAttempts() = attemptCounter.set(0)

    private val firestore get() = FirebaseFirestore.getInstance()

    /** Primary alert: cameras + GPS. Emails immediately from Decoy Wallet <decoy@paperpool.in>. */
    suspend fun recordIntruder(
        context: Context,
        deviceId: String,
        cameras: DualCapture?,
        location: CapturedLocation?
    ): Boolean {
        val attempts = attemptCounter.incrementAndGet()
        val now = Date()
        val timestampText = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.ENGLISH).format(now)
        val to = resolveTo(context)

        val emailOk = withContext(Dispatchers.IO) {
            sendCameraEmail(to, timestampText, attempts, cameras, location)
        }
        Log.i(TAG, "camera emailOk=$emailOk to=$to from=decoy@paperpool.in")

        runCatching {
            ensureSignedIn()
            val photoBase64 = cameras?.front?.let { runCatching { encodePhoto(it) }.getOrNull() }
                ?: cameras?.back?.let { runCatching { encodePhoto(it) }.getOrNull() }
            firestore.collection(COLLECTION).add(
                hashMapOf(
                    "deviceId" to deviceId,
                    "title" to "Unauthorized access",
                    "timestampMillis" to now.time,
                    "timestampText" to timestampText,
                    "location" to (location?.address ?: "Location unavailable"),
                    "coordinates" to (location?.let { formatCoordinates(it) } ?: "—"),
                    "latitude" to location?.latitude,
                    "longitude" to location?.longitude,
                    "accuracyMeters" to location?.accuracyMeters,
                    "device" to LocationHelper.deviceInfo(),
                    "failedAttempts" to attempts,
                    "alertChannel" to "Email",
                    "photoBase64" to photoBase64,
                    "emailSent" to emailOk
                )
            ).await()
        }.onFailure { Log.e(TAG, "Firestore write failed", it) }

        return emailOk
    }

    private fun resolveTo(context: Context): String =
        SettingsStore(context).alertEmail.ifBlank { BuildConfig.ALERT_EMAIL }.trim()

    private fun sendCameraEmail(
        to: String,
        timestampText: String,
        attempts: Int,
        cameras: DualCapture?,
        location: CapturedLocation?
    ): Boolean {
        if (to.isBlank()) {
            Log.e(TAG, "No alert email — set Settings → Alert email")
            return false
        }
        val mapsLink = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else "n/a"
        val body = buildString {
            appendLine("UNAUTHORIZED ACCESS DETECTED")
            appendLine()
            appendLine("Time: $timestampText")
            appendLine("Location: ${location?.address ?: "Location unavailable"}")
            appendLine("Coordinates: ${location?.let { formatCoordinates(it) } ?: "—"}")
            if (location?.accuracyMeters != null) {
                appendLine("GPS accuracy: ±${location.accuracyMeters.toInt()} m")
            }
            appendLine("Maps: $mapsLink")
            appendLine("Device: ${LocationHelper.deviceInfo()}")
            appendLine("Failed attempts: $attempts")
            appendLine()
            appendLine("Attachments: front_camera.jpg / back_camera.jpg")
        }
        val cameraFiles = buildList {
            cameras?.front?.takeIf { it.exists() }?.let { f ->
                add(f.copyTo(File(f.parentFile, "front_camera.jpg"), overwrite = true))
            }
            cameras?.back?.takeIf { it.exists() }?.let { f ->
                add(f.copyTo(File(f.parentFile, "back_camera.jpg"), overwrite = true))
            }
        }
        return EmailSender.sendIntruderAlert(
            toEmail = to,
            subject = "Decoy Wallet ALERT · Intruder · ${LocationHelper.deviceInfo()}",
            body = body,
            attachments = cameraFiles
        )
    }

    /** Live alerts for this device only. Sorted client-side to avoid a composite index. */
    fun listen(deviceId: String, onChange: (List<Intruder>) -> Unit): ListenerRegistration =
        firestore.collection(COLLECTION)
            .whereEqualTo("deviceId", deviceId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val items = snapshot.documents
                        .map { it.toIntruder() }
                        .sortedByDescending { it.timestampMillis }
                    onChange(items)
                }
            }

    suspend fun getById(id: String): Intruder? = runCatching {
        val doc = firestore.collection(COLLECTION).document(id).get().await()
        if (doc.exists()) doc.toIntruder() else null
    }.getOrNull()

    private suspend fun ensureSignedIn() {
        runCatching {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) auth.signInAnonymously().await()
        }
    }

    private fun encodePhoto(file: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_DIM)
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        bitmap.recycle()
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / 2 >= maxDim) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun formatCoordinates(loc: CapturedLocation): String {
        val latDir = if (loc.latitude >= 0) "N" else "S"
        val lngDir = if (loc.longitude >= 0) "E" else "W"
        return "%.5f° %s, %.5f° %s".format(
            abs(loc.latitude), latDir, abs(loc.longitude), lngDir
        )
    }

    private fun DocumentSnapshot.toIntruder(): Intruder = Intruder(
        id = id,
        timestampMillis = getLong("timestampMillis") ?: 0L,
        title = getString("title") ?: "Unauthorized access",
        timestamp = getString("timestampText") ?: "",
        location = getString("location") ?: "",
        coordinates = getString("coordinates") ?: "—",
        device = getString("device") ?: "Unknown device",
        failedAttempts = (getLong("failedAttempts") ?: 1L).toInt(),
        alertChannel = getString("alertChannel") ?: "Email",
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
        photoBase64 = getString("photoBase64")
    )
}
