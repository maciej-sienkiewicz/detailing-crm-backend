package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.create.protocol.CreateServiceRecipeModel
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.domain.model.view.ServiceRecipeView

/**
 * Port repozytorium dla przepisów serwisowych.
 */
interface ServiceRecipeRepository {
    /**
     * Zapisuje przepis serwisowy.
     */
    fun save(serviceRecipe: CreateServiceRecipeModel): ServiceRecipeId

    /**
     * Pobiera przepis serwisowy po identyfikatorze.
     */
    fun getById(id: Long): ServiceRecipeView?

    /**
     * Pobiera wszystkie przepisy serwisowe.
     */
    fun getAllServices(): List<ServiceRecipeView>
}