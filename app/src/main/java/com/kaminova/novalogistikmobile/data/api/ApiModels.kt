package com.kaminova.novalogistikmobile.data.api

import com.google.gson.annotations.SerializedName

// Generic API response envelope: { "data": T, "meta": ... }
data class ApiEnvelope<T>(
    @SerializedName("data") val data: T,
    @SerializedName("meta") val meta: Map<String, Any>? = null
)

data class ApiListEnvelope<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("meta") val meta: Map<String, Any>? = null
)

// Auth
data class AuthRole(
    val id: Long,
    val slug: String,
    val name: String
)

data class AuthUser(
    val id: Long,
    val username: String,
    val name: String,
    val isOwner: Boolean = false,
    val isActive: Boolean = true,
    val employeeId: Long? = null,
    val warehouseId: Long? = null,
    val warehouseName: String? = null,
    val partnerId: Long? = null,
    val partnerType: String? = null,
    val roles: List<AuthRole> = emptyList(),
    val permissions: List<String> = emptyList()
)

data class CompanyInfo(
    val name: String
)

data class LoginResponse(
    val token: String,
    val expiresAt: String,
    val user: AuthUser,
    val company: CompanyInfo?
)

data class LoginRequest(
    val username: String,
    val password: String
)

// Transports
data class Checkpoint(
    val id: Long,
    val name: String,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val isActive: Boolean? = true
)

