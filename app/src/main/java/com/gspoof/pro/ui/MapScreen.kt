package com.gspoof.pro.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gspoof.pro.AddressFormat
import com.gspoof.pro.MockEngine
import com.gspoof.pro.SpoofBridge
import com.gspoof.pro.SpoofService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.MapTileIndex
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import com.gspoof.pro.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val Accent = Color(0xFF1A73E8)
private val Live = Color(0xFF34A853)
private val Danger = Color(0xFFEA4335)

/**
 * Free raster basemaps, no API key needed.
 *
 * LIGHT is Esri World Street Map — the closest free approximation of the
 * default Google Maps look (white streets, orange arterials, halo labels).
 * Exact Google styling is only possible with the Google Maps SDK + API key.
 */
private class PatternTileSource(
    name: String,
    private val pattern: String,
    maxZoom: Int = 19
) : OnlineTileSourceBase(name, 0, maxZoom, 256, "", arrayOf("")) {
    override fun getTileURLString(pMapTileIndex: Long): String = pattern
        .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
        .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
        .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())
}

private val STYLE_GOOGLE = PatternTileSource(
    "GoogleRoadmap",
    "https://mt1.google.com/vt/lyrs=m&hl=id&x={x}&y={y}&z={z}&scale=2",
    maxZoom = 20
)

private val STYLE_LIGHT = PatternTileSource(
    "EsriStreetMap",
    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}"
)

private val STYLE_DARK = PatternTileSource(
    "EsriDarkGray",
    "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}",
    maxZoom = 16
)

private val STYLE_OSM = TileSourceFactory.MAPNIK

private fun styleFor(index: Int) = when (index) {
    0 -> STYLE_GOOGLE
    1 -> STYLE_LIGHT
    2 -> STYLE_DARK
    else -> STYLE_OSM
}

