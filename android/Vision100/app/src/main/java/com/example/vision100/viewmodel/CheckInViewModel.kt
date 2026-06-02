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
import com.example.vision100.network.ApiService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

private const val TAG = "CheckInViewModel"

class CheckInViewModel(private val apiService: ApiService) : ViewModel() {

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
        _isLocationChecked.value = false
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
    fun verifyCheckIn(context: Context, photoUri: Uri, objectId: Int? = null) {
        _isLoading.value = true
        _errorMessage.value = null
        Log.d(TAG, "Starting Smart Check-in process...")

        viewModelScope.launch {
            try {
                Log.d(TAG, "Requesting GPS location...")
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                
                if (location == null) {
                    Log.e(TAG, "Failed to obtain GPS location (null)")
                    _isLoading.value = false
                    _errorMessage.value = context.getString(R.string.gps_error)
                    return@launch
                }

                /* 
                if (location.isFromMockProvider) {
                    Log.e(TAG, "Mock location detected during verification!")
                    _isMockLocation.value = true
                    _isLoading.value = false
                    _errorMessage.value = context.getString(R.string.mock_location_detected)
                    return@launch
                }
                */

                Log.d(TAG, "GPS Location obtained: Lat=${location.latitude}, Lon=${location.longitude}, Acc=${location.accuracy}")

                Log.d(TAG, "Preparing photo file from URI: $photoUri")
                val file = uriToFile(context, photoUri)
                Log.d(TAG, "File created at: ${file.absolutePath}, size: ${file.length()} bytes")
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

                val lat = location.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val lon = location.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val acc = location.accuracy.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val objId = objectId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                Log.d(TAG, "Sending multipart request to /api/checkins/verify...")
                val token = ApiService.getAuthHeader()
                val lang = ApiService.getLanguageHeader()
                
                val response = apiService.verifyCheckIn(token, lang, body, lat, lon, acc, objId)
                
                Log.i(TAG, "Check-in response received: verified=${response.verified}, reason='${response.reason}'")
                if (!response.verified) {
                    _errorMessage.value = response.reason
                } else {
                    _checkInResult.value = response
                }
                _isLoading.value = false
            } catch (e: HttpException) {
                val errorMsg = ApiService.parseError(e)
                Log.e(TAG, "HTTP error during check-in: code=${e.code()}, message='$errorMsg'")
                _isLoading.value = false
                _errorMessage.value = errorMsg
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception during check-in", e)
                _isLoading.value = false
                _errorMessage.value = context.getString(R.string.verification_failed_prefix, e.localizedMessage)
            }
        }
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
}
