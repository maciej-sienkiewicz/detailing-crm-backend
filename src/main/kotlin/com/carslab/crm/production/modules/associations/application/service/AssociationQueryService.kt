package com.carslab.crm.production.modules.associations.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.associations.application.dto.AssociationResponse
import com.carslab.crm.production.modules.associations.domain.service.AssociationDomainService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AssociationQueryService(
    private val associationDomainService: AssociationDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(AssociationQueryService::class.java)

    fun getClientVehicles(clientId: String): List<VehicleId> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting vehicles for client: {} in company: {}", clientId, companyId)

        return associationDomainService.getClientVehicles(ClientId.of(clientId.toLong()))
    }

    fun getVehicleClients(vehicleId: String): List<ClientId> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting clients for vehicle: {} in company: {}", vehicleId, companyId)

        return associationDomainService.getVehicleClients(VehicleId.of(vehicleId.toLong()))
    }

    fun getVehicleOwnersMap(vehicleIds: List<VehicleId>): Map<VehicleId, List<ClientId>> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting owners for {} vehicles in company: {}", vehicleIds.size, companyId)

        return associationDomainService.getVehicleOwnersMap(vehicleIds)
    }

    fun getClientVehiclesMap(clientIds: List<ClientId>): Map<ClientId, List<VehicleId>> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting vehicles for {} clients in company: {}", clientIds.size, companyId)

        return associationDomainService.getClientVehiclesMap(clientIds)
    }
}