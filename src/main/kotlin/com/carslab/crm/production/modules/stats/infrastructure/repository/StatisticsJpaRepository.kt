package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.stats.infrastructure.dto.UncategorizedServiceProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StatisticsJpaRepository : JpaRepository<Any, Long> {

    @Query(nativeQuery = true, value = """
        SELECT 
            s.id as serviceId,
            s.name as serviceName,
            COUNT(vs.id) as servicesCount,
            COALESCE(SUM(vs.final_price * vs.quantity), 0) as totalRevenue
        FROM services s
        LEFT JOIN visit_services vs ON s.name = vs.name
        LEFT JOIN visits v ON vs.visit_id = v.id AND v.company_id = :companyId
        LEFT JOIN service_category_mappings scm ON s.id = scm.service_id
        WHERE s.company_id = :companyId 
        AND scm.service_id IS NULL
        GROUP BY s.id, s.name
        ORDER BY totalRevenue DESC
    """)
    fun findUncategorizedServices(@Param("companyId") companyId: Long): List<UncategorizedServiceProjection>
}