private fun styleName(index: Int) = when (index) {
    0 -> "Google"
    1 -> "Street"
    2 -> "Dark"
    else -> "OSM"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SpoofBridge.engine(context) }

    val prefs = remember { context.getSharedPreferences("gspoof", Context.MODE_PRIVATE) }
    val hasSavedPos = remember { prefs.contains("lat") && prefs.contains("lon") }

    var lat by remember {
        mutableDoubleStateOf(
            prefs.getString("lat", null)?.toDoubleOrNull() ?: -6.2088
        )
    }
    var lon by remember {
        mutableDoubleStateOf(
            prefs.getString("lon", null)?.toDoubleOrNull() ?: 106.8456
        )
    }
    var speedKmh by remember { mutableFloatStateOf(6f) }
    var bearing by remember { mutableFloatStateOf(0f) }
    var simulate by remember { mutableStateOf(true) }
    val running by SpoofBridge.running.collectAsState()
    val tickLat by SpoofBridge.lastLat.collectAsState()
    val tickLon by SpoofBridge.lastLon.collectAsState()
    val tickAcc by SpoofBridge.lastAccuracy.collectAsState()
    val tickErr by SpoofBridge.lastError.collectAsState()
    var mockAllowed by remember { mutableStateOf(engine.isMockAllowed()) }
    var mapStyle by remember { mutableStateOf(0) }
    var routeMode by remember { mutableStateOf(false) }
    var routeLoop by remember { mutableStateOf(false) }
    var jitter by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    val routePts = remember { mutableStateListOf<Pair<Double, Double>>() }
    var polylineRef by remember { mutableStateOf<Polyline?>(null) }
    var pinAddress by remember { mutableStateOf<String?>(null) }
    var addrBusy by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var mapRef by remember { mutableStateOf<MapView?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }
    var favCount by remember { mutableStateOf(FavoritesStore(context).all().size) }
    var showFavs by remember { mutableStateOf(false) }
    var favList by remember { mutableStateOf(FavoritesStore(context).all()) }
    var locating by remember { mutableStateOf(false) }

    // Save-bookmark dialog
    var saveDialog by remember { mutableStateOf(false) }
    var pendingFav by remember { mutableStateOf<Fav?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var addrLoading by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }
    var editName by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(saveDialog) {
        if (saveDialog) {
            addrLoading = true
            val addr = reverseGeocode(lat, lon).orEmpty()
            pendingFav = Fav(lat, lon, "", addr)
            nameInput = if (addr.isNotBlank()) {
                addr.split(",").take(2).joinToString(",").trim()
            } else {
                "Pin %.4f, %.4f".format(lat, lon)
            }
            addrLoading = false
        }
    }

    // On first open: restore the last pin, or fall back to the device position
    LaunchedEffect(Unit) {
        if (hasSavedPos) {
            mapRef?.controller?.setZoom(17.0)
            mapRef?.controller?.setCenter(GeoPoint(lat, lon))
            statusMsg = "Posisi terakhir dipulihkan"
            return@LaunchedEffect
        }
        val real = currentDeviceLocation(context)
        if (real != null) {
            lat = real.latitude
            lon = real.longitude
            mapRef?.controller?.setZoom(17.0)
            mapRef?.controller?.setCenter(GeoPoint(lat, lon))
        } else {
            statusMsg = "GPS fix unavailable — long-press the map to pick a point"
        }
    }

    // Follow the position broadcast by the foreground service
    LaunchedEffect(tickLat, tickLon) {
        val la = tickLat
        val lo = tickLon
        if (la != null && lo != null) {
            lat = la
            lon = lo
        }
    }

    // Detailed street address of the current pin, shown in the bottom panel.
    // Polls instead of reacting to every tick: ~0.8 s after a tap/long-press when
    // idle, every 3 s while simulating (Nominatim allows ~1 request/second).
    LaunchedEffect(Unit) {
        var lastLa = Double.NaN
        var lastLo = Double.NaN
        while (true) {
            val la = lat
            val lo = lon
            if (lastLa.isNaN() || lastLa != la || lastLo != lo) {
                addrBusy = true
                pinAddress = AddressFormat.lookup(context, la, lo)
                addrBusy = false
                lastLa = la
                lastLo = lo
            }
            delay(if (running) 3000L else 800L)
        }
    }

    // Persist the pin so it survives app restarts
    DisposableEffect(Unit) {
        onDispose {
            prefs.edit().putString("lat", lat.toString())
                .putString("lon", lon.toString()).apply()
        }
    }

    // Re-check mock permission whenever the screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mockAllowed = engine.isMockAllowed()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Box(Modifier.fillMaxSize()) {

        // ---------- MAP ----------
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    // Browser-like UA: Google's tile CDN rejects the default osmdroid agent
                    Configuration.getInstance().userAgentValue =
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                    setTileSource(styleFor(mapStyle))
                    setMultiTouchControls(true)
                    setTilesScaledToDpi(true)
                    setUseDataConnection(true)
                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(lat, lon))
                    val receiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            p ?: return false
                            if (routeMode) {
                                routePts.add(p.latitude to p.longitude)
                                SpoofBridge.engine(context).setRoute(routePts.toList(), routeLoop)
                            } else {
                                lat = p.latitude
                                lon = p.longitude
                            }
                            return true
                        }
                    }
                    overlays.add(MapEventsOverlay(receiver))

                    // Google-Maps-ish chrome: scale bar bottom-left
                    val scale = ScaleBarOverlay(this)
                    scale.setAlignBottom(true)
                    scale.setAlignRight(false)
                    scale.setScaleBarOffset(16, 26)
                    overlays.add(scale)

                    mapRef = this
                }
            },
            update = { map ->
                val desired = styleFor(mapStyle)
                if (map.tileProvider.tileSource != desired) {
                    map.setTileSource(desired)
                    map.invalidate()
                }
                val gp = GeoPoint(lat, lon)
                val m = markerRef ?: Marker(map).also {
                    it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    it.icon = context.getDrawable(R.drawable.pin_blue)
                    it.title = "Mock position"
                    it.setInfoWindow(null)
                    map.overlays.add(it)
                    markerRef = it
                }
                m.position = gp
                m.snippet = "%.6f, %.6f".format(lat, lon)

                // Route polyline (Google-blue, rounded caps)
                val pl = polylineRef ?: Polyline(map).also {
                    it.outlinePaint.color = 0xFF1A73E8.toInt()
                    it.outlinePaint.strokeWidth = 12f
                    it.outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    it.outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    map.overlays.add(0, it)
                    polylineRef = it
                }
                pl.setPoints(routePts.map { GeoPoint(it.first, it.second) })
                map.invalidate()
            }
        )

        // ---------- SEARCH BAR ----------
        Column(
            Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search place or address…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searching) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp), strokeWidth = 2.dp
                            )
                        } else if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        val q = query.trim()
                        if (q.isNotEmpty()) {
                            searching = true
                            scope.launch {
                                val hit = geocode(q)
                                searching = false
                                if (hit != null) {
                                    lat = hit.first
                                    lon = hit.second
                                    mapRef?.controller?.animateTo(GeoPoint(lat, lon))
                                    statusMsg = null
                                } else {
                                    statusMsg = "Location not found"
                                }
                            }
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(visible = statusMsg != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Danger,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        statusMsg ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            AnimatedVisibility(visible = !mockAllowed) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF3CD),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Mock location not enabled",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF6B4E00)
                            )
                            Text(
                                "Enable Developer options → Select mock location app → G-Spoof Pro",
                                fontSize = 11.sp,
                                color = Color(0xFF8A6D1F)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF6B4E00),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        ) {
                            Text(
                                "OPEN",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---------- BOTTOM CONTROL PANEL ----------
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(Modifier.padding(18.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (running) Live else MaterialTheme.colorScheme.outline)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (running) {
                                val err = tickErr
                                if (err == null) "Broadcasting · acc %.1f m".format(tickAcc)
                                else "Running · " + err.take(48)
                            } else "Idle",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            favList = FavoritesStore(context).all()
                            showFavs = true
                        }) {
                            Box {
                                Icon(
                                    Icons.Filled.BookmarkBorder, "Favourites",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (favCount > 0) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .size(15.dp)
                                            .clip(CircleShape)
                                            .background(Accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$favCount",
                                            fontSize = 9.sp,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "%.6f, %.6f".format(lat, lon),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Accent
                    )

                    Spacer(Modifier.height(8.dp))

                    // Detailed address of the selected point
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = "Address",
                                tint = Accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    addrBusy && pinAddress == null -> "Mencari alamat…"
                                    addrBusy -> pinAddress ?: "Mencari alamat…"
                                    !pinAddress.isNullOrBlank() -> pinAddress!!
                                    else -> "Alamat tidak ditemukan (butuh internet)"
                                },
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        "mock-op=${engine.mockOpMode()} " +
                            "(0=unset, 3=allowed) · gps+net+fused · v2.1",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "© Esri, HERE, Garmin · © OpenStreetMap contributors",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Speed", fontSize = 13.sp)
                        Spacer(Modifier.width(12.dp))
                        Slider(
                            value = speedKmh,
                            onValueChange = { speedKmh = it; engine.speedMps = it / 3.6f },
                            valueRange = 0f..120f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "%.0f km/h".format(speedKmh),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Simulate movement", fontSize = 13.sp)
                            Text(
                                "Walk the pin along the current bearing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = simulate, onCheckedChange = { simulate = it })
                    }

                    // ---------- ADVANCED: route / bearing / jitter ----------
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { advanced = !advanced }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Opsi lanjutan", fontSize = 12.sp, color = Accent)
                        Spacer(Modifier.weight(1f))
                        if (routePts.isNotEmpty()) {
                            Text(
                                "${routePts.size} titik rute",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = advanced) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Mode rute", fontSize = 13.sp)
                                    Text(
                                        if (routeMode) "Tap & tahan peta untuk menambah titik"
                                        else "Pin bergerak mengikuti titik rute",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = routeMode, onCheckedChange = { routeMode = it })
                            }

                            if (routePts.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = {
                                        if (routePts.isNotEmpty()) routePts.removeAt(routePts.size - 1)
                                    }) { Text("Undo", fontSize = 12.sp) }
                                    TextButton(onClick = { routePts.clear() }) {
                                        Text("Reset", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text("Loop", fontSize = 12.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Switch(
                                        checked = routeLoop,
                                        onCheckedChange = { routeLoop = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }

                            if (!routeMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Arah", fontSize = 13.sp)
                                    Slider(
                                        value = bearing,
                                        onValueChange = {
                                            bearing = it
                                            engine.bearingDeg = it
                                        },
                                        valueRange = 0f..359f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${bearing.toInt()}°",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Jitter GPS", fontSize = 13.sp)
                                    Text(
                                        "Noise ±1.5 m supaya terlihat natural",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = jitter,
                                    onCheckedChange = { jitter = it; engine.jitter = it }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (running) Danger else Accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = startStop@{
                            if (!engine.isMockAllowed()) {
                                mockAllowed = false
                                statusMsg = "Enable mock location for G-Spoof Pro first"
                                return@startStop
                            }
                            prefs.edit().putString("lat", lat.toString())
                                .putString("lon", lon.toString()).apply()
                            if (running) {
                                context.startService(
                                    Intent(context, SpoofService::class.java)
                                        .setAction(SpoofService.ACTION_STOP)
                                )
                            } else {
                                val svc = Intent(context, SpoofService::class.java).apply {
                                    action = SpoofService.ACTION_START
                                    putExtra(SpoofService.EXTRA_LAT, lat)
                                    putExtra(SpoofService.EXTRA_LON, lon)
                                    putExtra(SpoofService.EXTRA_SPEED, speedKmh / 3.6f)
                                    putExtra(SpoofService.EXTRA_BEARING, bearing)
                                    putExtra(
                                        SpoofService.EXTRA_SIMULATE,
                                        simulate || (routeMode && routePts.size >= 2)
                                    )
                                }
                                // Push route/jitter straight into the shared engine
                                engine.jitter = jitter
                                engine.bearingDeg = bearing
                                engine.speedMps = speedKmh / 3.6f
                                if (routeMode && routePts.size >= 2) {
                                    engine.setRoute(routePts.toList(), routeLoop)
                                } else {
                                    engine.clearRoute()
                                }
                                context.startForegroundService(svc)
                                statusMsg = if (routeMode && routePts.size >= 2)
                                    "Rute ${routePts.size} titik · loop=${if (routeLoop) "on" else "off"}"
                                else null
                            }
                        }
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                null, tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (running) "STOP SPOOFING" else "START SPOOFING",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // ---------- ZOOM BUTTONS ----------
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 388.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp),
                onClick = {
                    mapStyle = (mapStyle + 1) % 4
                    statusMsg = "Map style: ${styleName(mapStyle)}"
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Layers,
                        "Map style",
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            listOf(Icons.Filled.Add to true, Icons.Filled.Remove to false).forEach { pair ->
                val zoomIn = pair.second
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(44.dp),
                    onClick = {
                        val c = mapRef?.controller ?: return@Surface
                        if (zoomIn) c.zoomIn() else c.zoomOut()
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            pair.first,
                            if (zoomIn) "Zoom in" else "Zoom out",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ---------- RECENTER FAB ----------
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 320.dp)
                .size(48.dp),
            onClick = {
                if (locating) return@Surface
                locating = true
                scope.launch {
                    val real = currentDeviceLocation(context)
                    locating = false
                    if (real == null) {
                        statusMsg = "Can't get a GPS fix — check location permission"
                    } else {
                        lat = real.latitude
                        lon = real.longitude
                        mapRef?.controller?.animateTo(GeoPoint(lat, lon))
                        statusMsg = "Pin moved to your real position"
                    }
                }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (locating) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp, color = Accent
                    )
                } else {
                    Icon(
                        Icons.Filled.MyLocation, "Use my real position",
                        tint = Accent, modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ---------- CENTER-ON-PIN FAB ----------
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 378.dp)
                .size(42.dp),
            onClick = {
                mapRef?.controller?.animateTo(GeoPoint(lat, lon))
                markerRef?.showInfoWindow()
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🎯", fontSize = 16.sp)
            }
        }

        // ---------- RENAME DIALOG ----------
        if (editIndex >= 0) {
            AlertDialog(
                onDismissRequest = { editIndex = -1 },
                title = { Text("Ubah nama bookmark", fontWeight = FontWeight.Bold) },
                text = {
                    TextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val store = FavoritesStore(context)
                        store.renameAt(editIndex, editName.ifBlank { "Pin" })
                        favList = store.all()
                        editIndex = -1
                        statusMsg = "Nama diperbarui"
                    }) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { editIndex = -1 }) { Text("Batal") }
                }
            )
        }

        // ---------- SAVE BOOKMARK DIALOG ----------
        if (saveDialog) {
            AlertDialog(
                onDismissRequest = { saveDialog = false },
                title = { Text("Simpan bookmark", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "%.6f, %.6f".format(lat, lon),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Accent
                        )
                        Spacer(Modifier.height(8.dp))

                        when {
                            addrLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    Modifier.size(14.dp), strokeWidth = 2.dp, color = Accent
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Mencari alamat…", fontSize = 12.sp)
                            }
                            else -> Text(
                                pendingFav?.address?.takeIf { it.isNotBlank() }
                                    ?: "Alamat tidak ditemukan (offline / tanpa jaringan)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        TextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama bookmark") },
                            placeholder = { Text("mis. Rumah, Kantor, Kafe") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val addr = pendingFav?.address.orEmpty()
                            val store = FavoritesStore(context)
                            store.add(
                                Fav(
                                    lat = lat,
                                    lon = lon,
                                    name = nameInput.ifBlank { "Pin %.4f, %.4f".format(lat, lon) },
                                    address = addr
                                )
                            )
                            favList = store.all()
                            favCount = favList.size
                            saveDialog = false
                            statusMsg = "Bookmark disimpan"
                        }
                    ) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { saveDialog = false }) { Text("Batal") }
                }
            )
        }

        // ---------- FAVOURITES SHEET ----------
        if (showFavs) {
            ModalBottomSheet(onDismissRequest = { showFavs = false }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text("Favourites", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Accent.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            pendingFav = null
                            nameInput = ""
                            saveDialog = true
                        }
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("＋", fontSize = 18.sp, color = Accent)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Save current pin",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "%.5f, %.5f".format(lat, lon),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Accent.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (favList.isEmpty()) {
                        Text(
                            "Belum ada favorit. Simpan dulu pakai tombol di atas.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 18.dp)
                        )
                    } else {
                        LazyColumn(Modifier.heightIn(max = 340.dp)) {
                            itemsIndexed(favList) { idx, item ->
                                val fLat = item.lat
                                val fLon = item.lon
                                val fName = item.name
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(76.dp),
                                        onClick = {
                                            lat = fLat
                                            lon = fLon
                                            mapRef?.controller?.animateTo(GeoPoint(fLat, fLon))
                                            mapRef?.controller?.setZoom(17.0)
                                            showFavs = false
                                            statusMsg = "Loaded $fName"
                                        }
                                    ) {
                                        Column(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                fName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            if (item.address.isNotBlank()) {
                                                Text(
                                                    item.address,
                                                    fontSize = 10.sp,
                                                    lineHeight = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "%.6f, %.6f".format(fLat, fLon),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Accent
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        editIndex = idx
                                        editName = fName
                                    }) {
                                        Icon(
                                            Icons.Filled.Edit, "Rename",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        clipboard.setText(
                                            AnnotatedString(
                                                "$fName\n${item.address}\n%.6f, %.6f"
                                                    .format(fLat, fLon)
                                            )
                                        )
                                        statusMsg = "Koordinat disalin"
                                    }) {
                                        Icon(
                                            Icons.Filled.ContentCopy, "Copy",
                                            tint = Accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        val store = FavoritesStore(context)
                                        store.removeAt(idx)
                                        favList = store.all()
                                        favCount = favList.size
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete, "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun currentDeviceLocation(context: Context): android.location.Location? {
    val fusedLoc = withContext(Dispatchers.Main) {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine<android.location.Location?> { cont ->
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        } catch (t: Throwable) {
            null
        }
    }
    if (fusedLoc != null) return fusedLoc

    return withContext(Dispatchers.IO) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE)
                as android.location.LocationManager
            listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER,
                android.location.LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
        } catch (t: Throwable) {
            null
        }
    }
}

private suspend fun reverseGeocode(lat: Double, lon: Double): String? =
    withContext(Dispatchers.IO) {
        var conn: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL(
                "https://nominatim.openstreetmap.org/reverse?format=json&zoom=18" +
                    "&addressdetails=1&accept-language=id&lat=$lat&lon=$lon"
            )
            conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "GSpoofPro/1.9 (Android)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            org.json.JSONObject(body).optString("display_name").takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

private suspend fun geocode(q: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    var conn: HttpURLConnection? = null
    try {
        val url = URL(
            "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
                URLEncoder.encode(q, "UTF-8")
        )
        conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("User-Agent", "GSpoofPro/1.0 (Android)")
        }
        if (conn.responseCode != 200) return@withContext null
        val body = conn.inputStream.bufferedReader().readText()
        val arr = JSONArray(body)
        if (arr.length() == 0) return@withContext null
        val o = arr.getJSONObject(0)
        o.getString("lat").toDouble() to o.getString("lon").toDouble()
    } catch (t: Throwable) {
        null
    } finally {
        conn?.disconnect()
    }
}

/** One saved location. */
data class Fav(
    val lat: Double,
    val lon: Double,
    val name: String,
    val address: String
)

/** Tiny JSON-backed favourites store (no Room / no KSP). */
class FavoritesStore(context: Context) {
    private val file = java.io.File(context.filesDir, "favorites.json")

    fun all(): List<Fav> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Fav(
                    lat = o.getDouble("lat"),
                    lon = o.getDouble("lon"),
                    name = o.optString("name"),
                    address = o.optString("address")
                )
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    fun add(f: Fav) {
        val list = all().toMutableList()
        list.add(f)
        persist(list)
    }

    fun renameAt(index: Int, name: String) {
        val list = all().toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(name = name)
            persist(list)
        }
    }

    fun removeAt(index: Int) {
        val list = all().toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            persist(list)
        }
    }

    private fun persist(list: List<Fav>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(
                org.json.JSONObject()
                    .put("lat", f.lat)
                    .put("lon", f.lon)
                    .put("name", f.name)
                    .put("address", f.address)
            )
        }
        runCatching { file.writeText(arr.toString()) }
    }
}
