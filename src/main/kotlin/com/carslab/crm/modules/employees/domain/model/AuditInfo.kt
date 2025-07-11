// src/main/kotlin/com/carslab/crm/modules/employees/domain/model/shared/AuditInfo.kt
package com.carslab.crm.modules.employees.domain.model.shared

import java.time.LocalDateTime

data class AuditInfo(
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val version: Long = 0
) {
    fun updated(by: String? = null): AuditInfo = copy(
        updatedAt = LocalDateTime.now(),
        updatedBy = by,
        version = version + 1
    )
}