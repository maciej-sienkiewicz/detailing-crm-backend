package com.carslab.crm.production.modules.associations.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.associations.application.dto.AssociationResponse
import com.carslab.crm.production.modules.associations.application.dto.CreateAssociationRequest
import com.carslab.crm.production.modules.associations.domain.model.AssociationType
import com.carslab.crm.production.modules.associations.domain.service.AssociationDomainService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.shared.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AssociationCommandService(
    private val associationDomainService: AssociationDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(AssociationCommandService::class.java)

    fun createAssociation(request: CreateAssociationRequest): AssociationResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating association between client: {} and vehicle: {} for company: {}",
            request.clientId, request.vehicleId, companyId)

        validateCreateRequest(request)

        val association = associationDomainService.createAssociation(
            clientId = ClientId.of(request.clientId),
            vehicleId = VehicleId.of(request.vehicleId),
            companyId = companyId,
            associationType = request.associationType ?: AssociationType.OWNER,
            isPrimary = request.isPrimary ?: false
        )

        logger.info("Association created successfully")
        return AssociationResponse.from(association)
    }

    fun endAssociation(clientId: String, vehicleId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Ending association between client: {} and vehicle: {} for company: {}",
            clientId, vehicleId, companyId)

        val ended = associationDomainService.endAssociation(
            ClientId.of(clientId.toLong()),
            VehicleId.of(vehicleId.toLong()),
            companyId
        )

        if (ended) {
            logger.info("Association ended successfully")
        } else {
            logger.warn("Association not found or already ended")
        }
    }

    fun makePrimaryOwner(clientId: String, vehicleId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Making client: {} primary owner of vehicle: {} for company: {}",
            clientId, vehicleId, companyId)

        associationDomainService.makePrimaryOwner(
            ClientId.of(clientId.toLong()),
            VehicleId.of(vehicleId.toLong()),
            companyId
        )

        logger.info("Primary owner updated successfully")
    }

    private fun validateCreateRequest(request: CreateAssociationRequest) {
        if (request.clientId <= 0) {
            throw BusinessException("Invalid client ID")
        }
        if (request.vehicleId <= 0) {
            throw BusinessException("Invalid vehicle ID")
        }
    }
}