package com.carslab.crm.domain

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CarReceptionFacade(
    private val carReceptionService: CarReceptionService
) {
    private val logger = LoggerFactory.getLogger(CarReceptionFacade::class.java)

    fun createProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        return carReceptionService.createProtocol(protocol)
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        return carReceptionService.updateProtocol(protocol)
    }

    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        return carReceptionService.changeStatus(protocolId, newStatus)
    }

    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        return carReceptionService.getProtocolById(protocolId)
    }

    fun getAllProtocols(): List<CarReceptionProtocol> {
        return carReceptionService.getAllProtocols()
    }

    fun searchProtocols(
        clientName: String? = null,
        licensePlate: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<CarReceptionProtocol> {
        return carReceptionService.searchProtocols(
            clientName = clientName,
            licensePlate = licensePlate,
            status = status,
            startDate = startDate,
            endDate = endDate
        )
    }

    fun deleteProtocol(protocolId: ProtocolId): Boolean {
        return carReceptionService.deleteProtocol(protocolId)
    }
}