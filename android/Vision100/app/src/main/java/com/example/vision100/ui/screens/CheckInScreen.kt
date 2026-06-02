package com.example.vision100.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vision100.R
import com.example.vision100.data.CheckInResponse
import com.example.vision100.ui.components.DisabledScrim
import com.example.vision100.ui.components.FlagChip
import com.example.vision100.ui.components.VisionEmptyState
import com.example.vision100.viewmodel.CheckInViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading
    val isMockLocation by viewModel.isMockLocation
    val isLocationChecked by viewModel.isLocationChecked
    val checkInResult by viewModel.checkInResult
    val errorMessage by viewModel.errorMessage

    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) 
    }
    var hasLocationPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) 
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasLocationPermission = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                               permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.checkMockLocation(context)
        }
    }

    BackHandler {
        if (checkInResult != null || errorMessage != null) {
            viewModel.clearResult()
        } else {
            onNavigateBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasCameraPermission || !hasLocationPermission) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VisionEmptyState(
                    icon = Icons.Default.Camera,
                    title = stringResource(R.string.smart_check_in),
                    message = stringResource(R.string.permissions_required)
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.request_permissions))
                }
            }
        } else if (!isLocationChecked) {
            // Wait for GPS mock check to complete without flickering the camera
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (isMockLocation) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VisionEmptyState(
                    icon = Icons.Default.LocationOff,
                    title = stringResource(R.string.error_title),
                    message = stringResource(R.string.mock_location_detected)
                )
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.back))
                }
            }
        } else {
            CameraPreview(
                onNavigateBack = onNavigateBack,
                onPhotoCaptured = { uri ->
                    viewModel.verifyCheckIn(context, uri)
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DisabledScrim()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.verifying_visit),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        checkInResult?.let { result ->
            CheckInResultDialog(
                result = result, 
                onDismiss = { 
                    viewModel.clearResult()
                    if (result.verified) {
                        onNavigateBack()
                    }
                }
            )
        }

        errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearResult() },
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(error) },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.clearResult()
                        onNavigateBack()
                    }) { Text(stringResource(R.string.ok)) }
                }
            )
        }
    }
}

@Composable
fun CameraPreview(
    onNavigateBack: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var camera: Camera? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "Capture pulse")
    val capturePulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Capture pulse scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", e)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        camera?.let { cam ->
                            val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                            val minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                            val maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                            val newZoom = (currentZoom * zoom).coerceIn(minZoom, maxZoom)
                            cam.cameraControl.setZoomRatio(newZoom)
                        }
                    }
                }
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.42f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { 
                    isFlashOn = !isFlashOn
                    imageCapture?.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 120.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = Color.Black.copy(alpha = 0.48f),
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.checkin_hint_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.checkin_hint_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.84f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CheckInOverlayChip(text = stringResource(R.string.camera_ready), icon = Icons.Default.Camera)
                    CheckInOverlayChip(text = stringResource(R.string.gps_verified), icon = Icons.Default.LocationOn)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        scaleX = capturePulse
                        scaleY = capturePulse
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            ) {}
            FloatingActionButton(
                onClick = {
                    val imgCapture = imageCapture ?: return@FloatingActionButton
                    val photoFile = File(
                        context.cacheDir,
                        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imgCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(Uri.fromFile(photoFile))
                            }
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("CameraPreview", "Photo capture failed: ${exc.message}", exc)
                            }
                        }
                    )
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = "Capture",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckInOverlayChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CheckInResultDialog(result: CheckInResponse, onDismiss: () -> Unit) {
    val statusColor = if (result.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (result.verified) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    if (result.verified) stringResource(R.string.checkin_success_title)
                    else stringResource(R.string.checkin_fail_title),
                    color = statusColor,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = result.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                if (result.verified) {
                    Spacer(modifier = Modifier.height(14.dp))
                    FlagChip(
                        text = stringResource(R.string.pts_awarded, result.pointsAwarded),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(
                            R.string.object_label,
                            result.touristObject?.name ?: stringResource(R.string.unknown_object)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                if (result.distanceMeters > 0) {
                    Text(
                        text = stringResource(R.string.distance_label, result.distanceMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (result.aiConfidence > 0) {
                    Text(
                        text = "AI ${(result.aiConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.awesome)) }
        }
    )
}
