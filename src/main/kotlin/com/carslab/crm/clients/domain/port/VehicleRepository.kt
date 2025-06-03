package com.carslab.crm.clients.domain.port

import com.carslab.crm.clients.domain.model.CreateVehicle
import com.carslab.crm.clients.domain.model.Vehicle
import com.carslab.crm.clients.domain.model.VehicleId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface VehicleRepository {
    fun save(vehicle: Vehicle): Vehicle
    fun save(vehicle: CreateVehicle): Vehicle
    fun findById(id: VehicleId): Vehicle?
    fun findByIds(ids: List<VehicleId>): List<Vehicle>
    fun findAll(pageable: Pageable): Page<Vehicle>
    fun findByLicensePlate(licensePlate: String): Vehicle?
    fun findByVin(vin: String): Vehicle?
    fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle?
    fun searchVehicles(criteria: VehicleSearchCriteria, pageable: Pageable): Page<Vehicle>
    fun existsById(id: VehicleId): Boolean
    fun deleteById(id: VehicleId): Boolean
    fun count(): Long
}

data class VehicleSearchCriteria(
    val make: String? = null,
    val model: String? = null,
    val licensePlate: String? = null,
    val vin: String? = null,
    val year: Int? = null,
    val hasOwners: Boolean? = null
)