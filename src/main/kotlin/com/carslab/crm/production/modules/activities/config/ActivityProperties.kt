// src/main/kotlin/com/carslab/crm/production/modules/activities/config/ActivityConfig.kt
package com.carslab.crm.production.modules.activities.config

import org.springframework.boot.context.properties.ConfigurationProperties
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
    var defaultPageSize: Int = 50,

    @field:NotNull
    @field:Min(10)
    var maxPageSize: Int = 500,

    @field:NotNull
    var enableEventHandling: Boolean = true,

    @field:NotNull
    var enableAsyncProcessing: Boolean = true
)