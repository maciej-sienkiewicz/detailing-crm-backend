package com.carslab.crm.production.modules.clients.application.service

import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.clients.domain.service.ClientVisitRecorder
import com.carslab.crm.production.modules.clients.domain.service.ClientVehicleCounter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ClientStatisticsCommandService(
    private val clientVisitRecorder: ClientVisitRecorder,
    private val clientVehicleCounter: ClientVehicleCounter
) {
    fun recordVisit(clientId: String) {
        clientVisitRecorder.recordVisit(ClientId(clientId.toLong()))
    }

    fun incrementVehicleCount(clientId: String) {
        clientVehicleCounter.incrementVehicleCount(ClientId(clientId.toLong()))
    }
}