package com.kaminova.novalogistikmobile.data.api

import com.kaminova.novalogistikmobile.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val onUnauthorized: () -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        sessionManager.currentToken?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }

        sessionManager.currentCorpId?.let { corpId ->
            if (corpId.isNotBlank()) {
                builder.header("x-corp-id", corpId)
            }
        }

        val response = chain.proceed(builder.build())

        if (response.code == 401) {
            // Trigger logout and session clear
            sessionManager.clearSession()
            onUnauthorized()
        }

        return response
    }
}
