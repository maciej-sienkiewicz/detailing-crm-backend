package com.carslab.crm.modules.email.infrastructure.services

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.email.domain.model.ProtocolEmailData
import com.carslab.crm.modules.email.domain.model.ProtocolServiceData
import com.carslab.crm.modules.email.domain.ports.ProtocolDataProvider
import com.carslab.crm.modules.visits.infrastructure.persistence.read.ProtocolReadRepositoryDeprecated
import com.carslab.crm.modules.visits.domain.services.VisitValidationServiceDeprecated
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProtocolDataProviderImpl(
    private val protocolReadRepository: ProtocolReadRepositoryDeprecated,
    private val visitValidationServiceDeprecated: VisitValidationServiceDeprecated
) : ProtocolDataProvider {

    private val logger = LoggerFactory.getLogger(ProtocolDataProviderImpl::class.java)

    override fun getProtocolData(protocolId: String): ProtocolEmailData? {
        try {
            val access = visitValidationServiceDeprecated.validateVisitAccess(ProtocolId(protocolId))
            if(!access.isValid) {
                logger.warn("Access validation failed for protocolId: $protocolId")
                throw IllegalArgumentException("Access denied for visitId: $protocolId")
            }
            
            val protocol = protocolReadRepository.findDetailById(protocolId)
                ?: return null

            val services = protocol.services.map { service ->
                ProtocolServiceData(
                    name = service.name,
                    quantity = service.quantity,
                    price = service.basePrice,
                    finalPrice = service.finalPrice
                )
            }

            val totalAmount = services.sumOf { it.finalPrice }

            return ProtocolEmailData(
                protocolId = protocol.id,
                clientName = protocol.client.name,
                clientEmail = protocol.client.email ?: "",
                companyName = protocol.client.companyName,
                vehicleMake = protocol.vehicle.make,
                vehicleModel = protocol.vehicle.model,
                licensePlate = protocol.vehicle.licensePlate,
                servicePeriod = "${protocol.period.startDate} - ${protocol.period.endDate}",
                status = protocol.status,
                services = services,
                totalAmount = totalAmount,
                notes = protocol.notes
            )
        } catch (e: Exception) {
            logger.error("Error getting protocol data for protocolId: $protocolId", e)
            return null
        }
    }
}