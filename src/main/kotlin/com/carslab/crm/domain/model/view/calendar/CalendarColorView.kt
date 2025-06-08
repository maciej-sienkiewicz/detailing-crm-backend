package com.carslab.crm.domain.model.view.calendar

import com.carslab.crm.domain.model.Audit
import java.time.LocalDateTime
import java.util.UUID

/**
 * Value object for calendar color ID
 */
data class CalendarColorId(val value: String)

/**
 * Domain model for calendar color
 */
data class CalendarColorView(
    val id: CalendarColorId,
    val name: String,
    val color: String,
    val audit: Audit
) {
    companion object {
        fun create(id: CalendarColorId, name: String, color: String, audit: Audit): CalendarColorView {
            LocalDateTime.now()
            return CalendarColorView(
                id = id,
                name = name,
                color = color,
                audit = audit
            )
        }
    }
}