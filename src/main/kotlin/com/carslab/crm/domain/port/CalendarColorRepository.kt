package com.carslab.crm.domain.port

import com.carslab.crm.domain.model.create.CalendarColorCreate
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.domain.model.view.calendar.CalendarColorView


/**
 * Repository interface for calendar colors
 */
interface CalendarColorRepository {
    /**
     * Save a calendar color
     */
    fun save(calendarColor: CalendarColorCreate): CalendarColorView

    /**
     * Find a calendar color by ID
     */
    fun findById(id: CalendarColorId): CalendarColorView?

    /**
     * Find all calendar colors
     */
    fun findAll(): List<CalendarColorView>

    /**
     * Delete a calendar color by ID
     */
    fun deleteById(id: CalendarColorId): Boolean

    /**
     * Check if a color name is already taken
     */
    fun isNameTaken(name: String, excludeId: CalendarColorId? = null): Boolean

    fun update(calendarColor: CalendarColorView): CalendarColorView
}