package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.stats.infrastructure.dto.CategoryWithCountProjection
import com.carslab.crm.production.modules.stats.infrastructure.entity.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CategoryJpaRepository : JpaRepository<CategoryEntity, Long> {

    @Query("SELECT c FROM CategoryEntity c WHERE c.companyId = :companyId ORDER BY c.name")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<CategoryEntity>

    @Query("""
        SELECT c.id as categoryId, c.name as categoryName, COUNT(scm.serviceId) as servicesCount
        FROM CategoryEntity c 
        LEFT JOIN ServiceCategoryMappingEntity scm ON c.id = scm.categoryId
        WHERE c.companyId = :companyId
        GROUP BY c.id, c.name
        ORDER BY c.name
    """)
    fun findCategoriesWithCount(@Param("companyId") companyId: Long): List<CategoryWithCountProjection>
}