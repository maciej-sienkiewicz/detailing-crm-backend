package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.create.protocol.CreateServiceRecipeModel
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.domain.model.view.ServiceRecipeView
import com.carslab.crm.domain.port.ServiceRecipeRepository
import com.carslab.crm.infrastructure.persistence.entity.ServiceRecipeEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.ServiceRecipeJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository

@Repository
class JpaServiceRecipeRepositoryAdapter(
    private val serviceRecipeJpaRepository: ServiceRecipeJpaRepository
) : ServiceRecipeRepository {

    override fun save(serviceRecipe: CreateServiceRecipeModel): ServiceRecipeId {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = ServiceRecipeEntity(
            name = serviceRecipe.name,
            companyId = companyId,
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
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return serviceRecipeJpaRepository.findByCompanyIdAndId(companyId, id)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun getAllServices(): List<ServiceRecipeView> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return serviceRecipeJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
    }
}