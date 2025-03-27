package com.carslab.crm.domain.protocol

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.ClientStatisticsRepository
import com.carslab.crm.domain.port.VehicleRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NewVehicleCreator(
    private val vehicleRepository: VehicleRepository,
    private val clientStatisticsRepository: ClientStatisticsRepository
) {
    fun getVehicle(protocol: CarReceptionProtocol): Vehicle {
        val client = protocol.client
        val vehicle = vehicleRepository.findByVinOrLicensePlate(protocol.vehicle.vin, protocol.vehicle.licensePlate) ?:
        Vehicle(
            id = VehicleId.generate(),
            make = protocol.vehicle.make,
            model = protocol.vehicle.model,
            year = protocol.vehicle.productionYear,
            licensePlate = protocol.vehicle.licensePlate,
            color = protocol.vehicle.color,
            vin = protocol.vehicle.vin,
            totalServices = 0,
            lastServiceDate = LocalDateTime.now(),
            totalSpent = 0.0,
            ownerIds = emptyList(),
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val updated = if(!vehicle.ownerIds.contains(client.id.toString())) {
            vehicle.copy(ownerIds = vehicle.ownerIds.plus(client.id.toString()))
                .also { vehicleRepository.save(it) }
                .also { val clientStats = clientStatisticsRepository.findById(ClientId(protocol.client.id!!))
                    ?: ClientStats(protocol.client.id, 0, "0".toBigDecimal(), 0)
                    val incr = clientStats.copy(
                        vehiclesNo =  clientStats.vehiclesNo + 1
                    )
                    clientStatisticsRepository.save(incr)
                }
        } else vehicle


        return updated
    }
}