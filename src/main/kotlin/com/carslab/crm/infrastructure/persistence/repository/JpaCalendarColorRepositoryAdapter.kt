package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.domain.model.create.CalendarColorCreate
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.domain.model.view.calendar.CalendarColorView
import com.carslab.crm.domain.port.CalendarColorRepository
import com.carslab.crm.infrastructure.persistence.entity.CalendarColorEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaCalendarColorRepositoryAdapter(
    private val calendarColorJpaRepository: CalendarColorJpaRepository
) : CalendarColorRepository {

    override fun save(calendarColor: CalendarColorCreate): CalendarColorView {
        val entity = CalendarColorEntity.fromDomain(calendarColor)

        val savedEntity = calendarColorJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun update(calendarColor: CalendarColorView): CalendarColorView {
        val entity = if (calendarColorJpaRepository.existsById(calendarColor.id.value)) {
            val existingEntity = calendarColorJpaRepository.findById(calendarColor.id.value).get()
            existingEntity.name = calendarColor.name
            existingEntity.color = calendarColor.color
            existingEntity.updatedAt = calendarColor.audit.updatedAt
            existingEntity
        } else {
            throw IllegalStateException("Nie istnieje kolor kalendarza o id: ${calendarColor.id.value}")
        }

        val savedEntity = calendarColorJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun findById(id: CalendarColorId): CalendarColorView? {
        return calendarColorJpaRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(): List<CalendarColorView> {
        return calendarColorJpaRepository.findAll().map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: CalendarColorId): Boolean {
        return if (calendarColorJpaRepository.existsById(id.value)) {
            calendarColorJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun isNameTaken(name: String, excludeId: CalendarColorId?): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return if (excludeId != null) {
            calendarColorJpaRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId.value, companyId)
        } else {
            calendarColorJpaRepository.existsByNameIgnoreCaseAndCompanyId(name, companyId)
        }
    }
}