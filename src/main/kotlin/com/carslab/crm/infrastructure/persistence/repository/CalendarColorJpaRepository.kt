package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.CalendarColorEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CalendarColorJpaRepository : JpaRepository<CalendarColorEntity, String> {

    fun existsByNameIgnoreCaseAndCompanyId(name: String, companyId: Long): Boolean

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CalendarColorEntity c WHERE LOWER(c.name) = LOWER(:name) AND c.id <> :excludeId AND c.companyId = :companyId")
    fun existsByNameIgnoreCaseAndIdNot(@Param("name") name: String, @Param("excludeId") excludeId: String, @Param("companyId") companyId: Long): Boolean
}