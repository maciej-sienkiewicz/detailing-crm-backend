package com.carslab.crm.modules.clients.domain.model

import CustomFormatter
import DisplayName
import IgnoreComparison
import com.carslab.crm.modules.clients.domain.model.shared.AuditInfo
import com.carslab.crm.domain.model.events.VehicleEvent
import java.time.LocalDateTime
import java.math.BigDecimal

data class VehicleId(val value: Long) {
    companion object {
        fun generate(): VehicleId = VehicleId(System.currentTimeMillis())
        fun of(value: Long): VehicleId = VehicleId(value)
    }
}

data class CreateVehicle(
    val make: String,
    val model: String,
    val year: Int?,
    val licensePlate: String,
    val color: String? = null,
    val vin: String? = null,
    val mileage: Long? = null,
    val serviceInfo: VehicleServiceInfo = VehicleServiceInfo(),
    val audit: AuditInfo = AuditInfo()
)

data class Vehicle(
    @IgnoreComparison
    val id: VehicleId,

    @DisplayName("Marka")
    val make: String,

    @DisplayName("Model")
    val model: String,

    @DisplayName("Rok produkcji")
    val year: Int?,

    @DisplayName("Numer rejestracyjny")
    val licensePlate: String,

    @DisplayName("Kolor")
    val color: String? = null,

    @DisplayName("Numer VIN")
    val vin: String? = null,

    @DisplayName("Przebieg")
    val mileage: Long? = null,

    @DisplayName("Informacje serwisowe")
    val serviceInfo: VehicleServiceInfo = VehicleServiceInfo(),

    @DisplayName("Informacje audytowe")
    val audit: AuditInfo = AuditInfo(),

    @DisplayName("Właściciele")
    val owners: List<ClientId> = emptyList()
) {
    val displayName: String get() = "$make $model ($licensePlate)"

    // Event sourcing preparation
    fun apply(event: VehicleEvent): Vehicle {
        return when (event) {
            is VehicleEvent.VehicleCreated -> this
            is VehicleEvent.VehicleUpdated -> copy(
                make = event.make,
                model = event.model,
                year = event.year,
                licensePlate = event.licensePlate,
                color = event.color,
                vin = event.vin,
                mileage = event.mileage,
                audit = audit.updated()
            )
            is VehicleEvent.ServiceRecorded -> copy(
                serviceInfo = serviceInfo.addService(event.serviceDate, event.amount)
            )
            is VehicleEvent.VehicleDeleted -> this // Handle in aggregate
        }
    }
}

data class VehicleServiceInfo(
    @DisplayName("Łączna liczba usług")
    val totalServices: Int = 0,

    @DisplayName("Data ostatniego serwisu")
    val lastServiceDate: LocalDateTime? = null,

    @DisplayName("Łączne wydatki")
    val totalSpent: BigDecimal = BigDecimal.ZERO
) {
    fun addService(serviceDate: LocalDateTime, amount: BigDecimal): VehicleServiceInfo {
        return copy(
            totalServices = totalServices + 1,
            lastServiceDate = serviceDate,
            totalSpent = totalSpent + amount
        )
    }
}

data class VehicleWithStatistics(
    val vehicle: Vehicle,
    val statistics: VehicleStatistics,
    val owners: List<ClientSummary> = emptyList()
)

data class VehicleStatistics(
    val vehicleId: Long,
    val visitCount: Long = 0,
    val totalRevenue: BigDecimal = BigDecimal.ZERO,
    val lastVisitDate: LocalDateTime? = null
)