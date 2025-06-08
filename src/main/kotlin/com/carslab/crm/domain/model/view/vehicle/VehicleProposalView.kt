package com.carslab.crm.domain.model.view.vehicle

import com.carslab.crm.modules.clients.domain.model.ClientId

class VehicleProposalView(
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val licensePlate: String? = null,
    val mileage: Long?,
    val ownerIds: Set<ClientId>
)