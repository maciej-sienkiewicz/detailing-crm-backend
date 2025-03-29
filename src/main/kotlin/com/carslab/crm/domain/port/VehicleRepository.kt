package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.Vehicle
import com.carslab.crm.domain.model.VehicleId

interface VehicleRepository {
    fun save(vehicle: Vehicle): Vehicle
    fun findAll(): List<Vehicle>
    fun findById(id: VehicleId): Vehicle?
    fun findByIds(ids: List<VehicleId>): List<Vehicle>
    fun deleteById(id: VehicleId): Boolean
    fun findByVinOrLicensePlate(vin: String?, licensePlate: String?): Vehicle?
}