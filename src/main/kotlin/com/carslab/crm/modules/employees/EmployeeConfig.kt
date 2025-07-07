// src/main/kotlin/com/carslab/crm/modules/employees/config/EmployeeConfig.kt
package com.carslab.crm.modules.employees.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Configuration
@ConfigurationProperties(prefix = "app.employees")
@Validated
data class EmployeeProperties(
    @field:NotNull
    @field:Min(10)
    var maxPageSize: Int = 100,

    @field:NotNull
    @field:Min(1)
    var defaultPageSize: Int = 20,

    @field:NotNull
    var enableExport: Boolean = true,

    @field:NotNull
    @field:Min(1)
    var exportMaxRecords: Int = 10000,

    @field:NotNull
    var enableDocumentUpload: Boolean = true,

    @field:NotNull
    @field:Min(1)
    var maxDocumentSizeMB: Int = 50,

    @field:NotNull
    var allowedDocumentTypes: List<String> = listOf("pdf", "doc", "docx", "jpg", "jpeg", "png")
)