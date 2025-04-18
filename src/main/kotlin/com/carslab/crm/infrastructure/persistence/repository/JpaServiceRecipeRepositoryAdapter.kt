package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.create.protocol.CreateServiceRecipeModel
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.domain.model.view.ServiceRecipeView
import com.carslab.crm.domain.port.ServiceRecipeRepository
import com.carslab.crm.infrastructure.persistence.entity.ServiceRecipeEntity
import com.carslab.crm.infrastructure.persistence.repository.ServiceRecipeJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaServiceRecipeRepositoryAdapter(
    private val serviceRecipeJpaRepository: ServiceRecipeJpaRepository
) : ServiceRecipeRepository {

    override fun save(serviceRecipe: CreateServiceRecipeModel): ServiceRecipeId {
        val entity = ServiceRecipeEntity(
            name = serviceRecipe.name,
            description = serviceRecipe.description,
            price = serviceRecipe.price,
            vatRate = serviceRecipe.vatRate,
            createdAt = serviceRecipe.audit.createdAt,
            updatedAt = serviceRecipe.audit.updatedAt
        )

        val savedEntity = serviceRecipeJpaRepository.save(entity)
        return ServiceRecipeId(savedEntity.id.toString())
    }

    override fun getById(id: Long): ServiceRecipeView? {
        return serviceRecipeJpaRepository.findById(id)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun getAllServices(): List<ServiceRecipeView> {
        return serviceRecipeJpaRepository.findAll().map { it.toDomain() }
    }
}