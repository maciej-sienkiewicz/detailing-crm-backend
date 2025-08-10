package com.carslab.crm.modules.employees.domain.services

import com.carslab.crm.finances.domain.model.fixedcosts.*
import com.carslab.crm.finances.domain.ports.fixedcosts.FixedCostRepository
import com.carslab.crm.modules.employees.config.SalaryCostProperties
import com.carslab.crm.modules.employees.domain.events.EmployeeCreatedEvent
import com.carslab.crm.modules.employees.domain.events.EmployeeDeactivatedEvent
import com.carslab.crm.modules.employees.domain.events.EmployeeUpdatedEvent
import com.carslab.crm.modules.employees.domain.model.ContractType
import com.carslab.crm.modules.employees.infrastructure.persistence.SecureFixedCostService
import com.carslab.crm.domain.model.Audit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class SalaryFixedCostService(
    private val secureFixedCostService: SecureFixedCostService,
    private val salaryCostProperties: SalaryCostProperties
) {

    private val logger = LoggerFactory.getLogger(SalaryFixedCostService::class.java)

    fun createSalaryFixedCost(event: EmployeeCreatedEvent) {
        if (!salaryCostProperties.enableAutomaticCostCreation) {
            logger.debug("Automatic cost creation disabled - skipping for employee ${event.employeeId}")
            return
        }

        if (!event.hasSalaryInformation()) {
            logger.debug("Skipping fixed cost creation for employee ${event.employeeId} - no salary information")
            return
        }

        logger.info("Creating salary fixed cost for employee: ${event.employeeId}, company: ${event.companyId}")

        try {
            val grossSalary = event.calculateMonthlyGrossSalary(salaryCostProperties) ?: return
            val socialContributions = if (salaryCostProperties.enableSocialContributions) {
                calculateSocialContributions(grossSalary, event.contractType)
            } else {
                BigDecimal.ZERO
            }
            val totalMonthlyCost = BigDecimal.valueOf(grossSalary).add(socialContributions)

            val costName = salaryCostProperties.costNameTemplate.replace("{employeeName}", event.fullName)
            val costDescription = salaryCostProperties.costDescriptionTemplate
                .replace("{employeeName}", event.fullName)
                .replace("{position}", event.position)

            val fixedCost = FixedCost(
                id = FixedCostId.generate(),
                name = costName,
                description = costDescription,
                category = FixedCostCategory.PERSONNEL,
                monthlyAmount = totalMonthlyCost,
                frequency = CostFrequency.MONTHLY,
                startDate = event.hireDate,
                endDate = null,
                status = FixedCostStatus.ACTIVE,
                autoRenew = salaryCostProperties.autoRenewSalaryCosts,
                supplierInfo = null,
                contractNumber = null,
                notes = buildSalaryNotes(event, grossSalary, socialContributions),
                payments = emptyList(),
                audit = Audit(
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )

            val savedFixedCost = secureFixedCostService.createFixedCostForCompany(fixedCost, event.companyId)
            logger.info("Created salary fixed cost ${savedFixedCost.id.value} for employee ${event.employeeId}, company: ${event.companyId}, amount: ${totalMonthlyCost}")

        } catch (e: Exception) {
            logger.error("Failed to create salary fixed cost for employee ${event.employeeId}, company: ${event.companyId}", e)
            if (salaryCostProperties.retryFailedOperations) {
                logger.info("Will retry salary cost creation for employee ${event.employeeId}")
                throw e
            }
        }
    }

    fun updateSalaryFixedCost(event: EmployeeUpdatedEvent) {
        if (!salaryCostProperties.enableAutomaticCostCreation) {
            logger.debug("Automatic cost creation disabled - skipping update for employee ${event.employeeId}")
            return
        }

        if (!event.hasSalaryChanged()) {
            logger.debug("Skipping fixed cost update for employee ${event.employeeId} - no salary changes")
            return
        }

        logger.info("Updating salary fixed cost for employee: ${event.employeeId}, company: ${event.companyId}")

        try {
            val existingFixedCosts = secureFixedCostService.findSalaryFixedCostsForEmployee(event.employeeId, event.companyId)

            if (existingFixedCosts.isEmpty()) {
                logger.warn("No existing salary fixed cost found for employee ${event.employeeId} in company ${event.companyId}")

                if (event.newHourlyRate != null && event.newHourlyRate > 0.0) {
                    logger.info("Creating new salary fixed cost for employee ${event.employeeId}")
                    createSalaryFixedCostFromUpdate(event)
                }
                return
            }

            val existingFixedCost = existingFixedCosts.first()

            val newGrossSalary = event.calculateNewMonthlyGrossSalary(salaryCostProperties)
            if (newGrossSalary == null || newGrossSalary <= 0.0) {
                secureFixedCostService.deactivateFixedCost(existingFixedCost.id, event.companyId, "Wynagrodzenie zostało usunięte")
                return
            }

            val socialContributions = if (salaryCostProperties.enableSocialContributions) {
                calculateSocialContributions(newGrossSalary, event.contractType)
            } else {
                BigDecimal.ZERO
            }
            val totalMonthlyCost = BigDecimal.valueOf(newGrossSalary).add(socialContributions)

            val costName = salaryCostProperties.costNameTemplate.replace("{employeeName}", event.fullName)
            val costDescription = salaryCostProperties.costDescriptionTemplate
                .replace("{employeeName}", event.fullName)
                .replace("{position}", event.position)

            val updatedFixedCost = existingFixedCost.copy(
                name = costName,
                description = costDescription,
                monthlyAmount = totalMonthlyCost,
                notes = buildUpdatedSalaryNotes(event, newGrossSalary, socialContributions),
                audit = existingFixedCost.audit.copy(updatedAt = LocalDateTime.now())
            )

            val savedFixedCost = secureFixedCostService.updateFixedCostForCompany(updatedFixedCost, event.companyId)
            logger.info("Updated salary fixed cost ${savedFixedCost.id.value} for employee ${event.employeeId}, company: ${event.companyId}, new amount: ${totalMonthlyCost}")

        } catch (e: Exception) {
            logger.error("Failed to update salary fixed cost for employee ${event.employeeId}, company: ${event.companyId}", e)
            if (salaryCostProperties.retryFailedOperations) {
                throw e
            }
        }
    }

    fun deactivateSalaryFixedCost(event: EmployeeDeactivatedEvent) {
        if (!salaryCostProperties.deactivateOnEmployeeDeactivation) {
            logger.debug("Automatic deactivation disabled - skipping for employee ${event.employeeId}")
            return
        }

        logger.info("Deactivating salary fixed cost for employee: ${event.employeeId}, company: ${event.companyId}")

        try {
            val existingFixedCosts = secureFixedCostService.findSalaryFixedCostsForEmployee(event.employeeId, event.companyId)

            existingFixedCosts.forEach { fixedCost ->
                secureFixedCostService.deactivateFixedCost(fixedCost.id, event.companyId, "Pracownik został dezaktywowany")
            }

            logger.info("Successfully deactivated ${existingFixedCosts.size} salary fixed costs for employee ${event.employeeId}")

        } catch (e: Exception) {
            logger.error("Failed to deactivate salary fixed cost for employee ${event.employeeId}, company: ${event.companyId}", e)
            if (salaryCostProperties.retryFailedOperations) {
                throw e
            }
        }
    }

    private fun createSalaryFixedCostFromUpdate(event: EmployeeUpdatedEvent) {
        val grossSalary = event.calculateNewMonthlyGrossSalary(salaryCostProperties) ?: return
        val socialContributions = if (salaryCostProperties.enableSocialContributions) {
            calculateSocialContributions(grossSalary, event.contractType)
        } else {
            BigDecimal.ZERO
        }
        val totalMonthlyCost = BigDecimal.valueOf(grossSalary).add(socialContributions)

        val costName = salaryCostProperties.costNameTemplate.replace("{employeeName}", event.fullName)
        val costDescription = salaryCostProperties.costDescriptionTemplate
            .replace("{employeeName}", event.fullName)
            .replace("{position}", event.position)

        val fixedCost = FixedCost(
            id = FixedCostId.generate(),
            name = costName,
            description = costDescription,
            category = FixedCostCategory.PERSONNEL,
            monthlyAmount = totalMonthlyCost,
            frequency = CostFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            status = FixedCostStatus.ACTIVE,
            autoRenew = salaryCostProperties.autoRenewSalaryCosts,
            supplierInfo = null,
            contractNumber = null,
            notes = buildUpdatedSalaryNotes(event, grossSalary, socialContributions),
            payments = emptyList(),
            audit = Audit(
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val savedFixedCost = secureFixedCostService.createFixedCostForCompany(fixedCost, event.companyId)
        logger.info("Created new salary fixed cost ${savedFixedCost.id.value} from update for employee ${event.employeeId}, company: ${event.companyId}")
    }

    private fun calculateSocialContributions(grossSalary: Double, contractType: ContractType?): BigDecimal {
        return when (contractType) {
            ContractType.EMPLOYMENT -> {
                val zusEmployer = grossSalary * salaryCostProperties.employmentZusEmployerRate.toDouble()
                val fgsp = grossSalary * salaryCostProperties.employmentFgspRate.toDouble()
                val fp = grossSalary * salaryCostProperties.employmentFpRate.toDouble()
                BigDecimal.valueOf(zusEmployer + fgsp + fp).setScale(2, RoundingMode.HALF_UP)
            }
            ContractType.B2B -> {
                val zusEmployer = grossSalary * salaryCostProperties.b2bZusEmployerRate.toDouble()
                BigDecimal.valueOf(zusEmployer).setScale(2, RoundingMode.HALF_UP)
            }
            ContractType.MANDATE -> {
                val zusEmployer = grossSalary * salaryCostProperties.mandateZusEmployerRate.toDouble()
                BigDecimal.valueOf(zusEmployer).setScale(2, RoundingMode.HALF_UP)
            }
            null -> {
                val zusEmployer = grossSalary * salaryCostProperties.defaultContractZusEmployerRate.toDouble()
                BigDecimal.valueOf(zusEmployer).setScale(2, RoundingMode.HALF_UP)
            }
        }
    }

    private fun buildSalaryNotes(event: EmployeeCreatedEvent, grossSalary: Double, socialContributions: BigDecimal): String {
        val contractTypeText = getContractTypeDisplayName(event.contractType)
        val workingHours = event.workingHoursPerWeek ?: salaryCostProperties.defaultWorkingHoursPerWeek
        val hourlyRate = event.hourlyRate ?: 0.0

        return """
Typ kontraktu: $contractTypeText
Stawka godzinowa: ${String.format("%.2f", hourlyRate)} PLN
Godziny tygodniowo: ${String.format("%.1f", workingHours)}
Wynagrodzenie brutto: ${String.format("%.2f", grossSalary)} PLN
Składki pracodawcy: ${socialContributions} PLN
Utworzono automatycznie: ${LocalDate.now()}
        """.trimIndent()
    }

    private fun buildUpdatedSalaryNotes(event: EmployeeUpdatedEvent, grossSalary: Double, socialContributions: BigDecimal): String {
        val contractTypeText = getContractTypeDisplayName(event.contractType)
        val workingHours = event.newWorkingHours ?: salaryCostProperties.defaultWorkingHoursPerWeek
        val hourlyRate = event.newHourlyRate ?: 0.0
        val previousGross = event.calculatePreviousMonthlyGrossSalary(salaryCostProperties) ?: 0.0

        return """
Typ kontraktu: $contractTypeText
Stawka godzinowa: ${String.format("%.2f", hourlyRate)} PLN (poprzednio: ${String.format("%.2f", event.previousHourlyRate ?: 0.0)})
Godziny tygodniowo: ${String.format("%.1f", workingHours)} (poprzednio: ${String.format("%.1f", event.previousWorkingHours ?: salaryCostProperties.defaultWorkingHoursPerWeek)})
Wynagrodzenie brutto: ${String.format("%.2f", grossSalary)} PLN (poprzednio: ${String.format("%.2f", previousGross)})
Składki pracodawcy: ${socialContributions} PLN
Zaktualizowano: ${LocalDate.now()}
        """.trimIndent()
    }

    private fun getContractTypeDisplayName(contractType: ContractType?): String {
        return when (contractType) {
            ContractType.EMPLOYMENT -> "Umowa o pracę"
            ContractType.B2B -> "Kontrakt B2B"
            ContractType.MANDATE -> "Umowa zlecenie"
            null -> "Nie określono"
        }
    }
}

private fun EmployeeCreatedEvent.calculateMonthlyGrossSalary(config: SalaryCostProperties): Double? {
    return hourlyRate?.let { rate ->
        val hoursPerWeek = workingHoursPerWeek ?: config.defaultWorkingHoursPerWeek
        val hoursPerMonth = hoursPerWeek * config.weeksPerMonth
        rate * hoursPerMonth
    }
}

private fun EmployeeUpdatedEvent.calculateNewMonthlyGrossSalary(config: SalaryCostProperties): Double? {
    return newHourlyRate?.let { rate ->
        val hoursPerWeek = newWorkingHours ?: config.defaultWorkingHoursPerWeek
        val hoursPerMonth = hoursPerWeek * config.weeksPerMonth
        rate * hoursPerMonth
    }
}

private fun EmployeeUpdatedEvent.calculatePreviousMonthlyGrossSalary(config: SalaryCostProperties): Double? {
    return previousHourlyRate?.let { rate ->
        val hoursPerWeek = previousWorkingHours ?: config.defaultWorkingHoursPerWeek
        val hoursPerMonth = hoursPerWeek * config.weeksPerMonth
        rate * hoursPerMonth
    }
}