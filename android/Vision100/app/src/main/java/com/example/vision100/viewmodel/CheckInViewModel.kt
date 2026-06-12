package com.example.vision100.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vision100.R
import com.example.vision100.data.CheckInResponse
import com.example.vision100.data.local.AppDatabase
import com.example.vision100.data.local.PendingCheckIn
import com.example.vision100.network.ApiService
import com.example.vision100.notifications.CheckInNotifier
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private const val TAG = "CheckInViewModel"
private const val CHECK_IN_SERVER_TIMEOUT_MS = 60_000L

class CheckInViewModel(private val apiService: ApiService) : ViewModel() {
    private var notifyWhenCompleted = false

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isMockLocation = mutableStateOf(false)
    val isMockLocation: State<Boolean> = _isMockLocation

    private val _isLocationChecked = mutableStateOf(false)
    val isLocationChecked: State<Boolean> = _isLocationChecked

    private val _checkInResult = mutableStateOf<CheckInResponse?>(null)
    val checkInResult: State<CheckInResponse?> = _checkInResult

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun clearResult() {
        _checkInResult.value = null
        _errorMessage.value = null
        _isMockLocation.value = false
    }

    fun setNotifyWhenCompleted(enabled: Boolean) {
        notifyWhenCompleted = enabled
    }

    @SuppressLint("MissingPermission")
    fun checkMockLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            /*
            if (location?.isFromMockProvider == true) {
                _isMockLocation.value = true
                Log.e(TAG, "Mock location detected in lastLocation!")
            }
             */
            _isLocationChecked.value = true
        }.addOnFailureListener {
            Log.e(TAG, "Failed to get last location for mock check", it)
            _isLocationChecked.value = true
        }
    }

    @SuppressLint("MissingPermission")
    fun verifyCheckIn(context: Context, photoUri: Uri, isOfflineMode: Boolean = false) {
        if (_isLoading.value) return

        val appContext = context.applicationContext
        _isLoading.value = true
        _errorMessage.value = null
        _checkInResult.value = null
        Log.d(TAG, "Starting Smart Check-in process (isOfflineMode=$isOfflineMode)...")

        viewModelScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
                val location = withTimeout(15000L) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                }
                
                if (location == null) {
                    Log.e(TAG, "GPS location is null")
                    _isLoading.value = false
                    _errorMessage.value = appContext.getString(R.string.gps_error)
                    return@launch
                }

                if (isOfflineMode) {
                    Log.d(TAG, "Offline mode active. Saving locally.")
                    handleOfflineCheckIn(appContext, photoUri, location.latitude, location.longitude, location.accuracy)
                    return@launch
                }
/*
                if (location.isFromMockProvider) {
                    Log.e(TAG, "Mock location detected!")
                    _isMockLocation.value = true
                    _isLoading.value = false
                    _errorMessage.value = appContext.getString(R.string.mock_location_detected)
                    return@launch
                }
*/
                val file = uriToFile(appContext, photoUri)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                val lat = location.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val lon = location.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val acc = location.accuracy.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val token = withTimeout(10000L) { ApiService.getAuthHeader() }
                
                if (token.isEmpty()) {
                    Log.w(TAG, "Token is empty. Falling back to offline storage.")
                    handleOfflineCheckIn(appContext, photoUri, location.latitude, location.longitude, location.accuracy)
                    return@launch
                }
                
                val lang = ApiService.getLanguageHeader()
                
                try {
                    val response = withTimeout(CHECK_IN_SERVER_TIMEOUT_MS) {
                        apiService.verifyCheckIn(token, lang, body, lat, lon, acc)
                    }
                    
                    Log.i(TAG, "Check-in response: verified=${response.verified}")
                    if (!response.verified) {
                        _errorMessage.value = response.reason
                    } else {
                        _checkInResult.value = response
                    }
                    notifyIfNeeded(appContext, response = response)
                } catch (e: Exception) {
                    if (e is IOException || e is TimeoutCancellationException) {
                        Log.i(TAG, "Network issue. Saving locally. Error: ${e.message}")
                        handleOfflineCheckIn(appContext, photoUri, location.latitude, location.longitude, location.accuracy)
                    } else {
                        throw e
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val message = appContext.getString(R.string.checkin_timeout_message)
                Log.e(TAG, "Check-in timed out", e)
                _errorMessage.value = message
                notifyIfNeeded(appContext, message = message)
            } catch (e: HttpException) {
                val errorMsg = ApiService.parseError(e)
                Log.e(TAG, "HTTP error: code=${e.code()}, message='$errorMsg'")
                _errorMessage.value = errorMsg
                notifyIfNeeded(appContext, message = errorMsg)
            } catch (e: SocketTimeoutException) {
                val message = appContext.getString(R.string.checkin_timeout_message)
                Log.e(TAG, "Socket timeout", e)
                _errorMessage.value = message
                notifyIfNeeded(appContext, message = message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                val errorMsg = ApiService.parseError(e)
                Log.e(TAG, "Network error", e)
                _errorMessage.value = errorMsg
                notifyIfNeeded(appContext, message = errorMsg)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception during check-in", e)
                val message = appContext.getString(R.string.verification_failed_prefix, e.localizedMessage)
                _errorMessage.value = message
                notifyIfNeeded(appContext, message = message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun notifyIfNeeded(
        context: Context,
        response: CheckInResponse? = null,
        message: String? = null
    ) {
        if (!notifyWhenCompleted) return

        if (response != null) {
            CheckInNotifier.showResult(context, response)
        } else if (message != null) {
            CheckInNotifier.showProblem(context, message)
        }
        notifyWhenCompleted = false
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "temp_checkin.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private suspend fun handleOfflineCheckIn(
        context: Context,
        photoUri: Uri,
        lat: Double,
        lon: Double,
        accuracy: Float
    ) {
        try {
            val permanentPath = savePhotoPermanently(context, photoUri)
            val pendingCheckIn = PendingCheckIn(
                photoPath = permanentPath,
                latitude = lat,
                longitude = lon,
                accuracy = accuracy
            )

            val db = AppDatabase.getDatabase(context)
            db.pendingCheckInDao().insert(pendingCheckIn)

            scheduleOfflineSync(context)

            _errorMessage.value = context.getString(R.string.offline_checkin_saved)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save offline check-in", e)
            _errorMessage.value = context.getString(R.string.sync_failed, e.localizedMessage)
        } finally {
            _isLoading.value = false
        }
    }

    private fun savePhotoPermanently(context: Context, cacheUri: Uri): String {
        val fileName = "checkin_${System.currentTimeMillis()}.jpg"
        val permanentFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(cacheUri)?.use { input ->
            FileOutputStream(permanentFile).use { output ->
                input.copyTo(output)
            }
        }
        return permanentFile.absolutePath
    }

    private fun scheduleOfflineSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<com.example.vision100.worker.CheckInWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "offline_checkin_sync",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }
}
