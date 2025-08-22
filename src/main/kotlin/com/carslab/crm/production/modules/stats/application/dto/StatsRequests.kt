package com.carslab.crm.production.modules.stats.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    @field:Size(max = 100, message = "Category name cannot exceed 100 characters")
    val name: String
)

data class AddToCategoryRequest(
    val serviceIds: List<Long>
)
