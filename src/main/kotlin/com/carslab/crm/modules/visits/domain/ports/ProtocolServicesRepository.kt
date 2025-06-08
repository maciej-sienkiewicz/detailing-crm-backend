package com.carslab.crm.modules.visits.domain.ports

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.view.protocol.ProtocolServiceView

/**
 * Port repozytorium dla usług w protokołach.
 */
interface ProtocolServicesRepository {
    /**
     * Zapisuje usługi dla protokołu.
     */
    fun saveServices(services: List<CreateServiceModel>, protocolId: ProtocolId): List<String>

    /**
     * Znajduje usługi dla danego protokołu.
     */
    fun findByProtocolId(protocolId: ProtocolId): List<ProtocolServiceView>
}