package com.carslab.crm.production.modules.stats.infrastructure.repository

import com.carslab.crm.production.modules.services.infrastructure.entity.ServiceEntity
import com.carslab.crm.production.modules.stats.infrastructure.dto.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface StatisticsJpaRepository : JpaRepository<ServiceEntity, String> {

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

    @Query(nativeQuery = true, value = """
        SELECT 
            s.id as serviceId,
            s.name as serviceName,
            COUNT(vs.id) as servicesCount,
            COALESCE(SUM(vs.final_price * vs.quantity), 0) as totalRevenue
        FROM services s
        INNER JOIN service_category_mappings scm ON s.id = scm.service_id
        LEFT JOIN visit_services vs ON s.name = vs.name
        LEFT JOIN visits v ON vs.visit_id = v.id AND v.company_id = :companyId
        WHERE s.company_id = :companyId 
        AND scm.category_id = :categoryId
        GROUP BY s.id, s.name
        ORDER BY totalRevenue DESC
    """)
    fun findCategorizedServices(
        @Param("categoryId") categoryId: Long,
        @Param("companyId") companyId: Long
    ): List<UncategorizedServiceProjection>

    @Query(nativeQuery = true, value = """
        SELECT 
            c.id as categoryId,
            c.name as categoryName,
            COUNT(vs.id) as totalOrders,
            COALESCE(SUM(vs.final_price * vs.quantity), 0) as totalRevenue,
            COUNT(DISTINCT s.id) as servicesCount
        FROM service_categories c
        LEFT JOIN service_category_mappings scm ON c.id = scm.category_id
        LEFT JOIN services s ON scm.service_id = s.id
        LEFT JOIN visit_services vs ON s.name = vs.name
        LEFT JOIN visits v ON vs.visit_id = v.id AND v.company_id = :companyId
        WHERE c.id = :categoryId AND c.company_id = :companyId
        GROUP BY c.id, c.name
    """)
    fun findCategoryStatsSummary(
        @Param("categoryId") categoryId: Long,
        @Param("companyId") companyId: Long
    ): CategoryStatsSummaryProjection?

    @Query(nativeQuery = true, value = """
        SELECT 
            TO_CHAR(DATE_TRUNC(:granularity, v.start_date), :dateFormat) as period,
            COUNT(vs.id) as orders,
            COALESCE(SUM(vs.final_price * vs.quantity), 0) as revenue
        FROM service_categories c
        LEFT JOIN service_category_mappings scm ON c.id = scm.category_id
        LEFT JOIN services s ON scm.service_id = s.id
        LEFT JOIN visit_services vs ON s.name = vs.name
        LEFT JOIN visits v ON vs.visit_id = v.id 
        WHERE c.id = :categoryId 
        AND c.company_id = :companyId
        AND v.start_date >= :startDate 
        AND v.start_date <= :endDate
        GROUP BY period
        ORDER BY period
    """)
    fun findCategoryStatsTimeSeries(
        @Param("categoryId") categoryId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("granularity") granularity: String,
        @Param("dateFormat") dateFormat: String,
        @Param("companyId") companyId: Long
    ): List<TimeSeriesProjection>

    @Query(nativeQuery = true, value = """
        SELECT s.name as serviceName
        FROM services s
        WHERE s.id = :serviceId AND s.company_id = :companyId
    """)
    fun findServiceName(
        @Param("serviceId") serviceId: String,
        @Param("companyId") companyId: Long
    ): ServiceStatsProjection?

    @Query(nativeQuery = true, value = """
        SELECT 
            TO_CHAR(DATE_TRUNC(:granularity, v.start_date), :dateFormat) as period,
            COUNT(vs.id) as orders,
            COALESCE(SUM(vs.final_price * vs.quantity), 0) as revenue
        FROM services s
        LEFT JOIN visit_services vs ON s.name = vs.name
        LEFT JOIN visits v ON vs.visit_id = v.id 
        WHERE s.id = :serviceId 
        AND s.company_id = :companyId
        AND v.start_date >= :startDate 
        AND v.start_date <= :endDate
        GROUP BY period
        ORDER BY period
    """)
    fun findServiceStatsTimeSeries(
        @Param("serviceId") serviceId: String,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("granularity") granularity: String,
        @Param("dateFormat") dateFormat: String,
        @Param("companyId") companyId: Long
    ): List<TimeSeriesProjection>
}
