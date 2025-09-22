package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.stats.domain.model.Category
import com.carslab.crm.production.modules.stats.domain.model.CategoryId
import com.carslab.crm.production.modules.stats.domain.model.ServiceId
import com.carslab.crm.production.modules.stats.domain.repository.CategoriesRepository
import com.carslab.crm.production.modules.stats.infrastructure.entity.CategoryEntity
import com.carslab.crm.production.modules.stats.infrastructure.entity.ServiceCategoryMappingEntity
import com.carslab.crm.production.shared.observability.annotations.DatabaseMonitored
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrElse

@Repository
@Transactional
class CategoriesRepositoryImpl(
    private val categoryJpaRepository: CategoryJpaRepository,
    private val mappingJpaRepository: ServiceCategoryMappingJpaRepository
) : CategoriesRepository {

    private val logger = LoggerFactory.getLogger(CategoriesRepositoryImpl::class.java)

    @DatabaseMonitored(repository = "category", method = "createCategory", operation = "insert")
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

    @DatabaseMonitored(repository = "category", method = "addToCategory", operation = "update")
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

    @DatabaseMonitored(repository = "category", method = "updateCategoryName", operation = "update")
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
    @DatabaseMonitored(repository = "category", method = "getCategories", operation = "select")
    override fun getCategories(companyId: Long): List<Category> {
        logger.debug("Fetching categories for company: {}", companyId)

        val projections = categoryJpaRepository.findCategoriesWithCount(companyId)

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

    @Transactional(readOnly = true)
    @DatabaseMonitored(repository = "category", method = "getCategoryName", operation = "select")
    override fun getCategoryName(categoryId: CategoryId, companyId: Long): String {
        logger.debug("Fetching category name for category: {} and company: {}", categoryId.id, companyId)

        val entity = categoryJpaRepository.findById(categoryId.id).getOrElse { throw IllegalArgumentException("Category not found: ${categoryId.id}") }

        return entity.name
    }
}