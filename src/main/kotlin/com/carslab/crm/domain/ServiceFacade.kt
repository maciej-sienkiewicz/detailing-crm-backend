package com.carslab.crm.domain

import com.carslab.crm.domain.model.create.protocol.CreateServiceRecipeModel
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.domain.model.view.ServiceRecipeView
import com.carslab.crm.infrastructure.repository.InMemoryServiceRecipeRepository
import org.springframework.stereotype.Service

@Service
class ServiceFacade(
    private val serviceRecipeRepository: InMemoryServiceRecipeRepository,
) {

    fun createService(serviceRecipe: CreateServiceRecipeModel): ServiceRecipeId =
        serviceRecipeRepository.save(serviceRecipe)

    fun getServiceById(id: Long): ServiceRecipeView? =
        serviceRecipeRepository.getById(id)

    fun getAllServices (): List<ServiceRecipeView> =
        serviceRecipeRepository.getAllServices()
}