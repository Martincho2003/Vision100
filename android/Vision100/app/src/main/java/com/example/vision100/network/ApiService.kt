package com.example.vision100.network

import android.util.Log
import com.example.vision100.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.Locale

interface ApiService {
    @GET("health")
    suspend fun checkHealth(
        @Header("Accept-Language") language: String,
    ): Map<String, String>

    @GET("api/objects")
    suspend fun getObjects(
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
        @Part("gps_accuracy") accuracy: RequestBody?,
        @Part("object_id") objectId: RequestBody?
    ): CheckInResponse

    companion object {
        private const val TAG = "ApiService"
        private const val BASE_URL = "http://192.168.0.2:8000/"

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
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

        fun parseError(e: HttpException): String {
            return try {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                val json = JSONObject(errorBody)
                val detail = json.optString("detail", "Unknown server error")
                if (detail.length > 200) "An unexpected server error occurred." else detail
            } catch (ex: Exception) {
                when (e.code()) {
                    500 -> "Server internal error. Please try again later."
                    401 -> "Session expired."
                    403 -> "Access denied."
                    else -> "Error: ${e.code()}"
                }
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
