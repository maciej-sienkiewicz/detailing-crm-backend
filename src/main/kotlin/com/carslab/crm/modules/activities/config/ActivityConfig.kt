package com.carslab.crm.modules.activities.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Configuration
@ConfigurationProperties(prefix = "app.activities")
@Validated
data class ActivityProperties(
    @field:NotNull
    @field:Min(1)
    var retentionDays: Int = 365,

    @field:NotNull
    @field:Min(10)
    var maxPageSize: Int = 100,

    @field:NotNull
    @field:Min(1)
    var defaultPageSize: Int = 20,

    @field:NotNull
    var enableAnalytics: Boolean = true,

    @field:NotNull
    var enableExport: Boolean = true,

    @field:NotNull
    @field:Min(1)
    var exportMaxRecords: Int = 10000,

    @field:NotNull
    var enableAutoCleanup: Boolean = true,

    @field:NotNull
    @field:Min(1)
    var cleanupIntervalHours: Int = 24
)