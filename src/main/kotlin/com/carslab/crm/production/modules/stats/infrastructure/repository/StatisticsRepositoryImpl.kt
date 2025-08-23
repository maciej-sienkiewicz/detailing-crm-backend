package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.stats.domain.model.ServiceId
import com.carslab.crm.production.modules.stats.domain.model.UncategorizedService
import com.carslab.crm.production.modules.stats.domain.repository.StatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class StatisticsRepositoryImpl(
    private val statisticsJpaRepository: StatisticsJpaRepository,
    private val securityContext: SecurityContext,
) : StatisticsRepository {

    private val logger = LoggerFactory.getLogger(StatisticsRepositoryImpl::class.java)

    override fun getUncategorizedServices(): List<UncategorizedService> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Fetching uncategorized services for company: {}", companyId)

        val projections = statisticsJpaRepository.findUncategorizedServices(companyId)

        val services = projections.map { projection ->
            UncategorizedService(
                id = ServiceId(projection.getServiceId()),
                name = projection.getServiceName(),
                servicesCount = projection.getServicesCount(),
                totalRevenue = projection.getTotalRevenue()
            )
        }

        logger.debug("Found {} uncategorized services", services.size)
        return services
    }
}