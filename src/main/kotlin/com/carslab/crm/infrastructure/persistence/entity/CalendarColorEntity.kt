package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.create.CalendarColorCreate
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import com.carslab.crm.domain.model.view.calendar.CalendarColorView
import jakarta.persistence.*
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@Entity
@Table(name = "calendar_colors")
class CalendarColorEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    @Column(nullable = false)
    var color: String,

    @Column(name = "company_id", nullable = false)
    var companyId: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): CalendarColorView {
        return CalendarColorView(
            id = CalendarColorId(id!!.toString()),
            name = name,
            color = color,
            audit = Audit(
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        )
    }

    companion object {
        fun fromDomain(domain: CalendarColorCreate): CalendarColorEntity {
            return CalendarColorEntity(
                name = domain.name,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                color = domain.color,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}