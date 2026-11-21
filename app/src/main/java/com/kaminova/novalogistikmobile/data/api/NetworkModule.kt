package com.kaminova.novalogistikmobile.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kaminova.novalogistikmobile.data.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkException(
    override val message: String,
    val statusCode: Int = 0,
    val isAmbiguous: Boolean = false,
    val errors: Map<String, List<String>>? = null
) : Exception(message)

object NetworkModule {
    private const val DEFAULT_BASE_URL = "https://api.logistik.kaminova.id/api/v1/"
    private val gson = Gson()

    fun createApiService(
        sessionManager: SessionManager,
        baseUrl: String = DEFAULT_BASE_URL,
        onUnauthorized: () -> Unit
    ): NovaLogistikApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authInterceptor = AuthInterceptor(sessionManager, onUnauthorized)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 120s for photo uploads
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(cleanBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(NovaLogistikApiService::class.java)
    }

    fun parseError(exception: Throwable): NetworkException {
        if (exception is HttpException) {
            val code = exception.code()
            val errorBody = exception.response()?.errorBody()?.string()
            var message = "Permintaan gagal (HTTP $code)."
            var errorMap: Map<String, List<String>>? = null

            if (!errorBody.isNullOrBlank()) {
                runCatching {
                    val json = gson.fromJson(errorBody, JsonObject::class.java)
                    if (json.has("message") && !json.get("message").isJsonNull) {
                        message = json.get("message").asString
                    }
                    if (json.has("errors") && json.get("errors").isJsonObject) {
                        val obj = json.getAsJsonObject("errors")
                        val map = mutableMapOf<String, List<String>>()
                        obj.keySet().forEach { key ->
                            val elem = obj.get(key)
                            if (elem.isJsonArray) {
                                map[key] = elem.asJsonArray.map { it.asString }
                            }
                        }
                        if (map.isNotEmpty()) errorMap = map
                    }
                }
            }

            if (code == 401) {
                message = "Sesi Anda berakhir. Silakan login kembali."
            }

            return NetworkException(
                message = message,
                statusCode = code,
                isAmbiguous = code >= 500,
                errors = errorMap
            )
        } else if (exception is IOException) {
            return NetworkException(
                message = "Tidak dapat menghubungi server. Periksa koneksi internet Anda.",
                statusCode = 0,
                isAmbiguous = true
            )
        }
        return NetworkException(
            message = exception.message ?: "Terjadi kesalahan yang tidak terduga.",
            statusCode = 0,
            isAmbiguous = false
        )
    }
}
