// src/main/kotlin/com/carslab/crm/modules/company_settings/api/UserSignatureController.kt
package com.carslab.crm.modules.company_settings.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.modules.company_settings.api.requests.CreateSignatureRequest
import com.carslab.crm.modules.company_settings.api.requests.UpdateSignatureRequest
import com.carslab.crm.modules.company_settings.api.requests.SignatureValidationRequest
import com.carslab.crm.modules.company_settings.api.responses.UserSignatureResponse
import com.carslab.crm.modules.company_settings.api.responses.SignatureValidationResponse
import com.carslab.crm.modules.company_settings.domain.UserSignatureApplicationService
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.security.SecurityContext
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.IllegalStateException

@RestController
@RequestMapping("/api/company-settings/signature")
@Tag(name = "User Signature", description = "User signature management endpoints")
class UserSignatureController(
    private val userSignatureApplicationService: UserSignatureApplicationService,
    private val securityContext: SecurityContext
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get user signature", description = "Retrieves the current user's signature")
    fun getUserSignature(): ResponseEntity<UserSignatureResponse?> {
        val userId = securityContext.getCurrentUserId() ?: throw IllegalStateException("No user signatures found")
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Getting signature for user: $userId in company: $companyId")

        val signature = userSignatureApplicationService.getUserSignature(userId.toLong(), companyId)
        return if (signature != null) {
            ok(signature)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @Operation(summary = "Create signature", description = "Creates a new signature for the current user")
    fun createSignature(@Valid @RequestBody request: CreateSignatureRequest): ResponseEntity<UserSignatureResponse> {
        val userId = securityContext.getCurrentUserId()
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Creating signature for user: $userId in company: $companyId")

        try {
            val signature = userSignatureApplicationService.createSignature(userId!!.toLong(), companyId, request)
            logger.info("Successfully created signature for user: $userId")
            return created(signature)
        } catch (e: Exception) {
            return logAndRethrow("Error creating signature", e)
        }
    }

    @PutMapping
    @Operation(summary = "Update signature", description = "Updates the current user's existing signature")
    fun updateSignature(@Valid @RequestBody request: UpdateSignatureRequest): ResponseEntity<UserSignatureResponse> {
        val userId = securityContext.getCurrentUserId()
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Updating signature for user: $userId in company: $companyId")

        try {
            val signature = userSignatureApplicationService.updateSignature(userId!!.toLong(), companyId, request)
            logger.info("Successfully updated signature for user: $userId")
            return ok(signature)
        } catch (e: Exception) {
            return logAndRethrow("Error updating signature", e)
        }
    }

    @DeleteMapping
    @Operation(summary = "Delete signature", description = "Deletes the current user's signature")
    fun deleteSignature(): ResponseEntity<Map<String, Any>> {
        val userId = securityContext.getCurrentUserId()
        val companyId = securityContext.getCurrentCompanyId()

        logger.info("Deleting signature for user: $userId in company: $companyId")

        try {
            val deleted = userSignatureApplicationService.deleteSignature(userId!!.toLong(), companyId)
            return if (deleted) {
                logger.info("Successfully deleted signature for user: $userId")
                ok(createSuccessResponse("Signature successfully deleted", mapOf("userId" to userId)))
            } else {
                logger.warn("Signature for user: $userId not found for deletion")
                throw ResourceNotFoundException("Signature", userId.toString())
            }
        } catch (e: Exception) {
            return logAndRethrow("Error deleting signature", e)
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate signature", description = "Validates signature content before saving")
    fun validateSignature(@Valid @RequestBody request: SignatureValidationRequest): ResponseEntity<SignatureValidationResponse> {
        logger.debug("Validating signature content")

        val validation = userSignatureApplicationService.validateSignatureContent(request.content)
        return ok(validation)
    }
}