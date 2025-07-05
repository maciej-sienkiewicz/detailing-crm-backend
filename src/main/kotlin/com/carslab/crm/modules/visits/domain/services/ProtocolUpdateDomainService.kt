package com.carslab.crm.modules.visits.domain.services

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.modules.clients.domain.ClientApplicationService
import com.carslab.crm.modules.clients.domain.VehicleApplicationService
import com.carslab.crm.modules.clients.domain.model.ClientId
import com.carslab.crm.modules.clients.domain.model.VehicleId
import com.carslab.crm.modules.visits.application.commands.models.UpdateProtocolCommand
import com.carslab.crm.modules.visits.domain.exceptions.ProtocolNotFoundException
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.visits.domain.valueobjects.ProtocolUpdateResult
import com.carslab.crm.modules.visits.domain.valueobjects.ServicesUpdateResult
import com.carslab.crm.modules.visits.domain.valueobjects.StatusChangeResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ProtocolUpdateDomainService(
    private val protocolRepository: ProtocolRepository,
    private val protocolServicesService: ProtocolServicesService,
    private val clientStatisticsService: ClientStatisticsService,
    private val vehicleStatisticsService: VehicleStatisticsService,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleApplicationService: VehicleApplicationService,
    private val protocolDomainService: ProtocolDomainService
) {
    private val logger = LoggerFactory.getLogger(ProtocolUpdateDomainService::class.java)

    @Transactional
    fun updateProtocol(command: UpdateProtocolCommand): ProtocolUpdateResult {
        val protocolId = ProtocolId(command.protocolId)

        // 1. Walidacja i pobranie istniejącego protokołu
        val existingProtocol = protocolRepository.findById(protocolId)
            ?: throw ProtocolNotFoundException(protocolId)

        // 2. Aktualizacja podstawowych danych protokołu
        val updatedProtocol = protocolDomainService.updateProtocol(existingProtocol, command)
        protocolRepository.save(updatedProtocol)

        val servicesUpdateResult = if (command.services.isNotEmpty()) {
            protocolServicesService.updateProtocolServices(protocolId, command.services)
        } else {
            ServicesUpdateResult.noChanges()
        }

        // 4. Obsługa zmian statusu
        val statusChangeResult = handleStatusChange(
            existingProtocol.status,
            updatedProtocol.status,
            updatedProtocol.client.id!!,
            updatedProtocol.vehicle.id!!,
        )

        return ProtocolUpdateResult(
            protocolId = protocolId,
            oldStatus = existingProtocol.status,
            newStatus = updatedProtocol.status,
            servicesUpdateResult = servicesUpdateResult,
            statusChangeResult = statusChangeResult,
            updatedProtocol = updatedProtocol
        )
    }

    private fun handleStatusChange(
        oldStatus: ProtocolStatus,
        newStatus: ProtocolStatus,
        clientId: Long,
        vehicleId: VehicleId,
    ): StatusChangeResult {
        if (oldStatus == newStatus) {
            return StatusChangeResult.noChange()
        }

        // Specjalna obsługa statusu IN_PROGRESS
        if (newStatus == ProtocolStatus.IN_PROGRESS && oldStatus != ProtocolStatus.IN_PROGRESS) {
            clientStatisticsService.updateLastVisitDate(ClientId.of(clientId))
            clientApplicationService.updateClientStatistics(
                clientId = ClientId.of(clientId),
                counter = 1L
            )
            vehicleStatisticsService.updateLastVisitDate(vehicleId)
            vehicleApplicationService.updateVehicleStatistics(
                id = vehicleId.value,
                counter = 1L
            )
            return StatusChangeResult.visitStarted(clientId)
        }

        return StatusChangeResult.statusChanged(oldStatus, newStatus)
    }
}