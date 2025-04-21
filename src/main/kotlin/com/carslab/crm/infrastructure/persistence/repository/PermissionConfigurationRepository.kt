package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.PermissionConfigurationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PermissionConfigurationRepository : JpaRepository<PermissionConfigurationEntity, Long> {

    @Query("SELECT pc FROM PermissionConfigurationEntity pc " +
            "WHERE pc.roleId = :roleId AND pc.permissionId = :permissionId")
    fun findByRoleIdAndPermissionId(
        @Param("roleId") roleId: Long,
        @Param("permissionId") permissionId: Long
    ): PermissionConfigurationEntity?

    @Query("SELECT pc FROM PermissionConfigurationEntity pc " +
            "WHERE pc.roleId = :roleId AND pc.enabled = :enabled")
    fun findByRoleIdAndEnabled(
        @Param("roleId") roleId: Long,
        @Param("enabled") enabled: Boolean
    ): List<PermissionConfigurationEntity>

    @Query("SELECT pc FROM PermissionConfigurationEntity pc " +
            "WHERE pc.roleId = :roleId")
    fun findByRoleId(@Param("roleId") roleId: Long): List<PermissionConfigurationEntity>

    @Query("SELECT pc FROM PermissionConfigurationEntity pc " +
            "WHERE pc.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<PermissionConfigurationEntity>

    @Query("SELECT COUNT(pc) > 0 FROM PermissionConfigurationEntity pc " +
            "WHERE pc.roleId = :roleId AND pc.permissionId = :permissionId AND pc.enabled = true")
    fun existsEnabledPermission(
        @Param("roleId") roleId: Long,
        @Param("permissionId") permissionId: Long
    ): Boolean

    @Modifying
    @Query("DELETE FROM PermissionConfigurationEntity pc WHERE pc.roleId = :roleId")
    fun deleteAllByRoleId(@Param("roleId") roleId: Long)
}