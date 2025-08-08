// src/main/kotlin/com/carslab/crm/modules/company_settings/domain/UserSignatureApplicationService.kt
package com.carslab.crm.modules.company_settings.domain

import com.carslab.crm.modules.company_settings.api.requests.CreateSignatureRequest
import com.carslab.crm.modules.company_settings.api.requests.UpdateSignatureRequest
import com.carslab.crm.modules.company_settings.api.responses.UserSignatureResponse
import com.carslab.crm.modules.company_settings.api.responses.SignatureValidationResponse
import com.carslab.crm.modules.company_settings.domain.model.CreateUserSignature
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSignatureApplicationService(
    private val userSignatureDomainService: UserSignatureDomainService
) {
    private val logger = LoggerFactory.getLogger(UserSignatureApplicationService::class.java)

    fun createSignature(userId: Long, companyId: Long, request: CreateSignatureRequest): UserSignatureResponse {
        logger.info("Creating signature for user: $userId in company: $companyId")

        try {
            validateSignatureRequest(request.content)

            val createDomain = CreateUserSignature(
                userId = userId,
                companyId = companyId,
                content = request.content
            )

            val signature = userSignatureDomainService.createSignature(createDomain)

            logger.info("Successfully created signature with ID: ${signature.id.value}")
            return UserSignatureResponse.from(signature)
        } catch (e: DomainException) {
            logger.error("Failed to create signature: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating signature", e)
            throw RuntimeException("Failed to create signature", e)
        }
    }

    fun updateSignature(userId: Long, companyId: Long, request: UpdateSignatureRequest): UserSignatureResponse {
        logger.info("Updating signature for user: $userId in company: $companyId")

        try {
            validateSignatureRequest(request.content)

            val signature = userSignatureDomainService.updateSignature(userId, companyId, request.content)

            logger.info("Successfully updated signature for user: $userId")
            return UserSignatureResponse.from(signature)
        } catch (e: DomainException) {
            logger.error("Failed to update signature for user $userId: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating signature for user $userId", e)
            throw RuntimeException("Failed to update signature", e)
        }
    }

    @Transactional(readOnly = true)
    fun getUserSignature(userId: Long, companyId: Long): UserSignatureResponse? {
        logger.debug("Getting signature for user: $userId in company: $companyId")

        val signature = userSignatureDomainService.getUserSignature(userId, companyId)
        return signature?.let { UserSignatureResponse.from(it) }
    }

    fun deleteSignature(userId: Long, companyId: Long): Boolean {
        logger.info("Deleting signature for user: $userId in company: $companyId")

        val deleted = userSignatureDomainService.deleteSignature(userId, companyId)

        if (deleted) {
            logger.info("Successfully deleted signature for user: $userId")
        } else {
            logger.warn("Signature for user: $userId not found for deletion")
        }

        return deleted
    }

    fun validateSignatureContent(content: String): SignatureValidationResponse {
        logger.debug("Validating signature content")

        return try {
            validateSignatureRequest(content)
            SignatureValidationResponse(isValid = true, errors = emptyList())
        } catch (e: Exception) {
            SignatureValidationResponse(
                isValid = false,
                errors = listOf(e.message ?: "Invalid signature content")
            )
        }
    }

    private fun validateSignatureRequest(content: String) {
        require(content.isNotBlank()) { "Signature content cannot be blank" }
        require(content.startsWith("data:image/")) { "Signature must be a valid data URL" }
        require(content.contains("base64,")) { "Signature must contain base64 data" }
        require(content.length <= 1000000) { "Signature content exceeds maximum size of 1MB" }

        // Validate base64 content
        try {
            val base64Data = content.split(",")[1]
            require(base64Data.length >= 100) { "Signature data is too small" }
            // Test base64 decoding
            java.util.Base64.getDecoder().decode(base64Data)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid base64 signature data")
        }
    }
}