package com.carslab.crm.domain

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CarReceptionFacade(
    private val carReceptionService: CarReceptionService
) {
    private val logger = LoggerFactory.getLogger(CarReceptionFacade::class.java)

    fun createProtocol(protocol: CreateProtocolRootModel): ProtocolId {
        return carReceptionService.createProtocol(protocol)
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        return carReceptionService.updateProtocol(protocol)
    }

    fun updateServices(protocolId: ProtocolId, services: List<CreateServiceModel>) {
        return carReceptionService.updateServices(protocolId, services)
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
        clientId: Long? = null,
        licensePlate: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): List<CarReceptionProtocol> {
        return carReceptionService.searchProtocols(
            clientName = clientName,
            clientId = clientId,
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