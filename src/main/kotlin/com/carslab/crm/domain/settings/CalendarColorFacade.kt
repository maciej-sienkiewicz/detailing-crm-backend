package com.carslab.crm.domain.settings

import com.carslab.crm.api.model.commands.CreateCalendarColorCommand
import com.carslab.crm.api.model.commands.UpdateCalendarColorCommand
import com.carslab.crm.api.model.response.CalendarColorResponse
import com.carslab.crm.domain.model.create.CalendarColorCreate
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.domain.model.view.calendar.CalendarColorView
import com.carslab.crm.domain.port.CalendarColorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CalendarColorService(
    private val calendarColorRepository: CalendarColorRepository
) {
    private val logger = LoggerFactory.getLogger(CalendarColorService::class.java)

    fun createCalendarColor(name: String, color: String, companyId: Long?): CalendarColorView {
        logger.info("Creating new calendar color with name: $name")
        val calendarColor = CalendarColorCreate.create(name, color)
        return calendarColorRepository.save(calendarColor, companyId)
    }

    fun getAllCalendarColors(): List<CalendarColorView> {
        logger.debug("Getting all calendar colors")
        return calendarColorRepository.findAll()
    }

    fun getCalendarColorById(id: CalendarColorId): CalendarColorView? {
        logger.debug("Getting calendar color by ID: ${id.value}")
        return calendarColorRepository.findById(id)
    }

    fun updateCalendarColor(id: CalendarColorId, name: String, color: String): CalendarColorView? {
        logger.info("Updating calendar color with ID: ${id.value}")

        val existingColor = calendarColorRepository.findById(id) ?: return null

        val updatedColor = existingColor.copy(
            name = name,
            color = color,
            audit = existingColor.audit.copy(
                updatedAt = LocalDateTime.now()
            )
        )

        return calendarColorRepository.update(updatedColor)
    }

    fun deleteCalendarColor(id: CalendarColorId): Boolean {
        logger.info("Deleting calendar color with ID: ${id.value}")
        return calendarColorRepository.deleteById(id)
    }

    fun isColorNameTaken(name: String, excludeId: String? = null): Boolean {
        logger.debug("Checking if color name is taken: $name, excludeId: $excludeId")
        return calendarColorRepository.isNameTaken(name, excludeId?.let { CalendarColorId(it) })
    }
}

@Service
class CalendarColorFacade(
    private val calendarColorService: CalendarColorService
) {
    private val logger = LoggerFactory.getLogger(CalendarColorFacade::class.java)

    fun createCalendarColor(command: CreateCalendarColorCommand, companyId: Long? = null): CalendarColorResponse {
        val createdColor = calendarColorService.createCalendarColor(command.name, command.color, companyId)
        return toResponse(createdColor)
    }

    fun updateCalendarColor(command: UpdateCalendarColorCommand): CalendarColorResponse {
        val id = command.id ?: throw IllegalArgumentException("Calendar color ID is required")
        val updatedColor = calendarColorService.updateCalendarColor(
            CalendarColorId(id),
            command.name,
            command.color
        ) ?: throw IllegalArgumentException("Calendar color not found: $id")

        return toResponse(updatedColor)
    }

    fun getAllCalendarColors(): List<CalendarColorResponse> {
        return calendarColorService.getAllCalendarColors().map { toResponse(it) }
    }

    fun getCalendarColorById(id: CalendarColorId): CalendarColorResponse? {
        return calendarColorService.getCalendarColorById(id)?.let { toResponse(it) }
    }

    fun deleteCalendarColor(id: CalendarColorId): Boolean {
        return calendarColorService.deleteCalendarColor(id)
    }

    fun isColorNameTaken(name: String, excludeId: String? = null): Boolean {
        return calendarColorService.isColorNameTaken(name, excludeId)
    }

    private fun toResponse(color: CalendarColorView): CalendarColorResponse {
        return CalendarColorResponse(
            id = color.id.value,
            name = color.name,
            color = color.color,
            createdAt = color.audit.createdAt,
            updatedAt = color.audit.updatedAt
        )
    }
}