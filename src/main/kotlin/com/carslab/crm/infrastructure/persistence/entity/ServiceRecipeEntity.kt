package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.domain.model.view.ServiceRecipeView
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "service_recipes")
class ServiceRecipeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, precision = 10)
    var price: BigDecimal,

    @Column(name = "vat_rate", nullable = false)
    var vatRate: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ServiceRecipeView {
        return ServiceRecipeView(
            id = ServiceRecipeId(id.toString()),
            name = name,
            description = description,
            price = price,
            vatRate = vatRate,
            audit = Audit(
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        )
    }

    companion object {
        fun fromDomain(domain: ServiceRecipeView): ServiceRecipeEntity {
            return ServiceRecipeEntity(
                name = domain.name,
                description = domain.description,
                price = domain.price,
                vatRate = domain.vatRate,
                createdAt = domain.audit.createdAt,
                updatedAt = domain.audit.updatedAt
            )
        }
    }
}