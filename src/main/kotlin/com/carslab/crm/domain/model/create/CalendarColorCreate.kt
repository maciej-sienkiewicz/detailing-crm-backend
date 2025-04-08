package com.carslab.crm.domain.model.create

import com.carslab.crm.domain.model.Audit
import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model for calendar color
 */
data class CalendarColorCreate(
    val name: String,
    val color: String,
    val audit: Audit
) {
    companion object {
        fun create(name: String, color: String): CalendarColorCreate {
            val now = LocalDateTime.now()
            return CalendarColorCreate(
                name = name,
                color = color,
                audit = Audit(
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}