package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.ServiceHistory
import com.carslab.crm.domain.model.ServiceHistoryId
import com.carslab.crm.clients.domain.model.VehicleId

interface ServiceHistoryRepository {
    fun save(serviceHistory: ServiceHistory): ServiceHistory
    fun findAll(): List<ServiceHistory>
    fun findById(id: ServiceHistoryId): ServiceHistory?
    fun findByVehicleId(vehicleId: VehicleId): List<ServiceHistory>
    fun deleteById(id: ServiceHistoryId): Boolean
}