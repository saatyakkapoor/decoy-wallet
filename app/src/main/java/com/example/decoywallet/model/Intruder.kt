package com.example.decoywallet.model

/**
 * One intruder alert. Populated by the capture pipeline (front camera, fused
 * location, Build info) on a wrong PIN and stored in Firestore; the owner's
 * Security Center reads these back live.
 */
data class Intruder(
    val id: String = "",
    val timestampMillis: Long = 0L,
    val title: String = "Unauthorized access",
    val timestamp: String = "",
    val location: String = "",
    val coordinates: String = "",
    val device: String = "",
    val failedAttempts: Int = 1,
    val alertChannel: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Downscaled JPEG of the intruder, base64-encoded (stored inline in Firestore). */
    val photoBase64: String? = null
)
