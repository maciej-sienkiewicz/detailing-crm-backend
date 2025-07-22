package com.carslab.crm.modules.email.application.commands.handlers

import com.carslab.crm.modules.email.application.commands.models.SendProtocolEmailCommand
import com.carslab.crm.modules.email.domain.services.EmailSendingService
import com.carslab.crm.infrastructure.cqrs.CommandHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SendProtocolEmailCommandHandler(
    private val emailSendingService: EmailSendingService
) : CommandHandler<SendProtocolEmailCommand, String> {

    private val logger = LoggerFactory.getLogger(SendProtocolEmailCommandHandler::class.java)

    @Transactional
    override fun handle(command: SendProtocolEmailCommand): String {
        logger.info("Processing send protocol email command for protocol: ${command.protocolId}")

        try {
            val emailId = emailSendingService.sendProtocolEmail(
                protocolId = command.protocolId,
            )

            logger.info("Successfully processed email sending for protocol: ${command.protocolId}, emailId: $emailId")
            return emailId
        } catch (e: Exception) {
            logger.error("Failed to send protocol email for protocol: ${command.protocolId}", e)
            throw e
        }
    }
}
