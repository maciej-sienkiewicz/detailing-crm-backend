package com.carslab.crm.finances.domain

import com.carslab.crm.api.model.*
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenAnalysis
import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfiguration
import com.carslab.crm.finances.domain.model.fixedcosts.BreakevenConfigurationId
import com.carslab.crm.finances.domain.model.fixedcosts.FixedCostCategory
import com.carslab.crm.finances.domain.model.fixedcosts.RiskLevel
import com.carslab.crm.finances.domain.model.fixedcosts.SafetyMarginInfo
import com.carslab.crm.finances.domain.model.fixedcosts.TrendDirection
import com.carslab.crm.finances.domain.ports.fixedcosts.BreakevenConfigurationRepository
import com.carslab.crm.finances.domain.ports.fixedcosts.FixedCostRepository
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class BreakevenService(
    private val breakevenConfigurationRepository: BreakevenConfigurationRepository,
    private val fixedCostRepository: FixedCostRepository
) {

    private val logger = LoggerFactory.getLogger(BreakevenService::class.java)

    /**
     * Tworzy lub aktualizuje konfigurację break-even
     */
    @Transactional
    fun saveConfiguration(request: BreakevenConfigurationRequest): BreakevenConfiguration {
        logger.info("Saving break-even configuration: {}", request.name)

        validateConfigurationRequest(request)

        // Dezaktywuj wszystkie istniejące konfiguracje
        breakevenConfigurationRepository.deactivateAll()

        val configuration = BreakevenConfiguration(
            id = BreakevenConfigurationId.generate(),
            name = request.name,
            description = request.description,
            averageServicePrice = request.averageServicePrice,
            averageMarginPercentage = request.averageMarginPercentage,
            workingDaysPerMonth = request.workingDaysPerMonth,
            targetServicesPerDay = request.targetServicesPerDay,
            isActive = request.isActive,
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val savedConfiguration = breakevenConfigurationRepository.save(configuration)
        logger.info("Saved break-even configuration with ID: {}", savedConfiguration.id.value)

        return savedConfiguration
    }

    /**
     * Pobiera aktywną konfigurację break-even
     */
    fun getActiveConfiguration(): BreakevenConfiguration {
        return breakevenConfigurationRepository.findActiveConfiguration()
            ?: throw ResourceNotFoundException("Active BreakevenConfiguration", "none")
    }

    /**
     * Przeprowadza analizę break-even dla określonego okresu
     */
    fun getBreakevenAnalysis(period: LocalDate? = null): BreakevenAnalysisResponse {
        logger.debug("Performing break-even analysis for period: {}", period)

        val analysisPeriod = period ?: LocalDate.now()
        val configuration = getActiveConfiguration()

        val startOfMonth = analysisPeriod.withDayOfMonth(1)
        val endOfMonth = analysisPeriod.withDayOfMonth(analysisPeriod.lengthOfMonth())

        val totalFixedCosts = fixedCostRepository.calculateTotalFixedCostsForPeriod(startOfMonth, endOfMonth)
        val contributionMargin = configuration.calculateContributionMargin()

        val breakevenPointServices = if (contributionMargin > BigDecimal.ZERO) {
            totalFixedCosts.divide(contributionMargin, 0, RoundingMode.CEILING).toInt()
        } else 0

        val breakevenPointRevenue = BigDecimal(breakevenPointServices).multiply(configuration.averageServicePrice)

        val requiredServicesPerDay = if (configuration.workingDaysPerMonth > 0) {
            (breakevenPointServices + configuration.workingDaysPerMonth - 1) / configuration.workingDaysPerMonth
        } else 0

        val workingDaysNeeded = if (configuration.targetServicesPerDay != null && configuration.targetServicesPerDay > 0) {
            (breakevenPointServices + configuration.targetServicesPerDay - 1) / configuration.targetServicesPerDay
        } else configuration.workingDaysPerMonth

        val costBreakdown = createCostBreakdown(startOfMonth, endOfMonth)
        val safetyMargin = calculateSafetyMargin(totalFixedCosts, configuration)
        val recommendations = generateRecommendations(breakevenPointServices, workingDaysNeeded, configuration)

        val analysis = BreakevenAnalysis(
            period = analysisPeriod,
            configuration = configuration,
            totalFixedCosts = totalFixedCosts,
            contributionMarginPerService = contributionMargin,
            breakevenPointServices = breakevenPointServices,
            breakevenPointRevenue = breakevenPointRevenue,
            requiredServicesPerDay = requiredServicesPerDay,
            workingDaysNeeded = workingDaysNeeded,
            costBreakdown = costBreakdown,
            safetyMarginInfo = SafetyMarginInfo(
                currentMonthlyRevenue = safetyMargin.currentMonthlyRevenue,
                safetyMarginAmount = safetyMargin.safetyMarginAmount,
                safetyMarginPercentage = safetyMargin.safetyMarginPercentage,
                riskLevel = safetyMargin.riskLevel
            )
        )

        return BreakevenAnalysisResponse(
            period = analysis.period,
            configuration = configuration.toResponse(),
            totalFixedCosts = analysis.totalFixedCosts,
            contributionMarginPerService = analysis.contributionMarginPerService,
            breakevenPointServices = analysis.breakevenPointServices,
            breakevenPointRevenue = analysis.breakevenPointRevenue,
            requiredServicesPerDay = analysis.requiredServicesPerDay,
            workingDaysNeeded = analysis.workingDaysNeeded,
            isAchievableInMonth = analysis.isAchievableInMonth(),
            costBreakdown = costBreakdown.map { (category, amount) ->
                val percentage = if (totalFixedCosts > BigDecimal.ZERO) {
                    amount.divide(totalFixedCosts, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
                } else BigDecimal.ZERO

                CategoryBreakdownItem(
                    category = category,
                    categoryDisplay = category.displayName,
                    totalAmount = amount,
                    percentage = percentage,
                    activeCosts = 0, // TODO: Calculate from repository
                    trend = TrendDirection.STABLE, // TODO: Implement trend calculation
                    topCosts = emptyList() // TODO: Get top costs for category
                )
            },
            safetyMargin = SafetyMarginResponse(
                currentMonthlyRevenue = safetyMargin.currentMonthlyRevenue,
                safetyMarginAmount = safetyMargin.safetyMarginAmount,
                safetyMarginPercentage = safetyMargin.safetyMarginPercentage,
                riskLevel = safetyMargin.riskLevel,
                riskLevelDisplay = safetyMargin.riskLevel.displayName,
                riskLevelColor = safetyMargin.riskLevel.color
            ),
            recommendations = recommendations
        )
    }

    /**
     * Generuje projekcje finansowe
     */
    fun getProjections(months: Int = 12): FinancialProjectionsResponse {
        logger.debug("Generating financial projections for {} months", months)

        val configuration = getActiveConfiguration()
        val currentDate = LocalDate.now()

        val projections = (0 until months).map { monthOffset ->
            val projectionDate = currentDate.plusMonths(monthOffset.toLong())
            val startOfMonth = projectionDate.withDayOfMonth(1)
            val endOfMonth = projectionDate.withDayOfMonth(projectionDate.lengthOfMonth())

            val fixedCosts = fixedCostRepository.calculateTotalFixedCostsForPeriod(startOfMonth, endOfMonth)
            val contributionMargin = configuration.calculateContributionMargin()

            val breakevenPoint = if (contributionMargin > BigDecimal.ZERO) {
                fixedCosts.divide(contributionMargin, 0, RoundingMode.CEILING).toInt()
            } else 0

            val projectedServices = configuration.targetServicesPerDay?.let { servicesPerDay ->
                servicesPerDay * configuration.workingDaysPerMonth
            } ?: breakevenPoint

            val projectedRevenue = BigDecimal(projectedServices).multiply(configuration.averageServicePrice)
            val projectedProfit = projectedRevenue.subtract(fixedCosts)

            val coverageRatio = if (fixedCosts > BigDecimal.ZERO) {
                projectedRevenue.divide(fixedCosts, 4, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO

            FinancialProjectionItem(
                month = projectionDate,
                fixedCosts = fixedCosts,
                projectedRevenue = projectedRevenue,
                projectedProfit = projectedProfit,
                breakevenPoint = breakevenPoint,
                actualServices = null, // TODO: Get from actual data if available
                actualRevenue = null, // TODO: Get from actual data if available
                variance = null, // TODO: Calculate variance if actual data available
                isProfitable = projectedProfit > BigDecimal.ZERO,
                coverageRatio = coverageRatio
            )
        }

        val summary = ProjectionSummary(
            totalPeriods = projections.size,
            profitableMonths = projections.count { it.isProfitable },
            averageFixedCosts = projections.map { it.fixedCosts }.fold(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal(projections.size), 2, RoundingMode.HALF_UP),
            averageBreakevenPoint = projections.map { it.breakevenPoint }.average().toInt(),
            bestMonth = projections.maxByOrNull { it.projectedProfit },
            worstMonth = projections.minByOrNull { it.projectedProfit }
        )

        return FinancialProjectionsResponse(
            projections = projections,
            summary = summary
        )
    }

    // ============ PRIVATE METHODS ============

    private fun createCostBreakdown(startDate: LocalDate, endDate: LocalDate): Map<FixedCostCategory, BigDecimal> {
        return fixedCostRepository.getCategorySummary(startDate)
    }

    private fun calculateSafetyMargin(
        totalFixedCosts: BigDecimal,
        configuration: BreakevenConfiguration
    ): SafetyMarginInfo {
        // TODO: Get actual revenue from financial documents
        val currentMonthlyRevenue = BigDecimal("50000") // Placeholder

        val safetyMarginAmount = currentMonthlyRevenue.subtract(totalFixedCosts)
        val safetyMarginPercentage = if (currentMonthlyRevenue > BigDecimal.ZERO) {
            safetyMarginAmount.divide(currentMonthlyRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else BigDecimal.ZERO

        val riskLevel = when {
            safetyMarginPercentage < BigDecimal("10") -> RiskLevel.CRITICAL
            safetyMarginPercentage < BigDecimal("20") -> RiskLevel.HIGH
            safetyMarginPercentage < BigDecimal("40") -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return SafetyMarginInfo(
            currentMonthlyRevenue = currentMonthlyRevenue,
            safetyMarginAmount = safetyMarginAmount,
            safetyMarginPercentage = safetyMarginPercentage,
            riskLevel = riskLevel
        )
    }

    private fun generateRecommendations(
        breakevenPoint: Int,
        workingDaysNeeded: Int,
        configuration: BreakevenConfiguration
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (workingDaysNeeded > configuration.workingDaysPerMonth) {
            recommendations.add("Punkt break-even wymaga więcej dni roboczych niż dostępne w miesiącu. Rozważ zwiększenie ceny usług lub redukcję kosztów stałych.")
        }

        val servicesPerDay = configuration.targetServicesPerDay ?: 1
        if (breakevenPoint > servicesPerDay * configuration.workingDaysPerMonth) {
            recommendations.add("Obecna pojemność świadczenia usług jest niewystarczająca. Rozważ zwiększenie wydajności lub zespołu.")
        }

        if (configuration.averageMarginPercentage < BigDecimal("50")) {
            recommendations.add("Marża na usługach jest relatywnie niska. Sprawdź możliwości optymalizacji kosztów zmiennych.")
        }

        return recommendations
    }

    private fun validateConfigurationRequest(request: BreakevenConfigurationRequest) {
        if (request.averageServicePrice <= BigDecimal.ZERO) {
            throw ValidationException("Average service price must be greater than zero")
        }

        if (request.averageMarginPercentage <= BigDecimal.ZERO || request.averageMarginPercentage > BigDecimal("100")) {
            throw ValidationException("Average margin percentage must be between 0 and 100")
        }

        if (request.workingDaysPerMonth < 1 || request.workingDaysPerMonth > 31) {
            throw ValidationException("Working days per month must be between 1 and 31")
        }

        if (request.targetServicesPerDay != null && request.targetServicesPerDay < 1) {
            throw ValidationException("Target services per day must be at least 1")
        }
    }
}

// Extension function for BreakevenConfiguration
private fun BreakevenConfiguration.toResponse(): BreakevenConfigurationResponse {
    return BreakevenConfigurationResponse(
        id = id.value,
        name = name,
        description = description,
        averageServicePrice = averageServicePrice,
        averageMarginPercentage = averageMarginPercentage,
        contributionMargin = calculateContributionMargin(),
        variableCost = calculateVariableCost(),
        workingDaysPerMonth = workingDaysPerMonth,
        targetServicesPerDay = targetServicesPerDay,
        isActive = isActive,
        createdAt = audit.createdAt,
        updatedAt = audit.updatedAt
    )
}