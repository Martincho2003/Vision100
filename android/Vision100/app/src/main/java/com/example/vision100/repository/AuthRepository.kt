package com.example.vision100.repository

import com.example.vision100.data.UserRegisterRequest
import com.example.vision100.data.UserResponse
import com.example.vision100.network.ApiService
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(private val apiService: ApiService) {
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser() = auth.currentUser

    fun signOut() = auth.signOut()

    fun canCurrentUserChangePassword(): Boolean {
        return getCurrentUser()?.providerData?.any { provider ->
            provider.providerId == EmailAuthProvider.PROVIDER_ID
        } == true
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = getCurrentUser() ?: throw IllegalStateException("No signed-in user")
        val email = user.email ?: throw IllegalStateException("No email for current user")
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    suspend fun loginWithEmail(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).await()
    }

    suspend fun loginWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    suspend fun register(username: String, email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass).await()
        val user = getCurrentUser()
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        user?.updateProfile(profileUpdates)?.await()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun deleteCurrentUser() {
        getCurrentUser()?.delete()?.await()
        signOut()
    }

    suspend fun checkServerHealth() {
        apiService.checkHealth(ApiService.getLanguageHeader())
    }

    suspend fun syncUserWithBackend(): UserResponse {
        val token = ApiService.getAuthHeader()
        val lang = ApiService.getLanguageHeader()
        val user = getCurrentUser()
        return apiService.syncUser(
            token,
            lang,
            UserRegisterRequest(user?.displayName, user?.email)
        )
    }

    suspend fun syncUserAfterManualRegister(displayName: String, email: String): UserResponse {
        val token = ApiService.getAuthHeader()
        val lang = ApiService.getLanguageHeader()
        return apiService.syncUser(
            token,
            lang,
            UserRegisterRequest(displayName, email)
        )
    }
}
