package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.model.MapDisplayType
import com.example.data.model.RoutePoint
import com.example.ui.theme.NoirAccent
import com.example.ui.theme.NoirBackgroundPrimary
import com.example.ui.theme.NoirBackgroundSecondary
import com.example.ui.theme.NoirBorder
import com.example.ui.theme.NoirSuccess
import com.example.ui.theme.NoirTextPrimary
import com.example.ui.theme.NoirTextSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.util.Locale

object StrideoMapConfig {
    @Volatile
    private var initialized = false

    val DARK_MATTER_SOURCE: ITileSource = XYTileSource(
        "CartoDBDarkMatter",
        1,
        19,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
            "https://d.basemaps.cartocdn.com/dark_all/"
        ),
        "© OpenStreetMap · © CARTO"
    )

    val STREET_OSM_SOURCE: ITileSource = TileSourceFactory.MAPNIK

    val SATELLITE_ESRI_SOURCE: ITileSource = object : OnlineTileSourceBase(
        "EsriWorldImagery",
        1,
        18,
        256,
        "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
        "© Esri"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return "${baseUrl}${MapTileIndex.getZoom(pMapTileIndex)}/${MapTileIndex.getY(pMapTileIndex)}/${MapTileIndex.getX(pMapTileIndex)}"
        }
    }

    val TERRAIN_TOPO_SOURCE: ITileSource = XYTileSource(
        "OpenTopoMap",
        1,
        17,
        256,
        ".png",
        arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        ),
        "© OpenStreetMap · © OpenTopoMap"
    )

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            val cfg = Configuration.getInstance()
            cfg.userAgentValue = "Strideo/${appContext.packageName}"
            val baseDir = File(appContext.cacheDir, "osmdroid").apply { mkdirs() }
            val tileCache = File(baseDir, "tiles").apply { mkdirs() }
            cfg.osmdroidBasePath = baseDir
            cfg.osmdroidTileCache = tileCache
            initialized = true
        }
    }

    fun tileSourceFor(type: MapDisplayType): ITileSource = when (type) {
        MapDisplayType.STANDARD -> DARK_MATTER_SOURCE
        MapDisplayType.STREET -> STREET_OSM_SOURCE
        MapDisplayType.SATELLITE -> SATELLITE_ESRI_SOURCE
        MapDisplayType.TERRAIN -> TERRAIN_TOPO_SOURCE
    }
}

@Composable
fun NoirInteractiveMap(
    points: List<RoutePoint>,
    currentLat: Double? = null,
    currentLng: Double? = null,
    isLiveTracking: Boolean = false,
    height: Dp = 280.dp,
    onRequestLocationFix: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var mapType by remember { mutableStateOf(MapDisplayType.STANDARD) }
    var isFullscreen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, NoirBorder, RoundedCornerShape(4.dp))
            .background(NoirBackgroundPrimary)
    ) {
        MapCoreContent(
            points = points,
            currentLat = currentLat,
            currentLng = currentLng,
            isLiveTracking = isLiveTracking,
            mapType = mapType,
            onMapTypeChange = { mapType = it },
            isFullscreen = false,
            onToggleFullscreen = { isFullscreen = true },
            onRequestLocationFix = onRequestLocationFix
        )
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NoirBackgroundPrimary)
            ) {
                MapCoreContent(
                    points = points,
                    currentLat = currentLat,
                    currentLng = currentLng,
                    isLiveTracking = isLiveTracking,
                    mapType = mapType,
                    onMapTypeChange = { mapType = it },
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    onRequestLocationFix = onRequestLocationFix
                )
            }
        }
    }
}

