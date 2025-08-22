package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.stats.domain.model.Category
import com.carslab.crm.production.modules.stats.domain.model.CategoryId
import com.carslab.crm.production.modules.stats.domain.model.ServiceId
import com.carslab.crm.production.modules.stats.domain.repository.CategoriesRepository
import com.carslab.crm.production.modules.stats.infrastructure.entity.CategoryEntity
import com.carslab.crm.production.modules.stats.infrastructure.entity.ServiceCategoryMappingEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
@Transactional
class CategoriesRepositoryImpl(
    private val categoryJpaRepository: CategoryJpaRepository,
    private val mappingJpaRepository: ServiceCategoryMappingJpaRepository
) : CategoriesRepository {

    private val logger = LoggerFactory.getLogger(CategoriesRepositoryImpl::class.java)

    override fun createCategory(categoryName: String, companyId: Long): Category {
        logger.debug("Creating category: {} for company: {}", categoryName, companyId)

        val entity = CategoryEntity(
            companyId = companyId,
            name = categoryName
        )

        val savedEntity = categoryJpaRepository.save(entity)

        logger.debug("Category created with ID: {}", savedEntity.id)

        return Category(
            id = CategoryId(savedEntity.id!!),
            name = savedEntity.name,
            servicesCount = 0
        )
    }

    override fun addToCategory(servicesIds: List<ServiceId>, categoryId: CategoryId) {
        logger.debug("Adding {} services to category: {}", servicesIds.size, categoryId.id)

        val serviceIdStrings = servicesIds.map { it.id.toString() }

        mappingJpaRepository.deleteByServiceIdIn(serviceIdStrings)

        val mappings = servicesIds.map { serviceId ->
            ServiceCategoryMappingEntity(
                serviceId = serviceId.id.toString(),
                categoryId = categoryId.id
            )
        }

        mappingJpaRepository.saveAll(mappings)

        logger.debug("Services added to category successfully")
    }

    override fun updateCategoryName(categoryId: CategoryId, newName: String): Category {
        logger.debug("Updating category name: {} to: {}", categoryId.id, newName)

        val entity = categoryJpaRepository.findById(categoryId.id)
            .orElseThrow { IllegalArgumentException("Category not found: ${categoryId.id}") }

        val updatedEntity = CategoryEntity(
            id = entity.id,
            companyId = entity.companyId,
            name = newName,
            createdAt = entity.createdAt,
            updatedAt = LocalDateTime.now()
        )

        val saved = categoryJpaRepository.save(updatedEntity)

        logger.debug("Category name updated successfully")

        return Category(
            id = CategoryId(saved.id!!),
            name = saved.name,
            servicesCount = 0 
        )
    }

    @Transactional(readOnly = true)
    override fun getCategories(companyId: CategoryId): List<Category> {
        logger.debug("Fetching categories for company: {}", companyId.id)

        val projections = categoryJpaRepository.findCategoriesWithCount(companyId.id)

        val categories = projections.map { projection ->
            Category(
                id = CategoryId(projection.getCategoryId()),
                name = projection.getCategoryName(),
                servicesCount = projection.getServicesCount().toInt()
            )
        }

        logger.debug("Found {} categories", categories.size)
        return categories
    }
}