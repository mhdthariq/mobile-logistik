package com.kaminova.novalogistikmobile.data.repository

import com.kaminova.novalogistikmobile.data.api.ActivityReportRequest
import com.kaminova.novalogistikmobile.data.api.ArriveRequest
import com.kaminova.novalogistikmobile.data.api.CheckinRequest
import com.kaminova.novalogistikmobile.data.api.CheckinResponse
import com.kaminova.novalogistikmobile.data.api.DriverDashboardData
import com.kaminova.novalogistikmobile.data.api.EvidenceCapture
import com.kaminova.novalogistikmobile.data.api.NovaLogistikApiService
import com.kaminova.novalogistikmobile.data.api.MobileConfig
import com.kaminova.novalogistikmobile.data.api.NetworkModule
import com.kaminova.novalogistikmobile.data.api.TransportActivityRow
import com.kaminova.novalogistikmobile.data.api.TransportDetail
import com.kaminova.novalogistikmobile.data.api.TransportStatusUpdate
import com.kaminova.novalogistikmobile.data.api.TransportListItem

class DriverRepository(
    private val apiService: NovaLogistikApiService
) {
    suspend fun getDashboard(from: String, to: String): Result<DriverDashboardData> {
        return try {
            val response = apiService.getDriverDashboard(from, to)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getMyTransports(status: String? = null, search: String? = null): Result<List<TransportListItem>> {
        return try {
            val response = apiService.getMyTransports(mine = true, status = status, search = search)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getTransportHistory(): Result<List<TransportListItem>> {
        return try {
            val response = apiService.getMyTransportHistory(mine = true, history = true)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getTransportDetail(id: Long): Result<TransportDetail> {
        return try {
            val response = apiService.getTransportDetail(id)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun departTransport(id: Long): Result<TransportStatusUpdate> {
        return try {
            val response = apiService.departTransport(id)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun checkinAtCheckpoint(
        transportId: Long,
        checkpointId: Long,
        capture: EvidenceCapture
    ): Result<CheckinResponse> {
        return try {
            val request = CheckinRequest(
                checkpointId = checkpointId,
                photo = capture.photoDataUrl,
                latitude = capture.latitude,
                longitude = capture.longitude,
                locationAccuracy = capture.locationAccuracy,
                capturedAt = capture.capturedAt,
                timeZone = capture.timeZone,
                locationLabel = capture.locationLabel,
                mockLocation = capture.mockLocation
            )
            val response = apiService.checkinAtCheckpoint(transportId, request)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getActivities(transportId: Long): Result<List<TransportActivityRow>> {
        return try {
            val response = apiService.getActivities(transportId)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun reportActivity(
        transportId: Long,
        capture: EvidenceCapture
    ): Result<TransportActivityRow> {
        return try {
            val request = ActivityReportRequest(
                photo = capture.photoDataUrl,
                latitude = capture.latitude,
                longitude = capture.longitude,
                locationAccuracy = capture.locationAccuracy,
                capturedAt = capture.capturedAt,
                timeZone = capture.timeZone,
                locationLabel = capture.locationLabel,
                mockLocation = capture.mockLocation
            )
            val response = apiService.reportActivity(transportId, request)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun arriveTransport(
        transportId: Long,
        capture: EvidenceCapture?
    ): Result<TransportStatusUpdate> {
        return try {
            val request = ArriveRequest(
                photo = capture?.photoDataUrl,
                latitude = capture?.latitude,
                longitude = capture?.longitude,
                locationAccuracy = capture?.locationAccuracy,
                capturedAt = capture?.capturedAt,
                timeZone = capture?.timeZone,
                locationLabel = capture?.locationLabel,
                mockLocation = capture?.mockLocation
            )
            val response = apiService.arriveTransport(transportId, request)
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }

    suspend fun getMobileConfig(): Result<MobileConfig> {
        return try {
            val response = apiService.getMobileConfig()
            Result.success(response.data)
        } catch (e: Throwable) {
            Result.failure(NetworkModule.parseError(e))
        }
    }
}