@Composable
private fun MapCoreContent(
    points: List<RoutePoint>,
    currentLat: Double?,
    currentLng: Double?,
    isLiveTracking: Boolean,
    mapType: MapDisplayType,
    onMapTypeChange: (MapDisplayType) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onRequestLocationFix: (() -> Unit)?
) {
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var osmMapRef by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmInteractiveMapSurface(
            points = points,
            currentLat = currentLat,
            currentLng = currentLng,
            isLiveTracking = isLiveTracking,
            mapType = mapType,
            recenterTrigger = recenterTrigger,
            onMapReady = { osmMapRef = it }
        )

        // Top-left live map status badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirStatusPill(
                text = if (isLiveTracking) "LIVE GPS" else "OPEN MAPS",
                accentColor = if (isLiveTracking) NoirSuccess else NoirTextPrimary,
                borderColor = NoirBorder
            )
        }

        // Top-right map layer selector (DRK / MAP / SAT / TER)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(NoirBackgroundSecondary.copy(alpha = 0.94f), RoundedCornerShape(3.dp))
                .border(1.dp, NoirBorder, RoundedCornerShape(3.dp))
        ) {
            MapDisplayType.entries.forEach { mode ->
                val active = mode == mapType
                Box(
                    modifier = Modifier
                        .testTag("map_layer_${mode.shortCode.lowercase()}")
                        .clickable { onMapTypeChange(mode) }
                        .background(if (active) NoirAccent else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.shortCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) NoirBackgroundPrimary else NoirTextSecondary
                    )
                }
            }
        }

        // Bottom-right Strava-style map controls: Zoom +, Zoom -, Recenter, Fullscreen
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MapIconControl(
                icon = Icons.Default.Add,
                contentDescription = "Zoom in",
                tag = "map_zoom_in"
            ) {
                osmMapRef?.controller?.zoomIn()
            }
            MapIconControl(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom out",
                tag = "map_zoom_out"
            ) {
                osmMapRef?.controller?.zoomOut()
            }
            MapIconControl(
                icon = Icons.Default.MyLocation,
                contentDescription = "Recenter map",
                tag = "map_recenter"
            ) {
                onRequestLocationFix?.invoke()
                recenterTrigger++
            }
            MapIconControl(
                icon = if (isFullscreen) Icons.Default.Close else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "Close fullscreen" else "Open fullscreen",
                tag = "map_fullscreen"
            ) {
                onToggleFullscreen()
            }
        }

        // Bottom-left coordinate readout + OpenStreetMap attribution
        val coordLat = currentLat ?: points.lastOrNull()?.lat
        val coordLng = currentLng ?: points.lastOrNull()?.lng
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(NoirBackgroundSecondary.copy(alpha = 0.92f), RoundedCornerShape(2.dp))
                .border(1.dp, NoirBorder, RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val readout = if (coordLat != null && coordLng != null) {
                String.format(Locale.US, "%.4f, %.4f · © OSM", coordLat, coordLng)
            } else {
                "© OpenStreetMap"
            }
            Text(
                text = readout,
                style = MaterialTheme.typography.labelSmall,
                color = NoirTextSecondary
            )
        }
    }
}

