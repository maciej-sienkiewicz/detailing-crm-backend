package com.carslab.crm.production.modules.visits.application.service.query

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.visits.application.dto.*
import com.carslab.crm.production.modules.visits.domain.service.VisitDomainService
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VisitQueryService(
    private val visitDomainService: VisitDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VisitQueryService::class.java)
    
    fun getVisitsForClient(clientId: String, pageable: Pageable): Page<VisitResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visits for client: {} and company: {}", clientId, companyId)

        val visits = visitDomainService.getVisitsForClient(ClientId.of(clientId.toLong()), companyId, pageable)
        return visits.map { VisitResponse.from(it) }
    }

    fun getVisitsForVehicle(vehicleId: String, pageable: Pageable): Page<VisitResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching visits for vehicle: {} and company: {}", vehicleId, companyId)

        val visits = visitDomainService.getVisitsForVehicle(VehicleId.of(vehicleId.toLong()), companyId, pageable)
        return visits.map { VisitResponse.from(it) }
    }
}