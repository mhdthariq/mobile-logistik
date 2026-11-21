package com.kaminova.novalogistikmobile.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NovaLogistikApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest,
        @Header("x-corp-id") corpId: String? = null
    ): ApiEnvelope<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): ApiEnvelope<Map<String, Any>>

    @GET("dashboard/driver")
    suspend fun getDriverDashboard(
        @Query("from") from: String,
        @Query("to") to: String
    ): ApiEnvelope<DriverDashboardData>

    @GET("transports")
    suspend fun getMyTransports(
        @Query("mine") mine: Boolean = true,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): ApiListEnvelope<TransportListItem>

    @GET("transports")
    suspend fun getMyTransportHistory(
        @Query("mine") mine: Boolean = true,
        @Query("history") history: Boolean = true
    ): ApiListEnvelope<TransportListItem>

    @GET("transports/{id}")
    suspend fun getTransportDetail(
        @Path("id") id: Long
    ): ApiEnvelope<TransportDetail>

    @POST("transports/{id}/depart")
    suspend fun departTransport(
        @Path("id") id: Long
    ): ApiEnvelope<TransportStatusUpdate>

    @POST("transports/{id}/checkins")
    suspend fun checkinAtCheckpoint(
        @Path("id") id: Long,
        @Body request: CheckinRequest
    ): ApiEnvelope<CheckinResponse>

    @GET("transports/{id}/activities")
    suspend fun getActivities(
        @Path("id") id: Long
    ): ApiListEnvelope<TransportActivityRow>

    @POST("transports/{id}/activities")
    suspend fun reportActivity(
        @Path("id") id: Long,
        @Body request: ActivityReportRequest
    ): ApiEnvelope<TransportActivityRow>

    @POST("transports/{id}/arrive")
    suspend fun arriveTransport(
        @Path("id") id: Long,
        @Body request: ArriveRequest
    ): ApiEnvelope<TransportStatusUpdate>

    @GET("transports/{id}/expenses")
    suspend fun getExpenses(
        @Path("id") id: Long
    ): ApiListEnvelope<TransportExpenseRow>

    @GET("transports/{id}/expenses/{expenseId}")
    suspend fun getExpense(@Path("id") id: Long, @Path("expenseId") expenseId: Long): ApiEnvelope<TransportExpenseRow>

    @POST("transports/{id}/expenses")
    suspend fun createExpense(
        @Path("id") id: Long,
        @Body request: CreateExpenseRequest
    ): ApiEnvelope<TransportExpenseRow>

    @GET("transports/{id}/drops")
    suspend fun getDropsBoard(
        @Path("id") id: Long
    ): ApiEnvelope<DropsBoard>

    @POST("transports/{id}/drops/{shipmentId}")
    suspend fun dropShipment(
        @Path("id") id: Long,
        @Path("shipmentId") shipmentId: Long,
        @Body request: DropShipmentActionRequest = DropShipmentActionRequest()
    ): ApiEnvelope<DropShipmentActionResponse>

    @GET("mobile/config")
    suspend fun getMobileConfig(): ApiEnvelope<MobileConfig>
}
