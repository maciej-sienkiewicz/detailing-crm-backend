package com.carslab.crm.modules.email.domain.ports

import com.carslab.crm.modules.email.domain.model.ProtocolEmailData
import com.carslab.crm.production.modules.visits.domain.service.details.AuthContext

interface ProtocolDataProvider {
    fun getProtocolData(protocolId: String, authContext: AuthContext? = null): ProtocolEmailData?
}