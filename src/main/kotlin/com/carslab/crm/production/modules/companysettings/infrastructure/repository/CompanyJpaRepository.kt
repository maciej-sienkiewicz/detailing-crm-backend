package com.carslab.crm.production.modules.companysettings.infrastructure.repository

import com.carslab.crm.production.modules.companysettings.infrastructure.entity.CompanyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CompanyJpaRepository : JpaRepository<CompanyEntity, Long> {

    @Query("SELECT c FROM CompanyEntity c WHERE c.id = :id AND c.active = true")
    fun findByIdAndActiveTrue(@Param("id") id: Long): Optional<CompanyEntity>

    fun existsByTaxIdAndActiveTrue(taxId: String): Boolean
}