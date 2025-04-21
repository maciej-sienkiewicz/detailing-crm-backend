package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<RoleEntity, Long> {

    @Query("SELECT r FROM RoleEntity r WHERE r.name = :name AND r.companyId = :companyId")
    fun findByNameAndCompanyId(
        @Param("name") name: String,
        @Param("companyId") companyId: Long
    ): RoleEntity?

    @Query("SELECT r FROM RoleEntity r WHERE r.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<RoleEntity>

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RoleEntity r " +
            "WHERE r.name = :name AND r.companyId = :companyId AND r.id <> :excludeId")
    fun existsByNameForCompanyExcludingId(
        @Param("name") name: String,
        @Param("companyId") companyId: Long,
        @Param("excludeId") excludeId: Long
    ): Boolean

    @Query("SELECT r FROM RoleEntity r " +
            "JOIN r.permissions p " +
            "WHERE p.id = :permissionId")
    fun findByPermissionId(@Param("permissionId") permissionId: Long): List<RoleEntity>
}