@Composable
private fun MapIconControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .testTag(tag)
            .background(NoirBackgroundSecondary.copy(alpha = 0.94f), RoundedCornerShape(3.dp))
            .border(1.dp, NoirBorder, RoundedCornerShape(3.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = NoirTextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun OsmInteractiveMapSurface(
    points: List<RoutePoint>,
    currentLat: Double?,
    currentLng: Double?,
    isLiveTracking: Boolean,
    mapType: MapDisplayType,
    recenterTrigger: Int,
    onMapReady: (MapView) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember(context) {
        StrideoMapConfig.ensureInitialized(context)
        true
    }

    val startIcon = remember(context) {
        createDotMarkerDrawable(
            context = context,
            fillColor = 0xFF4ADE80.toInt(),
            ringColor = 0xFF080808.toInt(),
            haloColor = null,
            sizeDp = 16f
        )
    }
    val finishIcon = remember(context) {
        createDotMarkerDrawable(
            context = context,
            fillColor = 0xFFFFFFFF.toInt(),
            ringColor = 0xFF080808.toInt(),
            haloColor = null,
            sizeDp = 16f
        )
    }
    val liveIcon = remember(context) {
        createDotMarkerDrawable(
            context = context,
            fillColor = 0xFFFFFFFF.toInt(),
            ringColor = 0xFF080808.toInt(),
            haloColor = 0x44FFFFFF,
            sizeDp = 26f
        )
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(StrideoMapConfig.tileSourceFor(mapType))
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(14.5)
            // Neutral global framing until GPS lock or route arrives
            controller.setCenter(GeoPoint(37.7749, -122.4194))
        }
    }

    var appliedMapType by remember { mutableStateOf<MapDisplayType?>(null) }
    var lastCameraKey by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onMapReady(mapView)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            if (appliedMapType != mapType) {
                view.setTileSource(StrideoMapConfig.tileSourceFor(mapType))
                appliedMapType = mapType
            }

            view.overlays.clear()

            val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
            val strokeColor = when (mapType) {
                MapDisplayType.STREET, MapDisplayType.TERRAIN -> 0xFF111111.toInt()
                MapDisplayType.STANDARD, MapDisplayType.SATELLITE -> 0xFFFFFFFF.toInt()
            }
            val casingColor = when (mapType) {
                MapDisplayType.STREET, MapDisplayType.TERRAIN -> 0xFFFFFFFF.toInt()
                MapDisplayType.STANDARD, MapDisplayType.SATELLITE -> 0xFF080808.toInt()
            }

            // Dual-layer high-contrast Strava-style route polyline
            if (geoPoints.size >= 2) {
                val casingLine = Polyline(view).apply {
                    setPoints(geoPoints)
                    outlinePaint.color = casingColor
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                }
                val mainLine = Polyline(view).apply {
                    setPoints(geoPoints)
                    outlinePaint.color = strokeColor
                    outlinePaint.strokeWidth = 8.5f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                }
                view.overlays.add(casingLine)
                view.overlays.add(mainLine)
            }

            // Start & Finish route markers
            if (geoPoints.isNotEmpty()) {
                val startMarker = Marker(view).apply {
                    position = geoPoints.first()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = startIcon
                    title = "Start"
                }
                view.overlays.add(startMarker)

                if (!isLiveTracking && geoPoints.size > 1) {
                    val finishMarker = Marker(view).apply {
                        position = geoPoints.last()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = finishIcon
                        title = "Finish"
                    }
                    view.overlays.add(finishMarker)
                }
            }

            // Live GPS position marker
            val livePoint = if (currentLat != null && currentLng != null) {
                GeoPoint(currentLat, currentLng)
            } else null

            if (livePoint != null) {
                val liveMarker = Marker(view).apply {
                    position = livePoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = liveIcon
                    title = "Current Location"
                }
                view.overlays.add(liveMarker)
            }

            // Smart camera framing: avoids fighting user pan/zoom while keeping live follow & recenter crisp
            val cameraKey = if (isLiveTracking) {
                "live:${geoPoints.size}:${currentLat}:${currentLng}:$recenterTrigger"
            } else {
                "static:${geoPoints.size}:${geoPoints.firstOrNull()?.latitude}:${currentLat != null}:$recenterTrigger"
            }

            if (cameraKey != lastCameraKey) {
                lastCameraKey = cameraKey
                when {
                    livePoint != null && (isLiveTracking || geoPoints.size < 2) -> {
                        view.controller.setZoom(16.0)
                        view.controller.animateTo(livePoint)
                    }
                    geoPoints.size >= 2 -> {
                        val box = BoundingBox.fromGeoPointsSafe(geoPoints)
                        view.post {
                            try {
                                view.zoomToBoundingBox(box, true, 90)
                            } catch (_: Exception) {
                                view.controller.setZoom(15.0)
                                view.controller.setCenter(geoPoints.first())
                            }
                        }
                    }
                    geoPoints.size == 1 -> {
                        view.controller.setZoom(15.5)
                        view.controller.setCenter(geoPoints.first())
                    }
                }
            }

            view.invalidate()
        }
    )
}

private fun createDotMarkerDrawable(
    context: Context,
    fillColor: Int,
    ringColor: Int,
    haloColor: Int?,
    sizeDp: Float
): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(16)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    if (haloColor != null) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = haloColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, sizePx * 0.48f, haloPaint)
    }

    val coreRadius = if (haloColor != null) sizePx * 0.27f else sizePx * 0.40f
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ringColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, coreRadius + (2.2f * density), ringPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, coreRadius, fillPaint)

    return BitmapDrawable(context.resources, bitmap)
}
