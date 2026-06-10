package com.example.vision100.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vision100.data.local.AppDatabase
import com.example.vision100.network.ApiService
import com.example.vision100.notifications.CheckInNotifier
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CheckInWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.pendingCheckInDao()
        val apiService = ApiService.create()

        val pendingList = dao.getAllPending()
        if (pendingList.isEmpty()) return Result.success()

        var allSuccess = true
        val authHeader = ApiService.getAuthHeader()
        if (authHeader.isEmpty()) return Result.retry()
        
        val langHeader = ApiService.getLanguageHeader()

        for (item in pendingList) {
            try {
                val file = File(item.photoPath)
                if (!file.exists()) {
                    dao.deleteById(item.id)
                    continue
                }

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                val lat = item.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val lon = item.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val acc = item.accuracy.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = apiService.verifyCheckIn(
                    authHeader,
                    langHeader,
                    body, lat, lon, acc
                )

                dao.deleteById(item.id)
                if (file.exists()) {
                    file.delete()
                }

                if (response.verified) {
                    CheckInNotifier.showResult(applicationContext, response)
                } else {
                    CheckInNotifier.showProblem(applicationContext, response.reason)
                }
            } catch (e: retrofit2.HttpException) {
                Log.e("CheckInWorker", "Server rejected sync for item ${item.id}")
                dao.deleteById(item.id)
                val file = File(item.photoPath)
                if (file.exists()) file.delete()
                CheckInNotifier.showProblem(applicationContext, ApiService.parseError(e))
            } catch (e: Exception) {
                Log.e("CheckInWorker", "Sync failed for item ${item.id}", e)
                allSuccess = false
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }
}
