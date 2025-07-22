package com.carslab.crm.modules.email.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.cqrs.CommandBus
import com.carslab.crm.infrastructure.cqrs.QueryBus
import com.carslab.crm.modules.email.api.requests.SendProtocolEmailRequest
import com.carslab.crm.modules.email.api.responses.EmailSendResponse
import com.carslab.crm.modules.email.application.commands.models.SendProtocolEmailCommand
import com.carslab.crm.modules.email.application.queries.models.GetEmailHistoryQuery
import com.carslab.crm.modules.email.application.queries.models.EmailHistoryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/email")
@Tag(name = "Email Management", description = "Email sending endpoints")
class EmailController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus
) : BaseController() {

    @PostMapping("/send/protocol")
    @Operation(summary = "Send protocol email", description = "Sends protocol email to client using default template")
    fun sendProtocolEmail(@Valid @RequestBody request: SendProtocolEmailRequest): ResponseEntity<EmailSendResponse> {
        logger.info("Sending protocol email for protocol: ${request.visit_id}")

        try {
            val command = SendProtocolEmailCommand(
                protocolId = request.visit_id
            )

            val emailId = commandBus.execute(command)

            logger.info("Successfully sent protocol email: $emailId")
            return ok(EmailSendResponse(
                emailId = emailId,
                status = "SENT",
                message = "Protocol email sent successfully"
            ))
        } catch (e: Exception) {
            logger.error("Error sending protocol email for protocol: ${request.visit_id}", e)
            return ok(EmailSendResponse(
                emailId = null,
                status = "FAILED",
                message = "Failed to send email: ${e.message}"
            ))
        }
    }

    @GetMapping("/history")
    @Operation(summary = "Get email history", description = "Retrieves email sending history")
    fun getEmailHistory(
        @RequestParam(required = false) protocolId: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<com.carslab.crm.api.model.response.PaginatedResponse<EmailHistoryResponse>> {
        logger.debug("Getting email history for protocol: $protocolId")

        val query = GetEmailHistoryQuery(protocolId, page, size)
        val result = queryBus.execute(query)

        return ok(result)
    }
}
