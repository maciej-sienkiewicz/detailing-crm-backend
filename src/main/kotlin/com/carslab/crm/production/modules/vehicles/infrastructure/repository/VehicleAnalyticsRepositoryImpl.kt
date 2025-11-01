// src/main/kotlin/com/carslab/crm/production/modules/vehicles/infrastructure/repository/VehicleAnalyticsRepositoryImpl.kt
package com.carslab.crm.production.modules.vehicles.infrastructure.repository

import com.carslab.crm.production.modules.vehicles.domain.model.*
import com.carslab.crm.production.modules.vehicles.domain.repository.VehicleAnalyticsRepository
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Repository
class VehicleAnalyticsRepositoryImpl(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : VehicleAnalyticsRepository {

    override fun getProfitabilityAnalysis(vehicleId: VehicleId, companyId: Long): VehicleProfitabilityAnalysis? {
        val sql = """
            WITH visit_revenue AS (
                SELECT 
                    v.id,
                    v.vehicle_id,
                    v.start_date,
                    COALESCE(service_totals.total, 0) as visit_value
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id, 
                        SUM(vs.final_price_brutto) as total
                    FROM visit_services vs 
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.vehicle_id = :vehicleId 
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                  AND COALESCE(service_totals.total, 0) > 0
            ),
            visit_stats AS (
                SELECT 
                    vehicle_id,
                    COUNT(*) as total_visits,
                    AVG(visit_value) as avg_visit_value,
                    SUM(visit_value) as total_revenue,
                    MIN(start_date) as first_visit,
                    MAX(start_date) as last_visit,
                    COUNT(CASE WHEN start_date >= CURRENT_DATE - INTERVAL '3 months' THEN 1 END) as recent_visits,
                    COUNT(CASE WHEN start_date >= CURRENT_DATE - INTERVAL '6 months' 
                               AND start_date < CURRENT_DATE - INTERVAL '3 months' THEN 1 END) as previous_visits,
                    SUM(CASE WHEN start_date >= CURRENT_DATE - INTERVAL '3 months' THEN visit_value ELSE 0 END) as recent_revenue,
                    SUM(CASE WHEN start_date >= CURRENT_DATE - INTERVAL '6 months' 
                             AND start_date < CURRENT_DATE - INTERVAL '3 months' THEN visit_value ELSE 0 END) as previous_revenue
                FROM visit_revenue
                GROUP BY vehicle_id
            ),
            monthly_metrics AS (
                SELECT 
                    vs.*,
                    CASE 
                        WHEN EXTRACT(EPOCH FROM (last_visit - first_visit)) > 0 
                        THEN total_revenue / GREATEST(
                            EXTRACT(EPOCH FROM (last_visit - first_visit)) / (30.44 * 24 * 60 * 60), 
                            1
                        )
                        ELSE total_revenue
                    END as monthly_avg_revenue,
                    CASE 
                        WHEN previous_revenue > 0 
                        THEN ((recent_revenue - previous_revenue) / previous_revenue) * 100
                        WHEN recent_revenue > 0 AND previous_revenue = 0 
                        THEN 100
                        ELSE 0
                    END as trend_percentage
                FROM visit_stats vs
            )
            SELECT 
                mm.*,
                CASE 
                    WHEN mm.trend_percentage > 10 THEN 'GROWING'
                    WHEN mm.trend_percentage BETWEEN -10 AND 10 THEN 'STABLE'
                    WHEN mm.trend_percentage < -10 THEN 'DECLINING'
                    ELSE 'INSUFFICIENT_DATA'
                END as revenue_trend
            FROM monthly_metrics mm
        """.trimIndent()

        val params = mapOf(
            "vehicleId" to vehicleId.value,
            "companyId" to companyId
        )

        return try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ ->
                val avgVisitValue = rs.getBigDecimal("avg_visit_value") ?: BigDecimal.ZERO
                val monthlyRevenue = rs.getBigDecimal("monthly_avg_revenue") ?: BigDecimal.ZERO
                val totalVisits = rs.getInt("total_visits")
                val trendPercentage = rs.getBigDecimal("trend_percentage") ?: BigDecimal.ZERO
                val firstVisitTs = rs.getTimestamp("first_visit")

                val monthsSinceFirst = firstVisitTs?.let {
                    ChronoUnit.MONTHS.between(
                        it.toLocalDateTime().toLocalDate(),
                        LocalDate.now()
                    ).toInt()
                } ?: 0

                val visitFrequencyPerMonth = if (monthsSinceFirst > 0) {
                    totalVisits.toDouble() / monthsSinceFirst
                } else {
                    0.0
                }

                val profitabilityScore = VehicleProfitabilityAnalysis.calculateProfitabilityScore(
                    avgVisitValue, visitFrequencyPerMonth, monthsSinceFirst
                )

                VehicleProfitabilityAnalysis(
                    vehicleId = vehicleId,
                    companyId = companyId,
                    averageVisitValue = avgVisitValue.setScale(2, RoundingMode.HALF_UP),
                    monthlyRevenue = monthlyRevenue.setScale(2, RoundingMode.HALF_UP),
                    revenueTrend = RevenueTrend.valueOf(rs.getString("revenue_trend")),
                    trendPercentage = trendPercentage.setScale(1, RoundingMode.HALF_UP),
                    profitabilityScore = profitabilityScore,
                    calculatedAt = LocalDateTime.now()
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun getVisitPattern(vehicleId: VehicleId, companyId: Long): VehicleVisitPattern? {
        val sql = """
            WITH visit_dates AS (
                SELECT 
                    v.start_date::date as visit_date,
                    LAG(v.start_date::date) OVER (ORDER BY v.start_date) as prev_visit_date
                FROM visits v
                WHERE v.vehicle_id = :vehicleId 
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                ORDER BY v.start_date
            ),
            gap_analysis AS (
                SELECT 
                    COUNT(*) as total_visits,
                    MAX(visit_date) as last_visit_date,
                    MIN(visit_date) as first_visit_date,
                    AVG(CASE WHEN prev_visit_date IS NOT NULL 
                        THEN (visit_date - prev_visit_date)
                        ELSE NULL END) as avg_days_between_visits,
                    (CURRENT_DATE - MAX(visit_date)) as days_since_last_visit
                FROM visit_dates
            )
            SELECT 
                ga.*,
                CASE 
                    WHEN ga.total_visits <= 1 THEN 'NEW_CLIENT'
                    WHEN ga.days_since_last_visit > (COALESCE(ga.avg_days_between_visits, 45) * 2.5) THEN 'LOST'
                    WHEN ga.days_since_last_visit > (COALESCE(ga.avg_days_between_visits, 45) * 1.8) THEN 'AT_RISK'
                    WHEN ga.avg_days_between_visits IS NULL OR ga.avg_days_between_visits > 90 THEN 'IRREGULAR'
                    ELSE 'REGULAR'
                END as regularity_status,
                CASE 
                    WHEN ga.avg_days_between_visits IS NOT NULL AND ga.avg_days_between_visits > 0
                    THEN ga.last_visit_date + (ga.avg_days_between_visits || ' days')::interval
                    ELSE NULL
                END as next_recommended_date
            FROM gap_analysis ga
            WHERE ga.total_visits > 0
        """.trimIndent()

        val params = mapOf(
            "vehicleId" to vehicleId.value,
            "companyId" to companyId
        )

        return try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ ->
                val totalVisits = rs.getInt("total_visits")
                val daysSinceLastVisit = rs.getInt("days_since_last_visit").takeIf { !rs.wasNull() }
                val avgDaysBetweenVisits = rs.getDouble("avg_days_between_visits").takeIf { !rs.wasNull() }?.toInt()
                val nextRecommendedDate = rs.getDate("next_recommended_date")?.toLocalDate()
                val regularityStatus = VisitRegularityStatus.valueOf(rs.getString("regularity_status"))

                VehicleVisitPattern(
                    vehicleId = vehicleId,
                    companyId = companyId,
                    daysSinceLastVisit = daysSinceLastVisit,
                    averageDaysBetweenVisits = avgDaysBetweenVisits,
                    visitRegularityStatus = regularityStatus,
                    nextRecommendedVisitDate = nextRecommendedDate,
                    totalVisits = totalVisits,
                    calculatedAt = LocalDateTime.now()
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun getServicePreferences(vehicleId: VehicleId, companyId: Long): VehicleServicePreferences? {
        val sql = """
            SELECT 
                vs.service_id,
                vs.name as service_name,
                COUNT(*) as usage_count,
                SUM(vs.final_price_brutto) as total_revenue,
                AVG(vs.final_price_brutto) as average_price,
                MAX(v.start_date) as last_used_date
            FROM visit_services vs
            JOIN visits v ON vs.visit_id = v.id
            WHERE v.vehicle_id = :vehicleId 
              AND v.company_id = :companyId
              AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
            GROUP BY vs.service_id, vs.name
            ORDER BY usage_count DESC, total_revenue DESC
            LIMIT 10
        """.trimIndent()

        val params = mapOf(
            "vehicleId" to vehicleId.value,
            "companyId" to companyId
        )

        val services = jdbcTemplate.query(sql, params) { rs, _ ->
            ServiceUsageSummary(
                serviceId = rs.getString("service_id"),
                serviceName = rs.getString("service_name"),
                usageCount = rs.getInt("usage_count"),
                totalRevenue = rs.getBigDecimal("total_revenue").setScale(2, RoundingMode.HALF_UP),
                averagePrice = rs.getBigDecimal("average_price").setScale(2, RoundingMode.HALF_UP),
                lastUsedDate = rs.getTimestamp("last_used_date")?.toLocalDateTime()
            )
        }

        return if (services.isNotEmpty()) {
            VehicleServicePreferences(
                vehicleId = vehicleId,
                companyId = companyId,
                topServices = services,
                calculatedAt = LocalDateTime.now()
            )
        } else null
    }

    override fun getBatchProfitabilityAnalysis(
        vehicleIds: List<VehicleId>,
        companyId: Long
    ): Map<VehicleId, VehicleProfitabilityAnalysis> {
        if (vehicleIds.isEmpty()) return emptyMap()

        val sql = """
            WITH vehicle_filter AS (
                SELECT UNNEST(ARRAY[:vehicleIds]::bigint[]) as vehicle_id
            ),
            visit_revenue AS (
                SELECT 
                    v.vehicle_id,
                    v.start_date,
                    COALESCE(service_totals.total, 0) as visit_value
                FROM visits v
                JOIN vehicle_filter vf ON v.vehicle_id = vf.vehicle_id
                LEFT JOIN (
                    SELECT 
                        vs.visit_id, 
                        SUM(vs.final_price_brutto) as total
                    FROM visit_services vs 
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                  AND COALESCE(service_totals.total, 0) > 0
            ),
            analytics AS (
                SELECT 
                    vr.vehicle_id,
                    COUNT(*) as total_visits,
                    AVG(vr.visit_value) as avg_visit_value,
                    SUM(vr.visit_value) as total_revenue,
                    MIN(vr.start_date) as first_visit,
                    MAX(vr.start_date) as last_visit,
                    COUNT(CASE WHEN vr.start_date >= CURRENT_DATE - INTERVAL '3 months' THEN 1 END) as recent_visits,
                    COUNT(CASE WHEN vr.start_date >= CURRENT_DATE - INTERVAL '6 months' 
                               AND vr.start_date < CURRENT_DATE - INTERVAL '3 months' THEN 1 END) as previous_visits,
                    SUM(CASE WHEN vr.start_date >= CURRENT_DATE - INTERVAL '3 months' THEN vr.visit_value ELSE 0 END) as recent_revenue,
                    SUM(CASE WHEN vr.start_date >= CURRENT_DATE - INTERVAL '6 months' 
                             AND vr.start_date < CURRENT_DATE - INTERVAL '3 months' THEN vr.visit_value ELSE 0 END) as previous_revenue
                FROM visit_revenue vr
                GROUP BY vr.vehicle_id
            )
            SELECT 
                a.vehicle_id,
                a.avg_visit_value,
                CASE 
                    WHEN EXTRACT(EPOCH FROM (a.last_visit - a.first_visit)) > 0 
                    THEN a.total_revenue / GREATEST(
                        EXTRACT(EPOCH FROM (a.last_visit - a.first_visit)) / (30.44 * 24 * 60 * 60), 
                        1
                    )
                    ELSE a.total_revenue
                END as monthly_avg_revenue,
                CASE 
                    WHEN a.previous_revenue > 0 
                    THEN ((a.recent_revenue - a.previous_revenue) / a.previous_revenue) * 100
                    WHEN a.recent_revenue > 0 AND a.previous_revenue = 0 
                    THEN 100
                    ELSE 0
                END as trend_percentage,
                CASE 
                    WHEN a.previous_revenue > 0 AND ((a.recent_revenue - a.previous_revenue) / a.previous_revenue) * 100 > 10 THEN 'GROWING'
                    WHEN a.previous_revenue > 0 AND ((a.recent_revenue - a.previous_revenue) / a.previous_revenue) * 100 BETWEEN -10 AND 10 THEN 'STABLE'
                    WHEN a.previous_revenue > 0 AND ((a.recent_revenue - a.previous_revenue) / a.previous_revenue) * 100 < -10 THEN 'DECLINING'
                    ELSE 'INSUFFICIENT_DATA'
                END as revenue_trend,
                a.total_visits,
                a.first_visit
            FROM analytics a
        """.trimIndent()

        val params = mapOf(
            "vehicleIds" to vehicleIds.map { it.value }.toLongArray(),
            "companyId" to companyId
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            val vehicleId = VehicleId.of(rs.getLong("vehicle_id"))
            val avgVisitValue = rs.getBigDecimal("avg_visit_value") ?: BigDecimal.ZERO
            val monthlyRevenue = rs.getBigDecimal("monthly_avg_revenue") ?: BigDecimal.ZERO
            val trendPercentage = rs.getBigDecimal("trend_percentage") ?: BigDecimal.ZERO
            val totalVisits = rs.getInt("total_visits")
            val firstVisitTs = rs.getTimestamp("first_visit")

            val monthsSinceFirst = firstVisitTs?.let {
                ChronoUnit.MONTHS.between(
                    it.toLocalDateTime().toLocalDate(),
                    LocalDate.now()
                ).toInt()
            } ?: 0

            val visitFrequencyPerMonth = if (monthsSinceFirst > 0) {
                totalVisits.toDouble() / monthsSinceFirst
            } else {
                0.0
            }

            val profitabilityScore = VehicleProfitabilityAnalysis.calculateProfitabilityScore(
                avgVisitValue, visitFrequencyPerMonth, monthsSinceFirst
            )

            vehicleId to VehicleProfitabilityAnalysis(
                vehicleId = vehicleId,
                companyId = companyId,
                averageVisitValue = avgVisitValue.setScale(2, RoundingMode.HALF_UP),
                monthlyRevenue = monthlyRevenue.setScale(2, RoundingMode.HALF_UP),
                revenueTrend = RevenueTrend.valueOf(rs.getString("revenue_trend")),
                trendPercentage = trendPercentage.setScale(1, RoundingMode.HALF_UP),
                profitabilityScore = profitabilityScore,
                calculatedAt = LocalDateTime.now()
            )
        }.toMap()
    }

    override fun getBatchVisitPatterns(
        vehicleIds: List<VehicleId>,
        companyId: Long
    ): Map<VehicleId, VehicleVisitPattern> {
        if (vehicleIds.isEmpty()) return emptyMap()

        val sql = """
            WITH vehicle_filter AS (
                SELECT UNNEST(ARRAY[:vehicleIds]::bigint[]) as vehicle_id
            ),
            visit_gaps AS (
                SELECT 
                    v.vehicle_id,
                    v.start_date::date as visit_date,
                    LAG(v.start_date::date) OVER (PARTITION BY v.vehicle_id ORDER BY v.start_date) as prev_visit_date
                FROM visits v
                JOIN vehicle_filter vf ON v.vehicle_id = vf.vehicle_id
                WHERE v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
            ),
            pattern_analysis AS (
                SELECT 
                    vg.vehicle_id,
                    COUNT(*) as total_visits,
                    MAX(vg.visit_date) as last_visit_date,
                    MIN(vg.visit_date) as first_visit_date,
                    AVG(CASE WHEN vg.prev_visit_date IS NOT NULL 
                        THEN (vg.visit_date - vg.prev_visit_date)
                        ELSE NULL END) as avg_days_between_visits,
                    (CURRENT_DATE - MAX(vg.visit_date)) as days_since_last_visit
                FROM visit_gaps vg
                GROUP BY vg.vehicle_id
            )
            SELECT 
                pa.vehicle_id,
                pa.total_visits,
                pa.days_since_last_visit,
                pa.avg_days_between_visits,
                pa.last_visit_date,
                CASE 
                    WHEN pa.total_visits <= 1 THEN 'NEW_CLIENT'
                    WHEN pa.days_since_last_visit > (COALESCE(pa.avg_days_between_visits, 45) * 2.5) THEN 'LOST'
                    WHEN pa.days_since_last_visit > (COALESCE(pa.avg_days_between_visits, 45) * 1.8) THEN 'AT_RISK'
                    WHEN pa.avg_days_between_visits IS NULL OR pa.avg_days_between_visits > 90 THEN 'IRREGULAR'
                    ELSE 'REGULAR'
                END as regularity_status,
                CASE 
                    WHEN pa.avg_days_between_visits IS NOT NULL AND pa.avg_days_between_visits > 0
                    THEN pa.last_visit_date + (pa.avg_days_between_visits || ' days')::interval
                    ELSE NULL
                END as next_recommended_date
            FROM pattern_analysis pa
        """.trimIndent()

        val params = mapOf(
            "vehicleIds" to vehicleIds.map { it.value }.toLongArray(),
            "companyId" to companyId
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            val vehicleId = VehicleId.of(rs.getLong("vehicle_id"))
            val totalVisits = rs.getInt("total_visits")
            val daysSinceLastVisit = rs.getInt("days_since_last_visit").takeIf { !rs.wasNull() }
            val avgDaysBetweenVisits = rs.getDouble("avg_days_between_visits").takeIf { !rs.wasNull() }?.toInt()
            val nextRecommendedDate = rs.getDate("next_recommended_date")?.toLocalDate()
            val regularityStatus = VisitRegularityStatus.valueOf(rs.getString("regularity_status"))

            vehicleId to VehicleVisitPattern(
                vehicleId = vehicleId,
                companyId = companyId,
                daysSinceLastVisit = daysSinceLastVisit,
                averageDaysBetweenVisits = avgDaysBetweenVisits,
                visitRegularityStatus = regularityStatus,
                nextRecommendedVisitDate = nextRecommendedDate,
                totalVisits = totalVisits,
                calculatedAt = LocalDateTime.now()
            )
        }.toMap()
    }

    override fun getBatchServicePreferences(
        vehicleIds: List<VehicleId>,
        companyId: Long
    ): Map<VehicleId, VehicleServicePreferences> {
        if (vehicleIds.isEmpty()) return emptyMap()

        val sql = """
            WITH vehicle_filter AS (
                SELECT UNNEST(ARRAY[:vehicleIds]::bigint[]) as vehicle_id
            ),
            service_rankings AS (
                SELECT 
                    v.vehicle_id,
                    vs.service_id,
                    vs.name as service_name,
                    COUNT(*) as usage_count,
                    SUM(vs.final_price_brutto) as total_revenue,
                    AVG(vs.final_price_brutto) as average_price,
                    MAX(v.start_date) as last_used_date,
                    ROW_NUMBER() OVER (PARTITION BY v.vehicle_id ORDER BY COUNT(*) DESC, SUM(vs.final_price_brutto) DESC) as rank
                FROM visit_services vs
                JOIN visits v ON vs.visit_id = v.id
                JOIN vehicle_filter vf ON v.vehicle_id = vf.vehicle_id
                WHERE v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                GROUP BY v.vehicle_id, vs.service_id, vs.name
            )
            SELECT 
                sr.vehicle_id,
                sr.service_id,
                sr.service_name,
                sr.usage_count,
                sr.total_revenue,
                sr.average_price,
                sr.last_used_date
            FROM service_rankings sr
            WHERE sr.rank <= 10
            ORDER BY sr.vehicle_id, sr.rank
        """.trimIndent()

        val params = mapOf(
            "vehicleIds" to vehicleIds.map { it.value }.toLongArray(),
            "companyId" to companyId
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            val vehicleId = VehicleId.of(rs.getLong("vehicle_id"))
            val service = ServiceUsageSummary(
                serviceId = rs.getString("service_id"),
                serviceName = rs.getString("service_name"),
                usageCount = rs.getInt("usage_count"),
                totalRevenue = rs.getBigDecimal("total_revenue").setScale(2, RoundingMode.HALF_UP),
                averagePrice = rs.getBigDecimal("average_price").setScale(2, RoundingMode.HALF_UP),
                lastUsedDate = rs.getTimestamp("last_used_date")?.toLocalDateTime()
            )
            vehicleId to service
        }.groupBy({ it.first }, { it.second })
            .mapValues { (vehicleId, services) ->
                VehicleServicePreferences(
                    vehicleId = vehicleId,
                    companyId = companyId,
                    topServices = services,
                    calculatedAt = LocalDateTime.now()
                )
            }
    }

    override fun recalculateAnalytics(vehicleId: VehicleId, companyId: Long) {
        // For now, analytics are calculated on-demand
        // Could implement background recalculation here if needed
    }
}