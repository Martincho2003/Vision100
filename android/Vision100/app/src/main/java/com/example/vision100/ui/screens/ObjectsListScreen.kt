package com.example.vision100.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vision100.R
import com.example.vision100.data.TouristObject
import com.example.vision100.ui.components.FlagAccentBar
import com.example.vision100.ui.components.FlagChip
import com.example.vision100.ui.components.VisionBackground
import com.example.vision100.ui.components.VisionEmptyState
import com.example.vision100.ui.components.VisionLoadingState
import com.example.vision100.ui.components.VisionTopBar
import com.example.vision100.viewmodel.ObjectViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

private enum class ObjectsViewMode {
    List,
    Map
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectsListScreen(
    viewModel: ObjectViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val objects by viewModel.objects
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(ObjectsViewMode.List) }

    val regions = remember(objects) {
        objects.mapNotNull { it.region?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
    }
    val categories = remember(objects) {
        objects.mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
    }
    val filteredObjects = remember(objects, searchQuery, selectedRegion, selectedCategory) {
        val query = searchQuery.trim()
        objects.filter { obj ->
            val matchesSearch = query.isBlank() || listOfNotNull(
                obj.number,
                obj.name,
                obj.description,
                obj.region,
                obj.category,
                obj.aiLabels
            ).any { value -> value.contains(query, ignoreCase = true) }

            val matchesRegion = selectedRegion == null || obj.region?.trim() == selectedRegion
            val matchesCategory = selectedCategory == null || obj.category?.trim() == selectedCategory

            matchesSearch && matchesRegion && matchesCategory
        }
    }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        viewModel.fetchObjects()
    }

