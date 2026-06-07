package com.example.vision100.network

import android.util.Log
import com.example.vision100.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("health")
    suspend fun checkHealth(
        @Header("Accept-Language") language: String,
    ): Map<String, String>

    @GET("api/objects")
    suspend fun getObjects(
        @Header("Authorization") token: String,
        @Header("Accept-Language") language: String,
    ): List<TouristObject>

    @POST("api/auth/sync")
    suspend fun syncUser(
        @Header("Authorization") token: String,
        @Header("Accept-Language") language: String,
        @Body request: UserRegisterRequest
    ): UserResponse

    @GET("api/users/me")
    suspend fun getMe(
        @Header("Authorization") token: String,
        @Header("Accept-Language") language: String
    ): UserResponse

    @PUT("api/users/me/name")
    suspend fun updateName(
        @Header("Authorization") token: String,
        @Header("Accept-Language") language: String,
        @Body request: UpdateNameRequest
    ): UserResponse

    @GET("api/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardUser>

    @GET("api/visits/me")
    suspend fun getMyVisits(
        @Header("Authorization") token: String,
        @Header("Accept-Language") language: String
    ): List<VisitResponse>

    @Multipart
    @POST("api/checkins/verify")
    suspend fun verifyCheckIn(
        @Header("Authorization") token: String,
        @Header("Accept-Language") language: String,
        @Part photo: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("gps_accuracy") accuracy: RequestBody?
    ): CheckInResponse

    companion object {
        private const val TAG = "ApiService"
        private const val BASE_URL = "http://192.168.0.2:8000/"

        fun create(): ApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(65, TimeUnit.SECONDS)
                .writeTimeout(65, TimeUnit.SECONDS)
                .callTimeout(70, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        fun getLanguageHeader(): String {
            return Locale.getDefault().language
        }

        fun getVisitPhotoUrl(visitId: Int): String {
            return "${BASE_URL}api/visits/$visitId/photo"
        }

        fun parseError(e: Throwable): String {
            val isBg = Locale.getDefault().language == "bg"

            return when (e) {
                is HttpException -> {
                    try {
                        val errorBody = e.response()?.errorBody()?.string() ?: ""
                        val json = JSONObject(errorBody)
                        val detail = json.optString("detail", "")
                        if (detail.isNotEmpty()) {
                             if (detail.length > 200) {
                                 if (isBg) "Възникна неочаквана сървърна грешка." else "An unexpected server error occurred."
                             } else detail
                        } else {
                            when (e.code()) {
                                500 -> if (isBg) "Сървърна грешка. Моля, опитайте по-късно." else "Server internal error. Please try again later."
                                401 -> if (isBg) "Сесията е изтекла." else "Session expired."
                                403 -> if (isBg) "Достъпът е отказан." else "Access denied."
                                else -> "Error: ${e.code()}"
                            }
                        }
                    } catch (ex: Exception) {
                        "Error: ${e.code()}"
                    }
                }
                is SocketTimeoutException -> {
                    if (isBg) "Връзката прекъсна поради забавяне. Моля, опитайте отново."
                    else "Connection timed out. Please try again."
                }
                is IOException -> {
                    if (isBg) "Няма връзка със сървъра. Проверете интернет връзката си."
                    else "Cannot connect to server. Check your internet connection."
                }
                else -> e.localizedMessage ?: (if (isBg) "Възникна грешка" else "An error occurred")
            }
        }

        suspend fun getAuthHeader(): String {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Log.e(TAG, "getAuthHeader: No current Firebase user found!")
                return ""
            }
            
            return try {
                val result = user.getIdToken(true).await()
                val token = result.token
                
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "getAuthHeader: Token is null or empty")
                    ""
                } else {
                    Log.d(TAG, "getAuthHeader: Successfully retrieved Firebase ID Token")
                    "Bearer $token"
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAuthHeader: Failed to get ID token", e)
                ""
            }
        }
    }
}
