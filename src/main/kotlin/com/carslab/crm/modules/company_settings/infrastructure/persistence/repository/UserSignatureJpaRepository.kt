// src/main/kotlin/com/carslab/crm/modules/company_settings/infrastructure/persistence/repository/UserSignatureJpaRepository.kt
package com.carslab.crm.modules.company_settings.infrastructure.persistence.repository

import com.carslab.crm.modules.company_settings.infrastructure.persistence.entity.UserSignatureEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface UserSignatureJpaRepository : JpaRepository<UserSignatureEntity, String> {

    @Query("SELECT us FROM UserSignatureEntity us WHERE us.userId = :userId AND us.companyId = :companyId AND us.active = true")
    fun findByUserIdAndCompanyIdAndActiveTrue(
        @Param("userId") userId: Long,
        @Param("companyId") companyId: Long
    ): Optional<UserSignatureEntity>

    @Query("SELECT us FROM UserSignatureEntity us WHERE us.id = :id AND us.active = true")
    fun findByIdAndActiveTrue(@Param("id") id: String): Optional<UserSignatureEntity>

    fun existsByUserIdAndCompanyIdAndActiveTrue(userId: Long, companyId: Long): Boolean

    @Modifying
    @Query("UPDATE UserSignatureEntity us SET us.active = false, us.updatedAt = :now WHERE us.userId = :userId AND us.companyId = :companyId")
    fun softDeleteByUserIdAndCompanyId(
        @Param("userId") userId: Long,
        @Param("companyId") companyId: Long,
        @Param("now") now: LocalDateTime
    ): Int

    @Query("SELECT COUNT(us) FROM UserSignatureEntity us WHERE us.companyId = :companyId AND us.active = true")
    fun countActiveSignaturesByCompanyId(@Param("companyId") companyId: Long): Long

    @Query("SELECT us FROM UserSignatureEntity us WHERE us.companyId = :companyId AND us.active = true")
    fun findAllByCompanyIdAndActiveTrue(@Param("companyId") companyId: Long): List<UserSignatureEntity>
}