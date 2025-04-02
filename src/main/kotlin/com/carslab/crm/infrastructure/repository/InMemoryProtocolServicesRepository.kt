package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

data class ServiceEntity(
    val id: ServiceId,
    val protocolId: ProtocolId,
    val name: String,
    val basePrice: Money,
    val discount: Discount? = null,
    val finalPrice: Money,
    val approvalStatus: ApprovalStatus,
    val note: String
)

data class ServiceId(val id: Long)

@Repository
class InMemoryProtocolServicesRepository {

    private val services = ConcurrentHashMap<ProtocolId, List<ServiceEntity>>()

    fun saveServices(services: List<CreateServiceModel>, protocolId: ProtocolId): List<ServiceId> {
        val servicesToAdd = services.map { it.toEntity(protocolId) }
        this.services[protocolId] = servicesToAdd
        return servicesToAdd.map { it.id }
    }

    fun findByIds(protocolId: ProtocolId): List<ProtocolServiceView> {
        return services.filter { it.key == protocolId }
            .values
            .flatten()
            .map {
                ProtocolServiceView(
                    id = it.id,
                    name = it.name,
                    basePrice = it.basePrice,
                    discount = it.discount,
                    finalPrice = it.finalPrice,
                    approvalStatus = it.approvalStatus,
                    note = it.note
                )
            }
    }
}


private fun CreateServiceModel.toEntity(protocolId: ProtocolId): ServiceEntity {
    return ServiceEntity(
        id = ServiceId(System.currentTimeMillis()),
        protocolId = protocolId,
        name = this.name,
        basePrice = this.basePrice,
        discount = this.discount,
        finalPrice = this.finalPrice,
        approvalStatus = this.approvalStatus,
        note = this.note,
    )}
