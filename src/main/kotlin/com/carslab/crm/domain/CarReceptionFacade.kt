package com.carslab.crm.domain

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CarReceptionFacade(
    private val carReceptionService: CarReceptionService
) {
    private val logger = LoggerFactory.getLogger(CarReceptionFacade::class.java)

    @Transactional
    fun createProtocol(protocol: CreateProtocolRootModel): ProtocolId {
        return carReceptionService.createProtocol(protocol)
    }

    @Transactional
    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        return carReceptionService.updateProtocol(protocol)
    }

    @Transactional
    fun updateServices(protocolId: ProtocolId, services: List<CreateServiceModel>) {
        carReceptionService.updateServices(protocolId, services)
    }

    @Transactional
    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        return carReceptionService.changeStatus(protocolId, newStatus)
    }

    @Transactional(readOnly = true)
    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        return carReceptionService.getProtocolById(protocolId)
    }

    @Transactional(readOnly = true)
    fun getAllProtocols(): List<CarReceptionProtocol> {
        return carReceptionService.getAllProtocols()
    }

    @Transactional(readOnly = true)
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

    @Transactional
    fun deleteProtocol(protocolId: ProtocolId): Boolean {
        return carReceptionService.deleteProtocol(protocolId)
    }
}