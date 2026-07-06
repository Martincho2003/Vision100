package com.example.vision100.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vision100.network.ApiService
import com.example.vision100.worker.CheckInWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(private val apiService: ApiService) : ViewModel() {
    private val _isOffline = MutableStateFlow(false)
    val isOffline = _isOffline.asStateFlow()

    private val _isCheckingHealth = MutableStateFlow(true)
    val isCheckingHealth = _isCheckingHealth.asStateFlow()

    fun checkHealth(context: Context? = null) {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            try {
                kotlinx.coroutines.withTimeout(5000L) {
                    apiService.checkHealth(ApiService.getLanguageHeader())
                }
                _isOffline.value = false
                if (context != null) {
                    triggerOfflineSync(context)
                }
            } catch (e: Exception) {
                Log.d("MainViewModel", "Server unreachable: ${e.message}")
                _isOffline.value = true
            } finally {
                _isCheckingHealth.value = false
            }
        }
    }

    private fun triggerOfflineSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val workFuture = workManager.getWorkInfosForUniqueWork("offline_checkin_sync")
        workFuture.addListener({
            try {
                val workInfos = workFuture.get()
                val isRunning = workInfos?.any { it.state == androidx.work.WorkInfo.State.RUNNING } == true
                if (!isRunning) {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    val syncRequest = OneTimeWorkRequestBuilder<CheckInWorker>()
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                        .build()

                    workManager.enqueueUniqueWork(
                        "offline_checkin_sync",
                        ExistingWorkPolicy.REPLACE,
                        syncRequest
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error checking work status", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }
}
