package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.stats.infrastructure.entity.ServiceCategoryMappingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ServiceCategoryMappingJpaRepository : JpaRepository<ServiceCategoryMappingEntity, Long> {

    @Query("SELECT scm FROM ServiceCategoryMappingEntity scm WHERE scm.categoryId = :categoryId")
    fun findByCategoryId(@Param("categoryId") categoryId: Long): List<ServiceCategoryMappingEntity>

    @Query("SELECT scm FROM ServiceCategoryMappingEntity scm WHERE scm.serviceId IN :serviceIds")
    fun findByServiceIdIn(@Param("serviceIds") serviceIds: List<String>): List<ServiceCategoryMappingEntity>

    @Query("DELETE FROM ServiceCategoryMappingEntity scm WHERE scm.serviceId IN :serviceIds")
    fun deleteByServiceIdIn(@Param("serviceIds") serviceIds: List<String>)
}