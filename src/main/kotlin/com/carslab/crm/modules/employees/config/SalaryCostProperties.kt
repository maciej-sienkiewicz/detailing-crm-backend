package com.carslab.crm.modules.employees.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Configuration
@ConfigurationProperties(prefix = "app.salary-costs")
@Validated
data class SalaryCostProperties(
    @field:NotNull
    var enableAutomaticCostCreation: Boolean = true,

    @field:NotNull
    var enableSocialContributions: Boolean = true,

    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    var employmentZusEmployerRate: BigDecimal = BigDecimal("0.1952"),

    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    var employmentFgspRate: BigDecimal = BigDecimal("0.0245"),

    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    var employmentFpRate: BigDecimal = BigDecimal("0.010"),

    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    var mandateZusEmployerRate: BigDecimal = BigDecimal("0.0976"),

    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    var b2bZusEmployerRate: BigDecimal = BigDecimal("0.0"),

    @field:NotNull
    @field:DecimalMin("0.0")
    @field:DecimalMax("1.0")
    var defaultContractZusEmployerRate: BigDecimal = BigDecimal("0.1952"),

    @field:NotNull
    @field:DecimalMin("1.0")
    @field:DecimalMax("168.0")
    var defaultWorkingHoursPerWeek: Double = 40.0,

    @field:NotNull
    @field:DecimalMin("4.0")
    @field:DecimalMax("5.0")
    var weeksPerMonth: Double = 4.33,

    @field:NotNull
    var costNameTemplate: String = "Wynagrodzenie - {employeeName}",

    @field:NotNull
    var costDescriptionTemplate: String = "Miesięczne wynagrodzenie dla {employeeName} ({position})",

    @field:NotNull
    var autoRenewSalaryCosts: Boolean = true,

    @field:NotNull
    var deactivateOnEmployeeDeactivation: Boolean = true,

    @field:NotNull
    var asyncProcessingEnabled: Boolean = true,

    @field:NotNull
    var retryFailedOperations: Boolean = true,

    @field:NotNull
    @field:Min(1)
    @field:Max(10)
    var maxRetryAttempts: Int = 3,

    @field:NotNull
    @field:Min(1)
    @field:Max(60)
    var retryDelaySeconds: Int = 5,

    @field:NotNull
    var enableDetailedLogging: Boolean = true,

    @field:NotNull
    var logCalculationDetails: Boolean = false,

    @field:NotNull
    var enableAuditTrail: Boolean = true,

    @field:NotNull
    var batchProcessingEnabled: Boolean = false,

    @field:NotNull
    @field:Min(10)
    @field:Max(1000)
    var batchSize: Int = 50,

    @field:NotNull
    @field:Min(10)
    @field:Max(300)
    var processingTimeoutSeconds: Int = 30
) {

    fun getTotalEmploymentRate(): BigDecimal {
        return employmentZusEmployerRate
            .add(employmentFgspRate)
            .add(employmentFpRate)
    }

    fun formatCostName(employeeName: String): String {
        return costNameTemplate.replace("{employeeName}", employeeName)
    }

    fun formatCostDescription(employeeName: String, position: String): String {
        return costDescriptionTemplate
            .replace("{employeeName}", employeeName)
            .replace("{position}", position)
    }

    fun calculateMonthlyHours(weeklyHours: Double = defaultWorkingHoursPerWeek): Double {
        return weeklyHours * weeksPerMonth
    }

    fun isValidConfiguration(): Boolean {
        return enableAutomaticCostCreation &&
                employmentZusEmployerRate >= BigDecimal.ZERO &&
                mandateZusEmployerRate >= BigDecimal.ZERO &&
                b2bZusEmployerRate >= BigDecimal.ZERO &&
                defaultWorkingHoursPerWeek > 0 &&
                weeksPerMonth > 0 &&
                costNameTemplate.contains("{employeeName}") &&
                costDescriptionTemplate.contains("{employeeName}")
    }
}