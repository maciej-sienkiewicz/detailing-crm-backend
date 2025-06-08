package com.carslab.crm.company_settings.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.company_settings.api.requests.CreateCompanySettingsRequest
import com.carslab.crm.company_settings.api.requests.UpdateCompanySettingsRequest
import com.carslab.crm.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.company_settings.domain.CompanySettingsApplicationService
import com.carslab.crm.company_settings.infrastructure.storage.FileLogoStorageService
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.infrastructure.util.ValidationUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/company-settings")
@Tag(name = "Company Settings", description = "Company settings management endpoints")
class CompanySettingsController(
    private val companySettingsApplicationService: CompanySettingsApplicationService,
    private val securityContext: SecurityContext,
    private val logoStorageService: FileLogoStorageService
) : BaseController() {

    @PostMapping
    @Operation(summary = "Create company settings", description = "Creates company settings for the current company")
    @PreAuthorize("hasRole('ADMIN')")
    fun createCompanySettings(@Valid @RequestBody request: CreateCompanySettingsRequest): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Creating company settings for company: $companyId")

        try {
            validateCreateRequest(request)

            val requestWithCompanyId = request.copy(companyId = companyId)
            val response = companySettingsApplicationService.createCompanySettings(requestWithCompanyId)

            logger.info("Successfully created company settings for company: $companyId")
            return created(response)
        } catch (e: Exception) {
            return logAndRethrow("Error creating company settings", e)
        }
    }

    @GetMapping
    @Operation(summary = "Get company settings", description = "Retrieves company settings for the current company")
    fun getCompanySettings(): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting company settings for company: $companyId")

        val response = companySettingsApplicationService.getCompanySettings(companyId)
        return ok(response)
    }

    @PutMapping
    @Operation(summary = "Update company settings", description = "Updates company settings for the current company")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCompanySettings(@Valid @RequestBody request: UpdateCompanySettingsRequest): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Updating company settings for company: $companyId")

        try {
            validateUpdateRequest(request)

            val response = companySettingsApplicationService.updateCompanySettings(companyId, request)

            logger.info("Successfully updated company settings for company: $companyId")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error updating company settings", e)
        }
    }

    @PostMapping("/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload company logo", description = "Uploads a logo for the current company")
    @PreAuthorize("hasRole('ADMIN')")
    fun uploadLogo(
        @Parameter(description = "Logo file", required = true)
        @RequestParam("logo") logoFile: MultipartFile
    ): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading logo for company: $companyId")

        try {
            val response = companySettingsApplicationService.uploadLogo(companyId, logoFile)

            logger.info("Successfully uploaded logo for company: $companyId")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error uploading logo", e)
        }
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Delete company logo", description = "Deletes the logo for the current company")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteLogo(): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting logo for company: $companyId")

        try {
            val response = companySettingsApplicationService.deleteLogo(companyId)

            logger.info("Successfully deleted logo for company: $companyId")
            return ok(response)
        } catch (e: Exception) {
            return logAndRethrow("Error deleting logo", e)
        }
    }

    @DeleteMapping
    @Operation(summary = "Delete company settings", description = "Deletes all company settings for the current company")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteCompanySettings(): ResponseEntity<Map<String, Any>> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting company settings for company: $companyId")

        val deleted = companySettingsApplicationService.deleteCompanySettings(companyId)

        return if (deleted) {
            logger.info("Successfully deleted company settings for company: $companyId")
            ok(createSuccessResponse("Company settings successfully deleted", mapOf("companyId" to companyId)))
        } else {
            logger.warn("Company settings for company: $companyId not found for deletion")
            throw ResourceNotFoundException("Company settings", companyId)
        }
    }

    @GetMapping("/logo/{logoFileId}")
    @Operation(summary = "Get company logo", description = "Retrieves company logo by file ID")
    fun getLogo(
        @Parameter(description = "Logo file ID", required = true)
        @PathVariable logoFileId: String
    ): ResponseEntity<org.springframework.core.io.Resource> {
        logger.debug("Getting logo file: $logoFileId")

        return try {
            val logoPath = logoStorageService.getLogoPath(logoFileId)
                ?: throw ResourceNotFoundException("Logo", logoFileId)

            val resource = org.springframework.core.io.UrlResource(logoPath.toUri())

            if (resource.exists() && resource.isReadable) {
                val contentType = java.nio.file.Files.probeContentType(logoPath) ?: "application/octet-stream"

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource)
            } else {
                throw ResourceNotFoundException("Logo", logoFileId)
            }
        } catch (e: Exception) {
            logger.error("Error retrieving logo file: $logoFileId", e)
            throw ResourceNotFoundException("Logo", logoFileId)
        }
    }

    @GetMapping("/validation/nip/{nip}")
    @Operation(summary = "Validate Polish NIP", description = "Validates Polish tax identification number")
    fun validateNIP(
        @Parameter(description = "Polish NIP to validate", required = true)
        @PathVariable nip: String
    ): ResponseEntity<Map<String, Any>> {
        logger.debug("Validating NIP: $nip")

        val isValid = isValidPolishNIP(nip)

        return ok(mapOf(
            "nip" to nip,
            "valid" to isValid,
            "message" to if (isValid) "Valid Polish NIP" else "Invalid Polish NIP format or checksum"
        ))
    }

    private fun isValidPolishNIP(nip: String): Boolean {
        val nipDigits = nip.replace("-", "").replace(" ", "")
        if (nipDigits.length != 10 || !nipDigits.all { it.isDigit() }) {
            return false
        }

        // NIP checksum validation
        val weights = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        val digits = nipDigits.map { it.toString().toInt() }

        val sum = weights.zip(digits.take(9)).sumOf { it.first * it.second }
        val checkDigit = sum % 11

        return checkDigit == digits[9]
    }

    private fun validateCreateRequest(request: CreateCompanySettingsRequest) {
        ValidationUtils.validateNotBlank(request.companyName, "Company name")
        ValidationUtils.validateNotBlank(request.taxId, "Tax ID")

        request.senderEmail?.let { email ->
            ValidationUtils.validateEmail(email)
        }

        request.phone?.let { phone ->
            ValidationUtils.validatePhone(phone)
        }
    }

    private fun validateUpdateRequest(request: UpdateCompanySettingsRequest) {
        ValidationUtils.validateNotBlank(request.basicInfo.companyName, "Company name")
        ValidationUtils.validateNotBlank(request.basicInfo.taxId, "Tax ID")

        request.emailSettings.senderEmail?.let { email ->
            ValidationUtils.validateEmail(email)
        }

        request.basicInfo.phone?.let { phone ->
            ValidationUtils.validatePhone(phone)
        }
    }
}