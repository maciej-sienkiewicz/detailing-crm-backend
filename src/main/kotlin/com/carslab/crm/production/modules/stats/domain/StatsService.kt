package com.carslab.crm.production.modules.stats.domain

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.stats.domain.model.Category
import com.carslab.crm.production.modules.stats.domain.model.CategoryId
import com.carslab.crm.production.modules.stats.domain.model.ServiceId
import com.carslab.crm.production.modules.stats.domain.model.UncategorizedService
import com.carslab.crm.production.modules.stats.domain.repository.CategoriesRepository
import com.carslab.crm.production.modules.stats.domain.repository.StatisticsRepository
import org.springframework.stereotype.Service

@Service
class StatsService(
    private val statisticsRepository: StatisticsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val securityContext: SecurityContext
) {
    
    fun createCategory(name: String): Category = 
        categoriesRepository.createCategory(name, securityContext.getCurrentCompanyId())
    
    fun addToCategory(services: List<ServiceId>, categoryId: CategoryId) {
        categoriesRepository.addToCategory(services, categoryId)
    }
    
    fun getUncategorizedServices(): List<UncategorizedService> =
        statisticsRepository.getUncategorizedServices()
    
    fun getCategoriesWithServiceCounts(): List<Category> =
        categoriesRepository.getCategories(securityContext.getCurrentCompanyId())
}