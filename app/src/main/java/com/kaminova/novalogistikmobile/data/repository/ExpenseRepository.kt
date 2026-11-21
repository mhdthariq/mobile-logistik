package com.kaminova.novalogistikmobile.data.repository

import com.google.gson.Gson
import com.kaminova.novalogistikmobile.data.api.CreateExpenseRequest
import com.kaminova.novalogistikmobile.data.api.DropShipmentActionRequest
import com.kaminova.novalogistikmobile.data.api.DropShipmentActionResponse
import com.kaminova.novalogistikmobile.data.api.DropsBoard
import com.kaminova.novalogistikmobile.data.api.EvidenceCapture
import com.kaminova.novalogistikmobile.data.api.ExpenseSummary
import com.kaminova.novalogistikmobile.data.api.NovaLogistikApiService
import com.kaminova.novalogistikmobile.data.api.NetworkModule
import com.kaminova.novalogistikmobile.data.api.TransportExpenseRow

class ExpenseRepository(
    private val apiService: NovaLogistikApiService
) {
    private val gson = Gson()

    suspend fun getExpenses(transportId: Long): Result<Pair<List<TransportExpenseRow>, ExpenseSummary?>> {
        return try {
            val response = apiService.getExpenses(transportId)
            val summary = response.meta?.get("summary")?.let { raw ->
                runCatching {
                    val json = gson.toJsonTree(raw)
                    gson.fromJson(json, ExpenseSummary::class.java)
                }.getOrNull()
            }
            Result.success(Pair(response.data, summary))
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getExpensePhotos(transportId: Long, expenseId: Long): Result<List<String>> =
        try {
            val expense = apiService.getExpense(transportId, expenseId).data
            Result.success((listOfNotNull(expense.photoUrl) + expense.photoUrls.orEmpty()).filter(String::isNotBlank).distinct())
        } catch (e: Exception) {
            Result.failure(NetworkModule.parseError(e))
        }

    suspend fun createExpense(
        transportId: Long,
        type: String,
        amount: Double,
        description: String?,
        photos: List<String>,
        capture: EvidenceCapture?
    ): Result<TransportExpenseRow> {
        return try {
            val request = CreateExpenseRequest(
                type = type,
                amount = amount,
                description = description?.takeIf { it.isNotBlank() },
                photos = photos,
                latitude = capture?.latitude,
                longitude = capture?.longitude,
                locationAccuracy = capture?.locationAccuracy,
                capturedAt = capture?.capturedAt,
                timeZone = capture?.timeZone,
                locationLabel = capture?.locationLabel,
                mockLocation = capture?.mockLocation
            )
            val response = apiService.createExpense(transportId, request)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getDropsBoard(transportId: Long): Result<DropsBoard> {
        return try {
            val response = apiService.getDropsBoard(transportId)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun dropShipment(transportId: Long, shipmentId: Long): Result<DropShipmentActionResponse> {
        return try {
            val response = apiService.dropShipment(
                id = transportId,
                shipmentId = shipmentId,
                request = DropShipmentActionRequest(action = "DROP")
            )
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }
}
