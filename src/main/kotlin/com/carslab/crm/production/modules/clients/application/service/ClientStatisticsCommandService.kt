package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.service.ClientRevenueUpdater
import com.carslab.crm.production.modules.clients.domain.service.ClientVisitRecorder
import com.carslab.crm.production.modules.clients.domain.service.ClientVehicleCounter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class ClientStatisticsCommandService(
    private val clientVisitRecorder: ClientVisitRecorder,
    private val clientVehicleCounter: ClientVehicleCounter,
    private val clientRevenueUpdater: ClientRevenueUpdater,
) {
    fun recordVisit(clientId: String) {
        clientVisitRecorder.recordVisit(ClientId(clientId.toLong()))
    }

    fun incrementVehicleCount(clientId: String) {
        clientVehicleCounter.incrementVehicleCount(ClientId(clientId.toLong()))
    }
    
    fun updateTotalRevenue(clientId: ClientId, amount: BigDecimal) {
        clientRevenueUpdater.addRevenue(clientId, amount)
    }

    fun decrementVehicleCount(clientId: ClientId) {
        clientVehicleCounter.decrementVehicleCount(clientId)
    }
}