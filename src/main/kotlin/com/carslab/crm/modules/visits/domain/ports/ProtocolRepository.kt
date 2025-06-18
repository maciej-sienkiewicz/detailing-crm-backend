// src/main/kotlin/com/carslab/crm/modules/visits/domain/ports/ProtocolRepository.kt
package com.carslab.crm.modules.visits.domain.ports

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel

interface ProtocolRepository {
    fun save(protocol: CreateProtocolRootModel): ProtocolId
    fun save(protocol: CarReceptionProtocol): CarReceptionProtocol
    fun findById(id: ProtocolId): CarReceptionProtocol?
    fun existsById(id: ProtocolId): Boolean
    fun deleteById(id: ProtocolId): Boolean
}