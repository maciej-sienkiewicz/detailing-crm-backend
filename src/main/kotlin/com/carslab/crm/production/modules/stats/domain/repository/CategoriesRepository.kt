package com.carslab.crm.production.modules.stats.domain.repository

import com.carslab.crm.production.modules.stats.domain.model.Category
import com.carslab.crm.production.modules.stats.domain.model.CategoryId
import com.carslab.crm.production.modules.stats.domain.model.ServiceId

interface CategoriesRepository {
    fun createCategory(categoryName: String, companyId: Long): Category
    fun addToCategory(servicesIds: List<ServiceId>, categoryId: CategoryId)
    fun updateCategoryName(categoryId: CategoryId, newName: String): Category
    fun getCategories(companyId: Long): List<Category>
}