package com.gspoof.pro

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Single owner of the [MockEngine] instance, shared between the
 * foreground service (which keeps the process alive) and the UI.
 */
object SpoofBridge {

    private var engine: MockEngine? = null

    val running = MutableStateFlow(false)
    val lastLat = MutableStateFlow<Double?>(null)
    val lastLon = MutableStateFlow<Double?>(null)
    val lastSpeedKmh = MutableStateFlow(0f)
    val lastAccuracy = MutableStateFlow(0f)
    val lastError = MutableStateFlow<String?>(null)

    fun engine(context: Context): MockEngine =
        engine ?: MockEngine(context.applicationContext).also { engine = it }

    fun start(
        context: Context,
        lat: Double,
        lon: Double,
        speedMps: Float,
        bearing: Float,
        simulate: Boolean
    ): Boolean {
        val e = engine(context)
        if (!e.isMockAllowed()) {
            running.value = false
            return false
        }
        lastLat.value = lat
        lastLon.value = lon
        running.value = true
        e.start(lat, lon, speedMps, bearing, simulate) { loc ->
            lastLat.value = loc.latitude
            lastLon.value = loc.longitude
            lastSpeedKmh.value = loc.speed * 3.6f
            lastAccuracy.value = loc.accuracy
            lastError.value = e.lastError
        }
        return true
    }

    fun moveTo(lat: Double, lon: Double) {
        engine?.moveTo(lat, lon)
        lastLat.value = lat
        lastLon.value = lon
    }

    fun stop() {
        engine?.stop()
        running.value = false
        lastSpeedKmh.value = 0f
    }
}
