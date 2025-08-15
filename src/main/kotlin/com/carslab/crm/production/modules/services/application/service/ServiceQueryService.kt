package com.carslab.crm.production.modules.services.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.services.application.dto.ServiceResponse
import com.carslab.crm.production.modules.services.domain.service.ServiceDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ServiceQueryService(
    private val domainService: ServiceDomainService,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(ServiceQueryService::class.java)

    fun getServicesForCurrentCompany(): List<ServiceResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching services for company: {}", companyId)

        val services = domainService.getServicesForCompany(companyId)
        logger.debug("Found {} services for company: {}", services.size, companyId)

        return services.map { ServiceResponse.from(it) }
    }

    fun getService(serviceId: String): ServiceResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching service: {} for company: {}", serviceId, companyId)

        val service = domainService.getActiveServiceForCompany(serviceId, companyId)
        logger.debug("Service found: {}", service.name)

        return ServiceResponse.from(service)
    }
}
