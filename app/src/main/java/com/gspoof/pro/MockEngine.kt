package com.gspoof.pro

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Drives Android's mock-location pipeline for BOTH GPS and NETWORK providers.
 *
 * Requires the user to pick this app under
 * Settings → Developer options → "Select mock location app".
 *
 * Movement modes:
 *  - manual  : walks along [bearingDeg] at [speedMps]
 *  - route   : walks the waypoints in [route] in order, at [speedMps]
 * Both can be switched live while the service is running.
 */
class MockEngine(private val context: Context) {

    private val lm =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var job: Job? = null
    private val injected = mutableSetOf<String>()

    @Volatile private var curLat = 0.0
    @Volatile private var curLon = 0.0

    // Live controls — the UI can change these while the loop is running
    @Volatile var speedMps: Float = 0f
    @Volatile var bearingDeg: Float = 0f
    @Volatile var simulate: Boolean = false
    @Volatile var jitter: Boolean = false
    @Volatile var route: List<Pair<Double, Double>> = emptyList()
    @Volatile var routeLoop: Boolean = false
    @Volatile var routeIdx: Int = 0

    /** True when this app is the selected mock-location provider. */
    fun isMockAllowed(): Boolean = mockOpMode() == AppOpsManager.MODE_ALLOWED

    /** Raw AppOps mode for OP_MOCK_LOCATION: 0=default, 3=allowed, 1=ignored, 2=errored */
    fun mockOpMode(): Int = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), context.packageName
            )
        }
    } catch (t: Throwable) {
        -1
    }

    val isRunning: Boolean get() = job?.isActive == true

    /** Live position override (used by the nudge pad). */
    fun moveTo(lat: Double, lon: Double) {
        curLat = lat
        curLon = lon
    }

    fun currentPosition(): Pair<Double, Double> = curLat to curLon

    /** Replace the waypoint list; playback restarts from the first point. */
    fun setRoute(points: List<Pair<Double, Double>>, loop: Boolean = false) {
        route = points
        routeLoop = loop
        routeIdx = 0
    }

    fun clearRoute() {
        route = emptyList()
        routeIdx = 0
    }

    /** Live update of speed / bearing / movement flag without restarting. */
    fun updateControls(speedMps: Float, bearingDeg: Float, simulateMovement: Boolean) {
        this.speedMps = speedMps
        this.bearingDeg = bearingDeg
        this.simulate = simulateMovement
    }

    /**
     * Realistic mode (v2.1): variasi akurasi, interval fix, jitter, altitude,
     * dan noise akurasi. Tujuannya menghilangkan sidik jari "konstan":
     * akurasi 3.0 m + interval tepat 1000 ms adalah pola yang trivial dicari.
     */
    var realistic: Boolean = true

    @Volatile
    var lastAccuracy: Float = 0f
        private set

    @Volatile
    var lastError: String? = null
        private set

    private fun logErr(what: String, e: Throwable) {
        lastError = "$what: ${e.javaClass.simpleName}: ${e.message}"
    }

    @SuppressLint("MissingPermission")
    fun start(
        lat: Double,
        lon: Double,
        speedMps: Float,
        bearingDeg: Float,
        simulateMovement: Boolean,
        onTick: (Location) -> Unit
    ) {
        stop()
        ensureProviders()
        try {
            fused.setMockMode(true)
            lastError = null
        } catch (e: Throwable) {
            logErr("setMockMode(true)", e)
        }

        curLat = lat
        curLon = lon
        this.speedMps = speedMps
        this.bearingDeg = bearingDeg
        this.simulate = simulateMovement

        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                // Same fix delivered to every provider so apps using either
                // GPS, NETWORK or FUSED see a consistent position.
                for (provider in activeProviders()) {
                    val loc = buildLocation(provider)
                    try {
                        lm.setTestProviderLocation(provider, loc)
                    } catch (e: Throwable) {
                        logErr("setTestProviderLocation($provider)", e)
                    }
                }

                val fusedFix = buildLocation(LocationManager.GPS_PROVIDER)
                try {
                    fused.setMockLocation(fusedFix)
                } catch (e: Throwable) {
                    logErr("setMockLocation(fused)", e)
                }

                lastAccuracy = fusedFix.accuracy
                withContext(Dispatchers.Main) { onTick(fusedFix) }
                // Interval acak 800-1200 ms: interval tepat 1000 ms adalah pola
                // yang mudah dikenali sebagai hasil injeksi.
                delay(if (realistic) Random.nextLong(800L, 1201L) else 1000L)

                when {
                    route.isNotEmpty() -> advanceAlongRoute()
                    simulate && speedMps > 0f -> advanceAlongBearing()
                }
            }
        }
    }

    /** Walk [speedMps] metres toward the current waypoint, rolling over as needed. */
    private fun advanceAlongRoute() {
        var budget = speedMps.toDouble()
        if (budget <= 0.0) return
        var guard = 0
        while (budget > 0.0 && route.isNotEmpty() && guard++ < 512) {
            val idx = routeIdx
            if (idx >= route.size) {
                if (routeLoop) {
                    routeIdx = 0
                    continue
                }
                return // route finished — pin parks on the last waypoint
            }
            val (tLat, tLon) = route[idx]
            val dist = distanceMeters(curLat, curLon, tLat, tLon)
            if (dist <= budget) {
                curLat = tLat
                curLon = tLon
                budget -= dist
                routeIdx = idx + 1
            } else {
                val b = bearingTo(curLat, curLon, tLat, tLon)
                bearingDeg = b
                offsetByMetres(budget, b)
                budget = 0.0
            }
        }
    }

    private fun advanceAlongBearing() {
        offsetByMetres(speedMps.toDouble(), bearingDeg)
    }

    private fun offsetByMetres(metres: Double, bearing: Float) {
        val rad = Math.toRadians(bearing.toDouble())
        curLat += (metres * cos(rad)) / 111_320.0
        curLon += (metres * sin(rad)) / (111_320.0 * cos(Math.toRadians(curLat)))
    }

    private fun buildLocation(provider: String): Location {
        // Optional ±1.5 m jitter so the fix looks like a real GPS reading.
        // Applied to the reported fix only — the true path stays exact.
        val jLat: Double
        val jLon: Double
        if (jitter || realistic) {
            val jM = if (realistic) 2.0 else 1.5
            val rLat = jM / 111_320.0
            val rLon = jM / (111_320.0 * cos(Math.toRadians(curLat)))
            jLat = curLat + Random.nextDouble(-rLat, rLat)
            jLon = curLon + Random.nextDouble(-rLon, rLon)
        } else {
            jLat = curLat
            jLon = curLon
        }
        val moving = route.isNotEmpty() || (simulate && speedMps > 0f)
        val acc = if (realistic) Random.nextDouble(2.4, 6.8).toFloat()
        else if (jitter) 5f else 3f
        val alt = if (realistic) 22.0 + Random.nextDouble(-4.0, 4.0) else 25.0
        return Location(provider).apply {
            latitude = jLat
            longitude = jLon
            accuracy = acc
            speed = if (moving) speedMps else 0f
            bearing = bearingDeg
            altitude = alt
            time = System.currentTimeMillis()
            @Suppress("DEPRECATION")
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters =
                    if (realistic) Random.nextDouble(0.8, 3.5).toFloat() else 1f
                speedAccuracyMetersPerSecond =
                    if (realistic) Random.nextDouble(0.3, 1.1).toFloat() else 0.5f
                bearingAccuracyDegrees =
                    if (realistic) Random.nextDouble(3.0, 8.0).toFloat() else 2f
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            fused.setMockMode(false)
        } catch (_: Throwable) {
        }
        for (provider in injected.toList()) {
            try {
                lm.setTestProviderEnabled(provider, false)
            } catch (_: Throwable) {
            }
            try {
                lm.removeTestProvider(provider)
            } catch (_: Throwable) {
            }
        }
        injected.clear()
    }

    /**
     * Register both GPS and NETWORK as test providers.
     *
     * addTestProvider() MUST be called even when the real provider already
     * exists — otherwise setTestProviderLocation() throws and nothing is
     * injected at all.
     */
    @SuppressLint("MissingPermission")
    private fun ensureProviders() {
        for (provider in activeProviders()) {
            val network = provider != LocationManager.GPS_PROVIDER
            try {
                lm.addTestProvider(
                    provider,
                    network,        // requiresNetwork
                    !network,       // requiresSatellite
                    false,          // requiresCell
                    false,          // hasMonetaryCost
                    true,           // supportsAltitude
                    true,           // supportsSpeed
                    true,           // supportsBearing
                    1, 1
                )
            } catch (_: IllegalArgumentException) {
                // already registered as a test provider — fine
            } catch (e: Throwable) {
                logErr("addTestProvider($provider)", e)
            }
            try {
                lm.setTestProviderEnabled(provider, true)
                injected.add(provider)
            } catch (e: Throwable) {
                logErr("setTestProviderEnabled($provider)", e)
            }
        }
    }

    /** API 31+ juga punya provider "fused" di LocationManager; app modern
     *  (termasuk geolocator Flutter) bisa membaca jalur ini. */
    private fun activeProviders(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                "fused"
            )
        } else {
            PROVIDERS
        }

    private companion object {
        val PROVIDERS = arrayOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = (lat2 - lat1) * 111_320.0
            val dLon = (lon2 - lon1) * 111_320.0 * cos(Math.toRadians((lat1 + lat2) / 2.0))
            return sqrt(dLat * dLat + dLon * dLon)
        }

        fun bearingTo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val dLon = Math.toRadians(lon2 - lon1)
            val y = sin(dLon) * cos(Math.toRadians(lat2))
            val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
            val deg = Math.toDegrees(atan2(y, x)).toFloat()
            return (deg + 360f) % 360f
        }
    }
}
