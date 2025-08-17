package com.carslab.crm.production.modules.vehicles.domain.repository

import com.carslab.crm.production.modules.vehicles.domain.model.Vehicle
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VehicleRepository {
    fun save(vehicle: Vehicle): Vehicle
    fun findById(id: VehicleId): Vehicle?
    fun findByCompanyId(companyId: Long, pageable: Pageable): Page<Vehicle>
    fun findByLicensePlate(licensePlate: String, companyId: Long): Vehicle?
    fun findByVin(vin: String, companyId: Long): Vehicle?
    fun existsByLicensePlate(licensePlate: String, companyId: Long): Boolean
    fun existsByVin(vin: String, companyId: Long): Boolean
    fun deleteById(id: VehicleId): Boolean
    fun searchVehicles(companyId: Long, searchCriteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle>
    fun existsByIdAndCompanyId(vehicleId: VehicleId, companyId: Long): Boolean
    fun findAllById(vehiclesIds: List<VehicleId>): List<Vehicle>
}

data class VehicleSearchCriteria(
    val make: String? = null,
    val model: String? = null,
    val licensePlate: String? = null,
    val vin: String? = null,
    val year: Int? = null,
    val ownerName: String? = null,
    val minVisits: Int? = null,
    val maxVisits: Int? = null
)