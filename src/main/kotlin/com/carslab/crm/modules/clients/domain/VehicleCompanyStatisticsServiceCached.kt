package com.carslab.crm.modules.clients.domain

import com.carslab.crm.modules.clients.api.responses.VehicleCompanyStatisticsResponse
import com.carslab.crm.modules.clients.domain.port.VehicleCompanyStatisticsRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Performance monitoring aspect for database queries
 */
@org.springframework.stereotype.Component
@org.aspectj.lang.annotation.Aspect
class VehiclePerformanceMonitor {

    private val logger = LoggerFactory.getLogger(VehiclePerformanceMonitor::class.java)

    @org.aspectj.lang.annotation.Around("execution(* com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleTableJpaRepository.*(..))")
    fun monitorTableQueries(joinPoint: org.aspectj.lang.ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = joinPoint.signature.name

        return try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - startTime

            if (executionTime > 1000) { // Log slow queries
                logger.warn("Slow query detected: {} took {}ms", methodName, executionTime)
            } else {
                logger.debug("Query executed: {} took {}ms", methodName, executionTime)
            }

            result
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error("Query failed: {} after {}ms", methodName, executionTime, e)
            throw e
        }
    }

    @org.aspectj.lang.annotation.Around("execution(* com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleStatisticsJpaRepository.*(..))")
    fun monitorStatisticsQueries(joinPoint: org.aspectj.lang.ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = joinPoint.signature.name

        return try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - startTime

            if (executionTime > 2000) { // Statistics queries can be slower
                logger.warn("Slow statistics query: {} took {}ms", methodName, executionTime)
            } else {
                logger.debug("Statistics query executed: {} took {}ms", methodName, executionTime)
            }

            result
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error("Statistics query failed: {} after {}ms", methodName, executionTime, e)
            throw e
        }
    }
}

/**
 * Database index recommendations for optimal performance
 */
@org.springframework.stereotype.Component
class VehicleIndexRecommendations {

    /**
     * Recommended database indexes for optimal performance:
     *
     * 1. vehicles table:
     *    - INDEX idx_vehicles_company_active (company_id, active)
     *    - INDEX idx_vehicles_make_model (make, model)
     *    - INDEX idx_vehicles_license_plate (license_plate)
     *    - INDEX idx_vehicles_created_at (created_at DESC)
     *
     * 2. vehicle_statistics table:
     *    - INDEX idx_vehicle_stats_vehicle_id (vehicle_id) - already exists as FK
     *    - INDEX idx_vehicle_stats_visit_count (visit_count DESC)
     *    - INDEX idx_vehicle_stats_total_revenue (total_revenue DESC)
     *    - INDEX idx_vehicle_stats_last_visit (last_visit_date DESC)
     *
     * 3. client_vehicle_associations table:
     *    - INDEX idx_cva_vehicle_company_active (vehicle_id, company_id, end_date)
     *    - INDEX idx_cva_client_company_active (client_id, company_id, end_date)
     *
     * 4. clients table:
     *    - INDEX idx_clients_company_active (company_id, active)
     *    - INDEX idx_clients_full_name (first_name, last_name)
     *
     * 5. protocols table:
     *    - INDEX idx_protocols_vehicle_status (vehicle_id, status)
     *    - INDEX idx_protocols_company_status (company_id, status)
     *
     * 6. protocol_services table:
     *    - INDEX idx_protocol_services_protocol_approval (protocol_id, approval_status)
     *    - INDEX idx_protocol_services_final_price (final_price DESC)
     */

    fun getIndexCreationSQL(): List<String> = listOf(
        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vehicles_company_active 
        ON vehicles (company_id, active) 
        WHERE active = true;
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vehicles_make_model 
        ON vehicles (make, model);
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vehicles_license_plate_lower 
        ON vehicles (LOWER(license_plate));
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vehicle_stats_visit_count 
        ON vehicle_statistics (visit_count DESC);
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vehicle_stats_total_revenue 
        ON vehicle_statistics (total_revenue DESC);
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_cva_vehicle_company_active 
        ON client_vehicle_associations (vehicle_id, company_id, end_date) 
        WHERE end_date IS NULL;
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_clients_full_name_lower 
        ON clients (LOWER(first_name || ' ' || last_name));
        """,

        """
        CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_protocol_services_stats 
        ON protocol_services (protocol_id, approval_status, final_price) 
        WHERE approval_status = 'APPROVED';
        """
    )
}