package com.carslab.crm.modules.email.infrastructure.services

import com.carslab.crm.modules.email.domain.model.ProtocolEmailData
import com.carslab.crm.modules.email.domain.model.ProtocolServiceData
import com.carslab.crm.modules.email.domain.ports.ProtocolDataProvider
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProtocolDataProviderImpl(
    private val visitDetailQueryService: VisitDetailQueryService,
) : ProtocolDataProvider {

    private val logger = LoggerFactory.getLogger(ProtocolDataProviderImpl::class.java)

    override fun getProtocolData(protocolId: String, authContext: AuthContext?): ProtocolEmailData? {
        try {

            val protocol = visitDetailQueryService.getSimpleDetails(protocolId, authContext)

            val services = protocol.services.map { service ->
                ProtocolServiceData(
                    name = service.name,
                    quantity = service.quantity,
                    price = service.finalPriceBrutto.toDouble(),
                    finalPrice = service.finalPriceBrutto.toDouble()
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