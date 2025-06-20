package com.carslab.crm.modules.email.domain.ports

import com.carslab.crm.modules.email.domain.model.ProtocolEmailData

interface ProtocolDataProvider {
    fun getProtocolData(protocolId: String): ProtocolEmailData?
}