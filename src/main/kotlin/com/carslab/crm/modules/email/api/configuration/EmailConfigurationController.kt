package com.carslab.crm.modules.email.api.configuration

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.modules.email.api.configuration.requests.SaveEmailConfigurationRequest
import com.carslab.crm.modules.email.api.configuration.responses.EmailConfigurationResponse
import com.carslab.crm.modules.email.api.configuration.responses.EmailSuggestionsResponse
import com.carslab.crm.modules.email.application.services.EmailConfigurationApplicationService
import com.carslab.crm.infrastructure.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/settings/email")
@Tag(name = "Email Configuration", description = "Email SMTP settings management")
class EmailConfigurationController(
    private val emailConfigurationService: EmailConfigurationApplicationService,
    private val securityContext: SecurityContext
) : BaseController() {

    @PostMapping("/save")
    @Operation(summary = "Save email configuration", description = "Saves and validates email configuration")
    fun saveEmailConfiguration(@Valid @RequestBody request: SaveEmailConfigurationRequest): ResponseEntity<EmailConfigurationResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Saving email configuration for company: $companyId")

        return try {
            val response = emailConfigurationService.saveConfiguration(companyId, request)
            ok(response)
        } catch (e: Exception) {
            logger.error("Error saving email configuration for company: $companyId", e)
            badRequest("Configuration save error")
        }
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get configuration suggestions", description = "Returns suggested SMTP configuration based on email domain")
    fun getConfigurationSuggestions(
        @Parameter(description = "Email address for domain detection", required = true)
        @RequestParam email: String
    ): ResponseEntity<EmailSuggestionsResponse> {
        logger.debug("Getting email configuration suggestions for: $email")

        val response = emailConfigurationService.getConfigurationSuggestions(email)
        return ok(response)
    }

    @GetMapping("/current")
    @Operation(summary = "Get current configuration", description = "Retrieves current email configuration")
    fun getCurrentConfiguration(): ResponseEntity<EmailConfigurationResponse?> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting current email configuration for company: $companyId")

        val response = emailConfigurationService.getCurrentConfiguration(companyId)
        return ok(response)
    }
}