data class NextCheckpoint(
    val id: Long,
    val name: String,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

data class DriverTransportSummary(
    val id: Long,
    val transportCode: String,
    val trackingMode: String, // CHECKPOINT or ACTIVITY
    val status: String,       // PLANNED, DEPARTED, ARRIVED, COMPLETED, CANCELLED
    val routeName: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val plannedDepartureAt: String? = null,
    val plannedArrivalAt: String? = null,
    val departedAt: String? = null,
    val arrivedAt: String? = null,
    val vehicleNumber: String,
    val vehicleName: String? = null,
    val maxWeightKg: Double? = null,
    val maxVolumeM3: Double? = null,
    val driverName: String? = null,
    val kenekName: String? = null,
    val shipmentCount: Int = 0,
    val packageCount: Int = 0,
    val totalWeightKg: Double = 0.0,
    val totalVolumeM3: Double = 0.0,
    val totalPrice: Double? = null,
    val checkpointsTotal: Int = 0,
    val checkpointsCheckedIn: Int = 0,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val lastLocationAt: String? = null,
    val lastCheckpointName: String? = null,
    val lastCheckinAt: String? = null,
    val lastCheckinBy: String? = null,
    val nextCheckpoint: NextCheckpoint? = null
)

data class DriverDashboardData(
    val period: Map<String, String>? = null,
    val current: DriverTransportSummary? = null,
    val upcoming: List<DriverTransportSummary> = emptyList(),
    val future: List<DriverTransportSummary> = emptyList(),
    val completedCount: Int = 0,
    val history: List<DriverTransportSummary> = emptyList(),
    val totalTransportsAllTime: Int = 0,
    val note: String? = null
)

data class TransportListItem(
    val id: Long,
    val transportCode: String,
    val status: String,
    val trackingMode: String,
    val transportMode: String,
    val routeId: Long? = null,
    val routeName: String? = null,
    val vehicleId: Long? = null,
    val vehicleNumber: String = "",
    val vehicleName: String? = null,
    val driverName: String? = null,
    val kenekName: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val plannedDepartureAt: String? = null,
    val plannedArrivalAt: String? = null,
    val departedAt: String? = null,
    val arrivedAt: String? = null,
    val createdAt: String? = null,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val shipmentCount: Int = 0,
    val totalWeightKg: Double = 0.0,
    val totalVolumeM3: Double = 0.0,
    val totalPrice: Double? = null,
    val totalKoli: Int = 0
)

data class VehicleDetail(
    val id: Long,
    val vehicleNumber: String,
    val name: String? = null,
    val status: String? = null,
    val plateColor: String? = null,
    val wheelCount: Int? = null,
    val maxWeightKg: Double? = null,
    val maxVolumeM3: Double? = null,
    val maxKoli: Int? = null
)

data class UserSummary(
    val id: Long,
    val name: String,
    val phone: String? = null
)

data class CheckpointRecord(
    val id: Long,
    val checkpointId: Long,
    val checkpointName: String,
    val checkpointSequence: Int,
    val latitude: Double,
    val longitude: Double,
    val withinRadius: Boolean = false,
    val distanceMeters: Double? = null,
    val photoUrl: String? = null,
    val recordedBy: UserSummary? = null,
    val recordedAt: String
)

data class TransportShipmentRow(
    val id: Long,
    val masterCode: String,
    val resi: String? = null,
    val status: String,
    val origin: String? = null,
    val destination: String? = null,
    val customerName: String? = null,
    val penerimaName: String? = null,
    val arrivedWarehouseName: String? = null,
    val destinationWarehouseName: String? = null,
    val packages: Int = 0,
    val weightKg: Double = 0.0,
    val volumeM3: Double = 0.0,
    val priceAmount: Double? = null
)

data class TransportDetail(
    val id: Long,
    val transportCode: String,
    val status: String,
    val trackingMode: String,
    val transportMode: String,
    val routeId: Long? = null,
    val routeName: String? = null,
    val routeOrigin: String? = null,
    val routeDestination: String? = null,
    val checkpoints: List<Checkpoint> = emptyList(),
    val origin: String? = null,
    val destination: String? = null,
    val plannedDepartureAt: String? = null,
    val plannedArrivalAt: String? = null,
    val departedAt: String? = null,
    val arrivedAt: String? = null,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val vehicle: VehicleDetail,
    val driver: UserSummary? = null,
    val kenek: UserSummary? = null,
    val warehouseStaff: UserSummary? = null,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val lastLocationAt: String? = null,
    val checkpointRecords: List<CheckpointRecord> = emptyList(),
    val shipments: List<TransportShipmentRow> = emptyList(),
    val shipmentCount: Int = 0,
    val totalWeightKg: Double = 0.0,
    val totalVolumeM3: Double = 0.0,
    val totalPrice: Double? = null,
    val totalKoli: Int = 0,
    val capacity: Map<String, Any>? = null
)

data class TransportStatusUpdate(
    val status: String,
    val departedAt: String? = null,
    val arrivedAt: String? = null,
    val updatedAt: String? = null
) {
    fun applyTo(transport: TransportDetail): TransportDetail = transport.copy(
        status = status,
        departedAt = departedAt ?: transport.departedAt,
        arrivedAt = arrivedAt ?: transport.arrivedAt,
        updatedAt = updatedAt ?: transport.updatedAt
    )
}

// Evidence & Checkins
data class EvidenceCapture(
    val photoDataUrl: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Double? = null,
    val capturedAt: String, // ISO timestamp
    val locationLabel: String? = null,
    val timeZone: String,   // Asia/Jakarta, Asia/Makassar, Asia/Jayapura
    val mockLocation: Boolean? = null
)

data class CheckinRequest(
    val checkpointId: Long,
    val photo: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Double? = null,
    val capturedAt: String? = null,
    val timeZone: String? = null,
    val locationLabel: String? = null,
    val mockLocation: Boolean? = null
)

data class CheckinRecordSummary(
    val id: Long,
    val checkpointId: Long,
    val checkpointName: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val withinRadius: Boolean,
    val recordedAt: String
)

data class CheckinResponse(
    val record: CheckinRecordSummary,
    val distanceKm: String? = null,
    val radiusKm: String? = null,
    val autoArrived: Boolean = false,
    val message: String
)

data class ActivityReportRequest(
    val photo: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Double? = null,
    val capturedAt: String,
    val timeZone: String,
    val locationLabel: String? = null,
    val mockLocation: Boolean? = null
)

data class TransportActivityRow(
    val id: Long,
    val transportId: Long,
    val photoUrl: String,
    val label: String? = null,
    val latitude: Double,
    val longitude: Double,
    val locationAccuracy: Double? = null,
    val capturedAt: String,
    val locationLabel: String? = null,
    val timeZone: String? = null,
    val mockLocation: Boolean? = null,
    val recordedById: Long,
    val recordedByUsername: String? = null,
    val recordedAt: String
)

data class ArriveRequest(
    val photo: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Double? = null,
    val capturedAt: String? = null,
    val timeZone: String? = null,
    val locationLabel: String? = null,
    val mockLocation: Boolean? = null
)

// Expenses
data class TransportExpenseRow(
    val id: Long,
    val expenseCode: String,
    val transportId: Long,
    val type: String, // BBM, TOL, PARKIR, BONGKAR, ANGKUT, MAKAN
    val amount: Double,
    val description: String? = null,
    val status: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Double? = null,
    val locationLabel: String? = null,
    val timeZone: String? = null,
    val capturedAt: String? = null,
    val mockLocation: Boolean? = null,
    val createdById: Long,
    val createdAt: String,
    val updatedAt: String? = null,
    val hasPhoto: Boolean = false,
    val photoCount: Int = 0,
    val photoUrl: String? = null,
    val photoUrls: List<String>? = null
)

data class ExpenseTypeSummary(
    val count: Int = 0,
    val total: Double = 0.0
)

data class ExpenseSummary(
    val total: Double = 0.0,
    val byType: Map<String, ExpenseTypeSummary> = emptyMap()
)

data class CreateExpenseRequest(
    val type: String,
    val amount: Double,
    val description: String? = null,
    val photos: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Double? = null,
    val capturedAt: String? = null,
    val timeZone: String? = null,
    val locationLabel: String? = null,
    val mockLocation: Boolean? = null
)

// Multi-drop
data class DropShipment(
    val shipmentId: Long,
    val masterCode: String,
    val customerName: String? = null,
    val destination: String? = null,
    val penerimaName: String? = null,
    val dropCheckpointId: Long? = null,
    val effectiveDropCheckpointId: Long? = null,
    val dropStatus: String, // LOADED, AT_DROP_POINT, DROPPED, DELIVERY_PENDING, DELIVERY_APPROVED
    val droppedAt: String? = null,
    val deliveryApprovedAt: String? = null
)

data class DropBoardCheckpoint(
    val id: Long,
    val name: String,
    val sequence: Int,
    val checkedIn: Boolean = false,
    val isFinal: Boolean = false
)

data class DropGroup(
    val checkpointId: Long? = null,
    val shipmentIds: List<Long> = emptyList()
)

data class DropProgress(
    val total: Int = 0,
    val dropped: Int = 0
)

data class LoadSummaryItem(
    val totalActualWeightKg: Double = 0.0,
    val totalVolumeM3: Double = 0.0,
    val totalKoli: Int = 0
)

data class DropsLoadSummary(
    val initialLoad: LoadSummaryItem = LoadSummaryItem(),
    val remainingLoad: LoadSummaryItem = LoadSummaryItem(),
    val droppedLoad: LoadSummaryItem = LoadSummaryItem()
)

data class DropsBoard(
    val trackingMode: String,
    val transportMode: String,
    val transportStatus: String,
    val deliveryApprovedAt: String? = null,
    val checkpoints: List<DropBoardCheckpoint> = emptyList(),
    val shipments: List<DropShipment> = emptyList(),
    val groups: List<DropGroup> = emptyList(),
    val progress: DropProgress = DropProgress(),
    val vehicleEmpty: Boolean = false,
    val loadSummary: DropsLoadSummary = DropsLoadSummary()
)

data class DropShipmentActionRequest(
    val action: String = "DROP"
)

data class DropShipmentActionResponse(
    val shipmentId: Long,
    val dropStatus: String,
    val checkpointName: String? = null
)

// Mobile Config
data class MobileAndroidConfig(
    val minimumVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
    val distribution: String = "private"
)

data class MobileEvidenceConfig(
    val timeZones: List<String> = emptyList(),
    val defaultTimeZone: String = "Asia/Jakarta",
    val maxLocationLabelLength: Int = 240,
    val maxPhotoDataUrlLength: Int = 2500000,
    val maxCaptureAgeSeconds: Long = 300,
    val maxFutureSkewSeconds: Long = 30,
    val serverWatermark: Boolean = false,
    val locationLabelSource: String = "client"
)

data class MobileCapabilities(
    val offlineCapture: Boolean = false,
    val idempotentMutations: Boolean = false,
    val refreshSessions: Boolean = false,
    val pushNotifications: Boolean = false,
    val mockLocationMetadata: Boolean = true
)

data class MobileConfig(
    val serverTime: String,
    val android: MobileAndroidConfig? = null,
    val evidence: MobileEvidenceConfig = MobileEvidenceConfig(),
    val capabilities: MobileCapabilities = MobileCapabilities()
)
