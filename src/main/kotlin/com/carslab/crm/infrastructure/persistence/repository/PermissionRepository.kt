package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.DataField
import com.carslab.crm.infrastructure.persistence.entity.PermissionAction
import com.carslab.crm.infrastructure.persistence.entity.PermissionEntity
import com.carslab.crm.infrastructure.persistence.entity.ResourceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PermissionRepository : JpaRepository<PermissionEntity, Long> {

    @Query("SELECT p FROM PermissionEntity p WHERE p.resourceType = :resourceType AND p.action = :action")
    fun findByResourceTypeAndAction(
        @Param("resourceType") resourceType: ResourceType,
        @Param("action") action: PermissionAction
    ): Optional<PermissionEntity>

    @Query("SELECT p FROM PermissionEntity p ORDER BY p.resourceType, p.action")
    fun findAllOrdered(): List<PermissionEntity>

    @Query("SELECT p FROM PermissionEntity p " +
            "JOIN p.dataFields df " +
            "WHERE df = :dataField")
    fun findByDataField(@Param("dataField") dataField: DataField): List<PermissionEntity>
}