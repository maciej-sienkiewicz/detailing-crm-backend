package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.CalendarColorEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CalendarColorJpaRepository : JpaRepository<CalendarColorEntity, Long> {

    /**
     * Finds all calendar colors for a specific company
     */
    fun findByCompanyId(companyId: Long): List<CalendarColorEntity>

    /**
     * Finds a calendar color by its ID for a specific company
     */
    fun findByIdAndCompanyId(id: Long, companyId: Long): Optional<CalendarColorEntity>

    /**
     * Alternative method name that matches other repositories' naming pattern
     */
    fun findByCompanyIdAndId(companyId: Long, id: Long): Optional<CalendarColorEntity>

    /**
     * Checks if a calendar color with the given name exists for a company
     */
    fun existsByNameIgnoreCaseAndCompanyId(name: String, companyId: Long): Boolean

    /**
     * Checks if a calendar color with the given name exists for a company,
     * excluding a specific ID
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CalendarColorEntity c WHERE LOWER(c.name) = LOWER(:name) AND c.id <> :excludeId AND c.companyId = :companyId")
    fun existsByNameIgnoreCaseAndIdNotAndCompanyId(
        @Param("name") name: String,
        @Param("excludeId") excludeId: Long,
        @Param("companyId") companyId: Long
    ): Boolean

    /**
     * Deletes all calendar colors for a specific company
     */
    fun deleteByCompanyId(companyId: Long): Long

    /**
     * Counts calendar colors for a specific company
     */
    fun countByCompanyId(companyId: Long): Long

    /**
     * Finds calendar colors by name containing a pattern for a specific company
     */
    fun findByNameContainingIgnoreCaseAndCompanyId(name: String, companyId: Long): List<CalendarColorEntity>

    /**
     * Finds calendar colors by color value for a specific company
     */
    fun findByColorAndCompanyId(color: String, companyId: Long): List<CalendarColorEntity>

    /**
     * Custom query to find calendar colors based on multiple criteria
     */
    @Query("""
        SELECT c FROM CalendarColorEntity c 
        WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:color IS NULL OR c.color = :color)
        AND c.companyId = :companyId
        ORDER BY c.name ASC
    """)
    fun searchCalendarColors(
        @Param("name") name: String?,
        @Param("color") color: String?,
        @Param("companyId") companyId: Long
    ): List<CalendarColorEntity>
}