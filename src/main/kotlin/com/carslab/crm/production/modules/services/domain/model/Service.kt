package com.carslab.crm.production.modules.services.domain.model

import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import java.time.LocalDateTime
import java.util.*

@JvmInline
value class ServiceId(val value: String) {
    companion object {
        fun generate(): ServiceId = ServiceId(UUID.randomUUID().toString())
        fun of(value: String): ServiceId = ServiceId(value)
    }
}

data class Service(
    val id: ServiceId,
    val companyId: Long,
    val name: String,
    val description: String?,
    val price: PriceValueObject,
    val vatRate: Int,
    val isActive: Boolean,
    val previousVersionId: ServiceId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long
) {
    fun canBeUsedBy(companyId: Long): Boolean {
        return this.companyId == companyId && this.isActive
    }

    fun update(
        name: String,
        description: String?,
        price: PriceValueObject,
        vatRate: Int
    ): Service {
        return copy(
            id = ServiceId.generate(),
            name = name,
            description = description,
            price = price,
            vatRate = vatRate,
            isActive = true,
            previousVersionId = this.id,
            updatedAt = LocalDateTime.now(),
            version = 0
        )
    }

    fun deactivate(): Service {
        return copy(
            isActive = false,
            updatedAt = LocalDateTime.now(),
            version = version + 1
        )
    }
}