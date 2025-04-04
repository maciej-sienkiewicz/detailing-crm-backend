package com.carslab.crm.api.model.commands

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.VehicleStats
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mapper dla modeli DTO związanych z pojazdami.
 */
object VehicleDtoMapper {
    private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE
    private val DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    /**
     * Konwertuje komendę tworzenia pojazdu na model domenowy.
     */
    fun fromCreateCommand(command: CreateVehicleCommand): Vehicle {
        val now = LocalDateTime.now()

        return Vehicle(
            id = VehicleId.generate(),
            make = command.make,
            model = command.model,
            year = command.year,
            licensePlate = command.licensePlate,
            color = command.color,
            vin = command.vin,
            totalServices = 0, // Domyślnie dla nowego pojazdu
            lastServiceDate = null, // Domyślnie dla nowego pojazdu
            totalSpent = 0.0, // Domyślnie dla nowego pojazdu,
            mileage = command.mileage,
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje komendę aktualizacji pojazdu na model domenowy.
     */
    fun fromUpdateCommand(command: UpdateVehicleCommand, existingVehicle: Vehicle? = null): Vehicle {
        val now = LocalDateTime.now()

        // Zachowanie danych audytowych i statystycznych
        val createdAt = existingVehicle?.audit?.createdAt ?: now
        val totalServices = existingVehicle?.totalServices ?: 0
        val lastServiceDate = existingVehicle?.lastServiceDate
        val totalSpent = existingVehicle?.totalSpent ?: 0.0

        return Vehicle(
            id = VehicleId(command.id.toLong()),
            make = command.make,
            model = command.model,
            year = command.year,
            licensePlate = command.licensePlate,
            color = command.color,
            vin = command.vin,
            totalServices = totalServices,
            lastServiceDate = lastServiceDate,
            totalSpent = totalSpent,
            mileage = command.mileage,
            audit = Audit(
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje model domenowy na DTO.
     */
    fun toDto(vehicle: Vehicle): VehicleDto {
        return VehicleDto(
            id = vehicle.id.value.toString(),
            make = vehicle.make ?: "",
            model = vehicle.model ?: "",
            year = vehicle.year ?: 0,
            licensePlate = vehicle.licensePlate ?: "",
            color = vehicle.color,
            vin = vehicle.vin,
            totalServices = vehicle.totalServices,
            lastServiceDate = vehicle.lastServiceDate,
            totalSpent = vehicle.totalSpent,
            createdAt = vehicle.audit.createdAt,
            updatedAt = vehicle.audit.updatedAt
        )
    }

    /**
     * Konwertuje model domenowy pojazdu z statystykami na DTO.
     */
    fun toDto(vehicleWithStats: VehicleWithStats): VehicleDto {
        return VehicleDto(
            id = vehicleWithStats.vehicle.id.value.toString(),
            make = vehicleWithStats.vehicle.make ?: "",
            model = vehicleWithStats.vehicle.model ?: "",
            year = vehicleWithStats.vehicle.year ?: 0,
            licensePlate = vehicleWithStats.vehicle.licensePlate ?: "",
            color = vehicleWithStats.vehicle.color,
            vin = vehicleWithStats.vehicle.vin,
            totalServices = vehicleWithStats.stats.visitNo.toInt(),
            lastServiceDate = vehicleWithStats.vehicle.lastServiceDate,
            totalSpent = vehicleWithStats.stats.gmv.toDouble(),
            createdAt = vehicleWithStats.vehicle.audit.createdAt,
            updatedAt = vehicleWithStats.vehicle.audit.updatedAt
        )
    }

    /**
     * Konwertuje model statystyk pojazdu na DTO.
     */
    fun toStatisticsDto(stats: VehicleStats): VehicleStatisticsDto {
        return VehicleStatisticsDto(
            servicesNo = stats.visitNo,
            totalRevenue = stats.gmv
        )
    }

    /**
     * Konwertuje model właściciela pojazdu na DTO.
     */
    fun toOwnerDto(owner: ClientDetails): VehicleOwnerDto {
        return VehicleOwnerDto(
            ownerId = owner.id.value,
            ownerName = owner.fullName
        )
    }

    /**
     * Konwertuje komendę tworzenia historii serwisowej na model domenowy.
     */
    fun fromCreateHistoryCommand(command: CreateServiceHistoryCommand): ServiceHistory {
        val now = LocalDateTime.now()
        val date = LocalDate.parse(command.date, DATE_FORMATTER)

        return ServiceHistory(
            id = ServiceHistoryId.generate(),
            vehicleId = VehicleId(command.vehicleId?.toLong() ?: throw IllegalArgumentException("Vehicle ID is required")),
            date = date,
            serviceType = command.serviceType,
            description = command.description,
            price = command.price,
            protocolId = command.protocolId,
            audit = Audit(
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje komendę aktualizacji historii serwisowej na model domenowy.
     */
    fun fromUpdateHistoryCommand(command: UpdateServiceHistoryCommand, existingHistory: ServiceHistory? = null): ServiceHistory {
        val now = LocalDateTime.now()
        val date = LocalDate.parse(command.date, DATE_FORMATTER)

        // Zachowanie danych audytowych
        val createdAt = existingHistory?.audit?.createdAt ?: now

        return ServiceHistory(
            id = ServiceHistoryId(command.id),
            vehicleId = VehicleId(command.vehicleId.toLong()),
            date = date,
            serviceType = command.serviceType,
            description = command.description,
            price = command.price,
            protocolId = command.protocolId,
            audit = Audit(
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    /**
     * Konwertuje model historii serwisowej na DTO.
     */
    fun toServiceHistoryDto(serviceHistory: ServiceHistory): ServiceHistoryDto {
        return ServiceHistoryDto(
            id = serviceHistory.id.value,
            vehicleId = serviceHistory.vehicleId.value.toString(),
            date = serviceHistory.date.format(DATE_FORMATTER),
            serviceType = serviceHistory.serviceType,
            description = serviceHistory.description,
            price = serviceHistory.price,
            protocolId = serviceHistory.protocolId,
            createdAt = serviceHistory.audit.createdAt.format(DATETIME_FORMATTER),
            updatedAt = serviceHistory.audit.updatedAt.format(DATETIME_FORMATTER)
        )
    }
}