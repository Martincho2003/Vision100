package com.example.vision100.ui.screens

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.vision100.R
import com.example.vision100.data.CheckInResponse
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
    val checkInResult by viewModel.checkInResult
    val errorMessage by viewModel.errorMessage

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

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

    BackHandler {
        if (checkInResult != null || errorMessage != null) {
            viewModel.clearResult()
        } else {
            onNavigateBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission && hasLocationPermission) {
            CameraPreview(
                onPhotoCaptured = { uri ->
                    viewModel.verifyCheckIn(context, uri)
                }
            )
        } else {
            Text(
                stringResource(R.string.permissions_required),
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    Button(onClick = { viewModel.clearResult() }) { Text(stringResource(R.string.ok)) }
                }
            )
        }
    }
}

@Composable
fun CameraPreview(onPhotoCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var camera: Camera? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }

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

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Camera, contentDescription = "Capture", tint = Color.White)
        }
    }
}

@Composable
fun CheckInResultDialog(result: CheckInResponse, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (result.verified) stringResource(R.string.checkin_success_title) 
                else stringResource(R.string.checkin_fail_title),
                color = if (result.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(result.reason)
                if (result.verified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.pts_awarded, result.pointsAwarded), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.object_label, result.touristObject?.name ?: "Unknown"))
                }
                if (result.distanceMeters > 0) {
                    Text(stringResource(R.string.distance_label, result.distanceMeters))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.awesome)) }
        }
    )
}
