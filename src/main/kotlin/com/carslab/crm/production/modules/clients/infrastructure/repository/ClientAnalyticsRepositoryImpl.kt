package com.carslab.crm.production.modules.clients.infrastructure.repository

import com.carslab.crm.production.modules.clients.domain.model.*
import com.carslab.crm.production.modules.clients.domain.repository.ClientAnalyticsRepository
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.Month

@Repository
class ClientAnalyticsRepositoryImpl(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : ClientAnalyticsRepository {

    override fun getClientAnalytics(clientId: ClientId, companyId: Long): ClientAnalytics? {
        val sql = """
            WITH client_visit_data AS (
                SELECT 
                    v.client_id,
                    v.start_date,
                    COALESCE(service_totals.total_revenue, 0) as visit_revenue,
                    MIN(v.start_date) OVER () as first_visit_date,
                    MAX(v.start_date) OVER () as last_visit_date
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id,
                        SUM(vs.final_price * vs.quantity) as total_revenue
                    FROM visit_services vs
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.client_id = :clientId 
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
            )
            SELECT 
                client_id,
                COUNT(*) as total_visits,
                AVG(visit_revenue) as avg_visit_value,
                SUM(visit_revenue) as total_revenue,
                EXTRACT(EPOCH FROM (MAX(last_visit_date) - MIN(first_visit_date))) / (30.44 * 24 * 60 * 60) as months_since_first,
                CURRENT_DATE - MAX(last_visit_date)::date as days_since_last
            FROM client_visit_data
            WHERE visit_revenue > 0
            GROUP BY client_id, first_visit_date, last_visit_date
        """.trimIndent()

        val params = mapOf(
            "clientId" to clientId.value,
            "companyId" to companyId
        )

        return try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ ->
                ClientAnalytics(
                    clientId = clientId,
                    companyId = companyId,
                    averageVisitValue = rs.getBigDecimal("avg_visit_value")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                    totalRevenue = rs.getBigDecimal("total_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                    totalVisits = rs.getInt("total_visits"),
                    monthsSinceFirstVisit = rs.getDouble("months_since_first").toInt().coerceAtLeast(0),
                    daysSinceLastVisit = rs.getInt("days_since_last").takeIf { !rs.wasNull() },
                    calculatedAt = LocalDateTime.now()
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun getRevenueTrend(clientId: ClientId, companyId: Long): ClientRevenueTrend? {
        val sql = """
            WITH monthly_revenue AS (
                SELECT 
                    v.client_id,
                    v.start_date,
                    COALESCE(service_totals.total_revenue, 0) as visit_revenue,
                    CASE 
                        WHEN v.start_date >= CURRENT_DATE - INTERVAL '3 months' THEN 'recent'
                        WHEN v.start_date >= CURRENT_DATE - INTERVAL '6 months' 
                             AND v.start_date < CURRENT_DATE - INTERVAL '3 months' THEN 'previous'
                        ELSE 'older'
                    END as period
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id,
                        SUM(vs.final_price * vs.quantity) as total_revenue
                    FROM visit_services vs
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.client_id = :clientId 
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                  AND v.start_date >= CURRENT_DATE - INTERVAL '6 months'
            ),
            period_totals AS (
                SELECT 
                    client_id,
                    SUM(CASE WHEN period = 'recent' THEN visit_revenue ELSE 0 END) as recent_revenue,
                    SUM(CASE WHEN period = 'previous' THEN visit_revenue ELSE 0 END) as previous_revenue
                FROM monthly_revenue
                WHERE visit_revenue > 0
                GROUP BY client_id
            )
            SELECT 
                client_id,
                recent_revenue,
                previous_revenue,
                CASE 
                    WHEN previous_revenue > 0 
                    THEN ((recent_revenue - previous_revenue) / previous_revenue) * 100
                    WHEN recent_revenue > 0 AND previous_revenue = 0 
                    THEN 100
                    ELSE 0
                END as trend_percentage
            FROM period_totals
        """.trimIndent()

        val params = mapOf(
            "clientId" to clientId.value,
            "companyId" to companyId
        )

        return try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ ->
                val recentRevenue = rs.getBigDecimal("recent_revenue") ?: BigDecimal.ZERO
                val previousRevenue = rs.getBigDecimal("previous_revenue") ?: BigDecimal.ZERO
                val trendPercentage = rs.getBigDecimal("trend_percentage")?.setScale(1, RoundingMode.HALF_UP) ?: BigDecimal.ZERO

                ClientRevenueTrend(
                    clientId = clientId,
                    companyId = companyId,
                    recentRevenue = recentRevenue.setScale(2, RoundingMode.HALF_UP),
                    previousRevenue = previousRevenue.setScale(2, RoundingMode.HALF_UP),
                    trendPercentage = trendPercentage,
                    trendDirection = ClientRevenueTrend.calculateTrendDirection(trendPercentage),
                    calculatedAt = LocalDateTime.now()
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun getSeasonalityAnalysis(clientId: ClientId, companyId: Long): ClientSeasonalityAnalysis {
        val sql = """
            WITH monthly_data AS (
                SELECT 
                    EXTRACT(MONTH FROM v.start_date) as month_num,
                    COUNT(*) as visit_count,
                    COALESCE(SUM(service_totals.total_revenue), 0) as monthly_revenue
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id,
                        SUM(vs.final_price * vs.quantity) as total_revenue
                    FROM visit_services vs
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.client_id = :clientId 
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                  AND v.start_date >= CURRENT_DATE - INTERVAL '24 months' -- Last 2 years for better seasonality
                GROUP BY EXTRACT(MONTH FROM v.start_date)
            )
            SELECT 
                month_num,
                visit_count,
                monthly_revenue,
                CASE WHEN visit_count > 0 THEN monthly_revenue / visit_count ELSE 0 END as avg_visit_value
            FROM monthly_data
            ORDER BY month_num
        """.trimIndent()

        val params = mapOf(
            "clientId" to clientId.value,
            "companyId" to companyId
        )

        val monthlyData = jdbcTemplate.query(sql, params) { rs, _ ->
            MonthlyVisitData(
                month = Month.of(rs.getInt("month_num")),
                visitCount = rs.getInt("visit_count"),
                revenue = rs.getBigDecimal("monthly_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                averageVisitValue = rs.getBigDecimal("avg_visit_value")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
            )
        }

        val peakMonth = monthlyData.maxByOrNull { it.visitCount }?.month
        val leastActiveMonth = monthlyData.filter { it.visitCount > 0 }.minByOrNull { it.visitCount }?.month

        return ClientSeasonalityAnalysis(
            clientId = clientId,
            companyId = companyId,
            monthlyData = monthlyData,
            peakMonth = peakMonth,
            leastActiveMonth = leastActiveMonth,
            calculatedAt = LocalDateTime.now()
        )
    }

    override fun getTopServices(clientId: ClientId, companyId: Long, limit: Int): List<ClientServiceUsage> {
        val sql = """
            SELECT 
                vs.service_id,
                vs.name,
                COUNT(*) as usage_count,
                SUM(vs.final_price * vs.quantity) as total_revenue,
                AVG(vs.final_price) as average_price,
                MAX(v.start_date) as last_used_date
            FROM visit_services vs
            JOIN visits v ON vs.visit_id = v.id
            WHERE v.client_id = :clientId 
              AND v.company_id = :companyId
              AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
            GROUP BY vs.service_id, vs.name
            ORDER BY usage_count DESC, total_revenue DESC
            LIMIT :limit
        """.trimIndent()

        val params = mapOf(
            "clientId" to clientId.value,
            "companyId" to companyId,
            "limit" to limit
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            ClientServiceUsage(
                serviceId = rs.getString("service_id"),
                serviceName = rs.getString("name"),
                usageCount = rs.getInt("usage_count"),
                totalRevenue = rs.getBigDecimal("total_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                averagePrice = rs.getBigDecimal("average_price")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                lastUsedDate = rs.getTimestamp("last_used_date")?.toLocalDateTime()
            )
        }
    }

    override fun getReferralSources(clientId: ClientId, companyId: Long): List<ClientReferralSource> {
        val sql = """
            SELECT 
                COALESCE(v.referral_source::text, 'UNKNOWN') as source,
                COUNT(*) as visit_count,
                MIN(v.start_date) as first_visit_date,
                COALESCE(SUM(service_totals.total_revenue), 0) as total_revenue
            FROM visits v
            LEFT JOIN (
                SELECT 
                    vs.visit_id,
                    SUM(vs.final_price * vs.quantity) as total_revenue
                FROM visit_services vs
                GROUP BY vs.visit_id
            ) service_totals ON v.id = service_totals.visit_id
            WHERE v.client_id = :clientId 
              AND v.company_id = :companyId
              AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
            GROUP BY v.referral_source
            ORDER BY visit_count DESC
        """.trimIndent()

        val params = mapOf(
            "clientId" to clientId.value,
            "companyId" to companyId
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            ClientReferralSource(
                source = rs.getString("source"),
                visitCount = rs.getInt("visit_count"),
                firstVisitDate = rs.getTimestamp("first_visit_date").toLocalDateTime(),
                totalRevenue = rs.getBigDecimal("total_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
            )
        }
    }

    override fun getClientGrowthChart(clientId: ClientId, companyId: Long, months: Int): List<ClientMonthlyRevenue> {
        val sql = """
            WITH monthly_buckets AS (
                SELECT 
                    generate_series(
                        date_trunc('month', CURRENT_DATE - INTERVAL ':months months'),
                        date_trunc('month', CURRENT_DATE),
                        INTERVAL '1 month'
                    )::date as month_start
            ),
            monthly_revenue AS (
                SELECT 
                    date_trunc('month', v.start_date)::date as month_start,
                    COUNT(*) as visit_count,
                    COALESCE(SUM(service_totals.total_revenue), 0) as monthly_revenue
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id,
                        SUM(vs.final_price * vs.quantity) as total_revenue
                    FROM visit_services vs
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.client_id = :clientId 
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                  AND v.start_date >= CURRENT_DATE - INTERVAL ':months months'
                GROUP BY date_trunc('month', v.start_date)::date
            )
            SELECT 
                mb.month_start,
                EXTRACT(YEAR FROM mb.month_start)::int as year,
                EXTRACT(MONTH FROM mb.month_start)::int as month,
                COALESCE(mr.visit_count, 0) as visit_count,
                COALESCE(mr.monthly_revenue, 0) as revenue,
                SUM(COALESCE(mr.monthly_revenue, 0)) OVER (ORDER BY mb.month_start ROWS UNBOUNDED PRECEDING) as cumulative_revenue
            FROM monthly_buckets mb
            LEFT JOIN monthly_revenue mr ON mb.month_start = mr.month_start
            ORDER BY mb.month_start
        """.trimIndent()

        val params = mapOf(
            "clientId" to clientId.value,
            "companyId" to companyId,
            "months" to months
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            ClientMonthlyRevenue(
                year = rs.getInt("year"),
                month = Month.of(rs.getInt("month")),
                revenue = rs.getBigDecimal("revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                visitCount = rs.getInt("visit_count"),
                cumulativeRevenue = rs.getBigDecimal("cumulative_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
            )
        }
    }

    override fun getCompanyAverages(companyId: Long): CompanyAverages {
        val sql = """
            WITH client_metrics AS (
                SELECT 
                    v.client_id,
                    COUNT(*) as total_visits,
                    COALESCE(SUM(service_totals.total_revenue), 0) as total_revenue,
                    AVG(COALESCE(service_totals.total_revenue, 0)) as avg_visit_value,
                    MIN(v.start_date) as first_visit,
                    MAX(v.start_date) as last_visit
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id,
                        SUM(vs.final_price * vs.quantity) as total_revenue
                    FROM visit_services vs
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                  AND v.start_date >= CURRENT_DATE - INTERVAL '24 months'
                GROUP BY v.client_id
            ),
            company_stats AS (
                SELECT 
                    AVG(avg_visit_value) as avg_visit_value,
                    AVG(total_revenue / GREATEST(EXTRACT(EPOCH FROM (last_visit - first_visit)) / (30.44 * 24 * 60 * 60), 1)) as avg_monthly_revenue,
                    AVG(total_visits / GREATEST(EXTRACT(EPOCH FROM (last_visit - first_visit)) / (30.44 * 24 * 60 * 60), 1)) as avg_visits_per_month,
                    AVG(EXTRACT(EPOCH FROM (last_visit - first_visit)) / (30.44 * 24 * 60 * 60)) as avg_lifespan_months
                FROM client_metrics
                WHERE total_revenue > 0
            )
            SELECT 
                COALESCE(avg_visit_value, 0) as avg_visit_value,
                COALESCE(avg_monthly_revenue, 0) as avg_monthly_revenue,
                COALESCE(avg_visits_per_month, 0) as avg_visits_per_month,
                COALESCE(avg_lifespan_months, 0)::int as avg_lifespan_months
            FROM company_stats
        """.trimIndent()

        val params = mapOf("companyId" to companyId)

        return jdbcTemplate.queryForObject(sql, params) { rs, _ ->
            CompanyAverages(
                companyId = companyId,
                averageVisitValue = rs.getBigDecimal("avg_visit_value")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                averageMonthlyRevenue = rs.getBigDecimal("avg_monthly_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                averageVisitsPerMonth = rs.getDouble("avg_visits_per_month"),
                averageClientLifespanMonths = rs.getInt("avg_lifespan_months").coerceAtLeast(0),
                calculatedAt = LocalDateTime.now()
            )
        }!!
    }

    override fun getBatchClientAnalytics(clientIds: List<ClientId>, companyId: Long): Map<ClientId, ClientAnalytics> {
        if (clientIds.isEmpty()) return emptyMap()

        val sql = """
            WITH client_visit_data AS (
                SELECT 
                    v.client_id,
                    COUNT(*) as total_visits,
                    AVG(COALESCE(service_totals.total_revenue, 0)) as avg_visit_value,
                    SUM(COALESCE(service_totals.total_revenue, 0)) as total_revenue,
                    MIN(v.start_date) as first_visit_date,
                    MAX(v.start_date) as last_visit_date
                FROM visits v
                LEFT JOIN (
                    SELECT 
                        vs.visit_id,
                        SUM(vs.final_price * vs.quantity) as total_revenue
                    FROM visit_services vs
                    GROUP BY vs.visit_id
                ) service_totals ON v.id = service_totals.visit_id
                WHERE v.client_id = ANY(:clientIds)
                  AND v.company_id = :companyId
                  AND v.status IN ('COMPLETED', 'READY_FOR_PICKUP')
                GROUP BY v.client_id
            )
            SELECT 
                client_id,
                total_visits,
                avg_visit_value,
                total_revenue,
                EXTRACT(EPOCH FROM (last_visit_date - first_visit_date)) / (30.44 * 24 * 60 * 60) as months_since_first,
                CURRENT_DATE - last_visit_date::date as days_since_last
            FROM client_visit_data
            WHERE total_revenue > 0
        """.trimIndent()

        val params = mapOf(
            "clientIds" to clientIds.map { it.value }.toLongArray(),
            "companyId" to companyId
        )

        return jdbcTemplate.query(sql, params) { rs, _ ->
            val clientId = ClientId.of(rs.getLong("client_id"))
            clientId to ClientAnalytics(
                clientId = clientId,
                companyId = companyId,
                averageVisitValue = rs.getBigDecimal("avg_visit_value")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                totalRevenue = rs.getBigDecimal("total_revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                totalVisits = rs.getInt("total_visits"),
                monthsSinceFirstVisit = rs.getDouble("months_since_first").toInt().coerceAtLeast(0),
                daysSinceLastVisit = rs.getInt("days_since_last").takeIf { !rs.wasNull() },
                calculatedAt = LocalDateTime.now()
            )
        }.toMap()
    }

    override fun getClientComparison(clientId: ClientId, companyId: Long): ClientComparisonMetrics? {
        val clientAnalytics = getClientAnalytics(clientId, companyId) ?: return null
        val companyAverages = getCompanyAverages(companyId)

        val visitValueComparison = createComparisonMetric(
            clientAnalytics.averageVisitValue,
            companyAverages.averageVisitValue
        )

        val monthlyRevenue = if (clientAnalytics.monthsSinceFirstVisit > 0) {
            clientAnalytics.totalRevenue.divide(
                BigDecimal(clientAnalytics.monthsSinceFirstVisit),
                2,
                RoundingMode.HALF_UP
            )
        } else clientAnalytics.totalRevenue

        val monthlyRevenueComparison = createComparisonMetric(
            monthlyRevenue,
            companyAverages.averageMonthlyRevenue
        )

        val visitsFrequency = if (clientAnalytics.monthsSinceFirstVisit > 0) {
            BigDecimal(clientAnalytics.totalVisits).divide(
                BigDecimal(clientAnalytics.monthsSinceFirstVisit),
                2,
                RoundingMode.HALF_UP
            )
        } else BigDecimal.ZERO

        val visitsFrequencyComparison = createComparisonMetric(
            visitsFrequency,
            BigDecimal(companyAverages.averageVisitsPerMonth)
        )

        val lifespanComparison = createComparisonMetric(
            BigDecimal(clientAnalytics.monthsSinceFirstVisit),
            BigDecimal(companyAverages.averageClientLifespanMonths)
        )

        val overallScore = calculateClientScore(
            visitValueComparison,
            monthlyRevenueComparison,
            visitsFrequencyComparison,
            lifespanComparison
        )

        return ClientComparisonMetrics(
            clientId = clientId,
            companyId = companyId,
            visitValueVsAverage = visitValueComparison,
            monthlyRevenueVsAverage = monthlyRevenueComparison,
            visitsFrequencyVsAverage = visitsFrequencyComparison,
            lifespanVsAverage = lifespanComparison,
            overallScore = overallScore,
            calculatedAt = LocalDateTime.now()
        )
    }

    private fun createComparisonMetric(clientValue: BigDecimal, companyAverage: BigDecimal): ComparisonMetric {
        val percentageDifference = if (companyAverage > BigDecimal.ZERO) {
            ((clientValue - companyAverage).divide(companyAverage, 4, RoundingMode.HALF_UP))
                .multiply(BigDecimal(100))
                .setScale(1, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val performanceLevel = when {
            percentageDifference > BigDecimal("50") -> PerformanceLevel.EXCELLENT
            percentageDifference > BigDecimal("20") -> PerformanceLevel.GOOD
            percentageDifference >= BigDecimal("-20") -> PerformanceLevel.AVERAGE
            percentageDifference >= BigDecimal("-50") -> PerformanceLevel.BELOW
            else -> PerformanceLevel.POOR
        }

        return ComparisonMetric(
            clientValue = clientValue,
            companyAverage = companyAverage,
            percentageDifference = percentageDifference,
            performanceLevel = performanceLevel
        )
    }

    private fun calculateClientScore(
        visitValue: ComparisonMetric,
        monthlyRevenue: ComparisonMetric,
        visitsFrequency: ComparisonMetric,
        lifespan: ComparisonMetric
    ): ClientScore {
        val excellentCount = listOf(visitValue, monthlyRevenue, visitsFrequency, lifespan)
            .count { it.performanceLevel == PerformanceLevel.EXCELLENT }
        val goodCount = listOf(visitValue, monthlyRevenue, visitsFrequency, lifespan)
            .count { it.performanceLevel == PerformanceLevel.GOOD }
        val poorCount = listOf(visitValue, monthlyRevenue, visitsFrequency, lifespan)
            .count { it.performanceLevel == PerformanceLevel.POOR }

        return when {
            excellentCount >= 3 -> ClientScore.VIP
            excellentCount >= 2 || (excellentCount >= 1 && goodCount >= 2) -> ClientScore.HIGH_VALUE
            poorCount >= 3 -> ClientScore.AT_RISK
            poorCount >= 2 -> ClientScore.LOW_VALUE
            else -> ClientScore.AVERAGE
        }
    }
}