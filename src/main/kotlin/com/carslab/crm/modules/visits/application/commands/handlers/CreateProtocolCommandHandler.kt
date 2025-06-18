package com.carslab.crm.modules.visits.application.commands.handlers

import com.carslab.crm.modules.visits.application.commands.models.*
import com.carslab.crm.modules.visits.domain.ports.ProtocolRepository
import com.carslab.crm.modules.visits.domain.services.ProtocolDomainService
import com.carslab.crm.modules.visits.domain.exceptions.ProtocolNotFoundException
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import com.carslab.crm.domain.model.ProtocolId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService
) : CommandHandler<CreateProtocolCommand, String> {

    @Transactional
    override fun handle(command: CreateProtocolCommand): String {
        val protocolModel = protocolDomainService.createProtocol(command)
        val savedProtocolId = protocolRepository.save(protocolModel)
        return savedProtocolId.value
    }
}

@Service
class UpdateProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService
) : CommandHandler<UpdateProtocolCommand, Unit> {

    @Transactional
    override fun handle(command: UpdateProtocolCommand) {
        val existingProtocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ProtocolNotFoundException(command.protocolId)

        val updatedProtocol = protocolDomainService.updateProtocol(existingProtocol, command)
        protocolRepository.save(updatedProtocol)
    }
}

@Service
class ChangeProtocolStatusCommandHandler(
    private val protocolRepository: ProtocolRepository,
    private val protocolDomainService: ProtocolDomainService
) : CommandHandler<ChangeProtocolStatusCommand, Unit> {

    @Transactional
    override fun handle(command: ChangeProtocolStatusCommand) {
        val protocol = protocolRepository.findById(ProtocolId(command.protocolId))
            ?: throw ProtocolNotFoundException(command.protocolId)

        val updatedProtocol = protocolDomainService.changeStatus(protocol, command.newStatus, command.reason)
        protocolRepository.save(updatedProtocol)
    }
}

@Service
class DeleteProtocolCommandHandler(
    private val protocolRepository: ProtocolRepository
) : CommandHandler<DeleteProtocolCommand, Unit> {

    @Transactional
    override fun handle(command: DeleteProtocolCommand) {
        val protocolExists = protocolRepository.existsById(ProtocolId(command.protocolId))
        if (!protocolExists) {
            throw ProtocolNotFoundException(command.protocolId)
        }

        protocolRepository.deleteById(ProtocolId(command.protocolId))
    }
}