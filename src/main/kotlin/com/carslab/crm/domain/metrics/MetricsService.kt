package com.carslab.crm.domain.metrics

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ClientId
import com.carslab.crm.domain.port.CarReceptionRepository
import org.springframework.stereotype.Service

@Service
class MetricsService(
) {
    fun updateClientOnProtocolClosed(clientId: ClientId, protocol: CarReceptionProtocol) {
        // Zwieksz liczbe wizyt
        // Zwiesz liczbe GMV dla konta
        // Zaktualizuj ostatnia wizyte
    }

    fun updateClientOnVehicleAttached() {
        //
    }
}