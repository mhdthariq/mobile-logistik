package com.kaminova.novalogistikmobile.data.repository

import com.kaminova.novalogistikmobile.data.api.NovaLogistikApiService
import com.kaminova.novalogistikmobile.data.api.LoginRequest
import com.kaminova.novalogistikmobile.data.api.NetworkModule
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.data.session.StoredSession

class AuthRepository(
    private val apiService: NovaLogistikApiService,
    private val sessionManager: SessionManager
) {
    suspend fun login(corpId: String?, username: String, password: String): Result<StoredSession> {
        return try {
            val response = apiService.login(
                request = LoginRequest(username = username.trim().lowercase(), password = password),
                corpId = corpId?.takeIf { it.isNotBlank() }
            )
            val loginData = response.data
            val session = StoredSession(
                token = loginData.token,
                corpId = corpId?.takeIf { it.isNotBlank() },
                user = loginData.user,
                companyName = loginData.company?.name ?: "Nova Logistik",
                expiresAt = loginData.expiresAt
            )
            sessionManager.saveSession(session)
            Result.success(session)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Failure on server logout must never prevent local session clearing
        } finally {
            sessionManager.clearSession()
        }
    }
}