    Scaffold(
        topBar = {
            VisionTopBar(
                title = stringResource(R.string.tourist_objects),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        VisionBackground {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    VisionLoadingState(
                        title = stringResource(R.string.object_catalog),
                        message = stringResource(R.string.home_subtitle),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (errorMessage != null) {
                    VisionEmptyState(
                        icon = Icons.Default.FilterListOff,
                        title = stringResource(R.string.error_title),
                        message = errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ObjectsFilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            regions = regions,
                            selectedRegion = selectedRegion,
                            onRegionSelected = { selectedRegion = it },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            viewMode = viewMode,
                            onViewModeChange = { viewMode = it },
                            filteredCount = filteredObjects.size,
                            onClearFilters = {
                                searchQuery = ""
                                selectedRegion = null
                                selectedCategory = null
                            }
                        )

                        if (filteredObjects.isEmpty()) {
                            EmptyObjectsMessage(modifier = Modifier.weight(1f).fillMaxWidth())
                        } else {
                            when (viewMode) {
                                ObjectsViewMode.List -> ObjectsList(
                                    objects = filteredObjects,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                                ObjectsViewMode.Map -> ObjectsMap(
                                    objects = filteredObjects,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectsFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    regions: List<String>,
    selectedRegion: String?,
    onRegionSelected: (String?) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    viewMode: ObjectsViewMode,
    onViewModeChange: (ObjectsViewMode) -> Unit,
    filteredCount: Int,
    onClearFilters: () -> Unit
) {
    val hasActiveFilters = searchQuery.isNotBlank() || selectedRegion != null || selectedCategory != null

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(stringResource(R.string.search_objects)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = viewMode == ObjectsViewMode.List,
                    onClick = { onViewModeChange(ObjectsViewMode.List) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.list_view)) }
                )
                SegmentedButton(
                    selected = viewMode == ObjectsViewMode.Map,
                    onClick = { onViewModeChange(ObjectsViewMode.Map) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.map_view)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterDropdown(
                    label = stringResource(R.string.region_filter),
                    allLabel = stringResource(R.string.all_regions),
                    options = regions,
                    selectedValue = selectedRegion,
                    onSelected = onRegionSelected,
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    label = stringResource(R.string.category_filter),
                    allLabel = stringResource(R.string.all_categories),
                    options = categories,
                    selectedValue = selectedCategory,
                    onSelected = onCategorySelected,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.objects_count, filteredCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (hasActiveFilters) {
                    TextButton(onClick = onClearFilters) {
                        Text(stringResource(R.string.clear_filters))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    allLabel: String,
    options: List<String>,
    selectedValue: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = selectedValue ?: allLabel,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 180.dp)
            ) {
                DropdownMenuItem(
                    text = { Text(allLabel) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ObjectsList(
    objects: List<TouristObject>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(objects, key = { it.id }) { obj ->
            TouristObjectItem(obj)
        }
    }
}

@Composable
private fun ObjectsMap(
    objects: List<TouristObject>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val myLocationLabel = stringResource(R.string.my_location)
    val objectSignature = remember(objects) {
        objects.joinToString("|") { "${it.id}:${it.latitude}:${it.longitude}" }
    }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            controller.setZoom(if (objects.size == 1) 14.0 else 7.0)
            controller.setCenter(objects.centerGeoPoint())
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            fetchCurrentLocation(locationClient) { location ->
                currentLocation = location
            }
        }
    }

    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) {
            fetchCurrentLocation(locationClient) { location ->
                currentLocation = location
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            InfoWindow.closeAllInfoWindowsOn(mapView)
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().clipToBounds(),
            update = { view ->
                if (view.tag != objectSignature) {
                    InfoWindow.closeAllInfoWindowsOn(view)
                    view.controller.setZoom(if (objects.size == 1) 14.0 else 7.0)
                    view.controller.setCenter(objects.centerGeoPoint())
                    view.tag = objectSignature
                }

                InfoWindow.closeAllInfoWindowsOn(view)
                view.overlays.clear()
                objects.forEach { obj ->
                    view.overlays.add(obj.toObjectMarker(view))
                }
                currentLocation?.let { location ->
                    view.overlays.add(location.toCurrentLocationMarker(view, myLocationLabel))
                }
                view.invalidate()
            }
        )

        FloatingActionButton(
            onClick = {
                if (context.hasLocationPermission()) {
                    fetchCurrentLocation(locationClient) { location ->
                        currentLocation = location
                        mapView.controller.setZoom(15.0)
                        mapView.controller.animateTo(location.toGeoPoint())
                    }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = myLocationLabel)
        }
    }
}

private fun List<TouristObject>.centerGeoPoint(): GeoPoint {
    if (isEmpty()) return GeoPoint(42.7339, 25.4858)

    return GeoPoint(
        map { it.latitude.toDouble() }.average(),
        map { it.longitude.toDouble() }.average()
    )
}

private fun TouristObject.toObjectMarker(mapView: MapView): Marker {
    return Marker(mapView).apply {
        position = GeoPoint(latitude.toDouble(), longitude.toDouble())
        title = "$number. $name"
        snippet = listOfNotNull(region, category)
            .filter { it.isNotBlank() }
            .joinToString(" / ")
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
}

private fun Location.toCurrentLocationMarker(mapView: MapView, titleText: String): Marker {
    return Marker(mapView).apply {
        position = toGeoPoint()
        title = titleText
        snippet = if (hasAccuracy()) "Accuracy: ${accuracy.toInt()} m" else null
        icon = createCurrentLocationIcon(mapView.context)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    }
}

private fun createCurrentLocationIcon(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    val center = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = Color.argb(54, 25, 118, 210)
    canvas.drawCircle(center, center, center - 1f, paint)

    paint.color = Color.WHITE
    canvas.drawCircle(center, center, 9f * density, paint)

    paint.color = Color.rgb(25, 118, 210)
    canvas.drawCircle(center, center, 6f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun Location.toGeoPoint(): GeoPoint {
    return GeoPoint(latitude, longitude)
}

private fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    locationClient: FusedLocationProviderClient,
    onLocation: (Location) -> Unit
) {
    locationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onLocation(location)
            } else {
                val cancellationTokenSource = CancellationTokenSource()
                locationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { current ->
                    if (current != null) {
                        onLocation(current)
                    }
                }
            }
        }
}

@Composable
private fun EmptyObjectsMessage(modifier: Modifier = Modifier) {
    VisionEmptyState(
        icon = Icons.Default.FilterListOff,
        title = stringResource(R.string.no_objects_found),
        message = stringResource(R.string.clear_filters),
        modifier = modifier
    )
}

@Composable
fun TouristObjectItem(obj: TouristObject) {
    val metadata = listOfNotNull(
        obj.region?.trim()?.takeIf(String::isNotEmpty),
        obj.category?.trim()?.takeIf(String::isNotEmpty)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            FlagAccentBar(height = 3.dp)
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = obj.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = obj.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (metadata.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            metadata.take(2).forEachIndexed { index, item ->
                                FlagChip(
                                    text = item,
                                    color = if (index == 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                    Text(
                        text = obj.description ?: stringResource(R.string.no_description_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Icon(
                    imageVector = if (obj.category.isNullOrBlank()) Icons.Default.LocationOn else Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.74f),
                    modifier = Modifier.padding(start = 8.dp).size(22.dp)
                )
            }
        }
    }
}
