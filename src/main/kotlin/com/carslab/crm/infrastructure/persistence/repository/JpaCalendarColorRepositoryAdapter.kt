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
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = CalendarColorEntity(
            name = calendarColor.name,
            color = calendarColor.color,
            companyId = companyId,
            createdAt = calendarColor.audit.createdAt,
            updatedAt = calendarColor.audit.updatedAt
        )

        val savedEntity = calendarColorJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun update(calendarColor: CalendarColorView): CalendarColorView {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        // Pierwszy sprawdź czy obiekt istnieje i należy do firmy użytkownika
        val existingEntity = calendarColorJpaRepository.findByCompanyIdAndId(companyId, calendarColor.id.value.toLong())
            .orElseThrow { IllegalStateException("Calendar color not found or access denied: ${calendarColor.id.value}") }

        existingEntity.name = calendarColor.name
        existingEntity.color = calendarColor.color
        existingEntity.updatedAt = calendarColor.audit.updatedAt

        val savedEntity = calendarColorJpaRepository.save(existingEntity)
        return savedEntity.toDomain()
    }

    override fun findById(id: CalendarColorId): CalendarColorView? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return calendarColorJpaRepository.findByCompanyIdAndId(companyId, id.value.toLong())
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(): List<CalendarColorView> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return calendarColorJpaRepository.findByCompanyId(companyId)
            .map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: CalendarColorId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = calendarColorJpaRepository.findByCompanyIdAndId(companyId, id.value.toLong())
            .orElse(null) ?: return false

        calendarColorJpaRepository.delete(entity)
        return true
    }

    override fun isNameTaken(name: String, excludeId: CalendarColorId?): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return if (excludeId != null) {
            calendarColorJpaRepository.existsByNameIgnoreCaseAndIdNotAndCompanyId(name, excludeId.value.toLong(), companyId)
        } else {
            calendarColorJpaRepository.existsByNameIgnoreCaseAndCompanyId(name, companyId)
        }
    }
}