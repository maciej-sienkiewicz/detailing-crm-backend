package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.create.protocol.CreateServiceRecipeModel
import com.carslab.crm.domain.model.create.protocol.ServiceRecipeId
import com.carslab.crm.domain.model.view.ServiceRecipeView
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

data class ServiceRecipeEntity(
    val id: Long,
    val name: String,
    val description: String? = null,
    val price: BigDecimal,
    val vatRate: Int,
    val audit: Audit
)

@Repository
class InMemoryServiceRecipeRepository {

    private val entities = ConcurrentHashMap<Long, ServiceRecipeEntity>()

    fun save(service: CreateServiceRecipeModel): ServiceRecipeId {
        val randomId = System.currentTimeMillis()
        val serviceRecipeEntity = ServiceRecipeEntity(
            id = randomId,
            name = service.name,
            description = service.description,
            price = service.price,
            vatRate = service.vatRate,
            audit = Audit(
                createdAt = service.audit.createdAt,
                updatedAt = service.audit.updatedAt
            )
        )
        entities[randomId] = serviceRecipeEntity
        return ServiceRecipeId(randomId.toString())
    }

    fun getById(id: Long): ServiceRecipeView? =
        entities[id]?.let {
            ServiceRecipeView(
                id = ServiceRecipeId(it.id.toString()),
                name = it.name,
                description = it.description,
                price = it.price,
                vatRate = it.vatRate,
                audit = it.audit
            )
        }

    fun getAllServices(): List<ServiceRecipeView> {
        return entities.values.map {
            ServiceRecipeView(
                id = ServiceRecipeId(it.id.toString()),
                name = it.name,
                description = it.description,
                price = it.price,
                vatRate = it.vatRate,
                audit = it.audit
            )
        }
    }
}