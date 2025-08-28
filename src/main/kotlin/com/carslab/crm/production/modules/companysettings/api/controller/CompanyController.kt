package com.carslab.crm.production.modules.companysettings.api.controller

import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.CompanyResponse
import com.carslab.crm.production.modules.companysettings.application.dto.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.application.dto.CreateCompanyRequest
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyInitializationService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyLogoService
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/company")
@Tag(name = "Company Settings", description = "Company settings management endpoints")
class CompanyController(
    private val companyInitializationService: CompanyInitializationService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val companyLogoService: CompanyLogoService,
    private val securityContext: SecurityContext,
    private val logoStorageService: LogoStorageService
) : BaseController() {

    @GetMapping
    @Operation(summary = "Get company settings", description = "Retrieves company settings for the current company")
    fun getCompanySettings(): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Getting company settings for company: $companyId")

        val response = companyDetailsFetchService.getCompanySettings(companyId)
        return ok(response)
    }

    @PostMapping
    @Operation(summary = "Initialize company", description = "Creates a new company")
    fun createCompany(@Valid @RequestBody request: CreateCompanyRequest): ResponseEntity<CompanyResponse> {
        logger.info("Initializing new company: ${request.companyName}")

        val response = companyInitializationService.initializeCompany(request)

        logger.info("Successfully initialized company with ID: ${response.id}")
        return created(response)
    }

    @PostMapping("/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload company logo", description = "Uploads a logo for the current company")
    fun uploadLogo(
        @Parameter(description = "Logo file", required = true)
        @RequestParam("logo") logoFile: MultipartFile
    ): ResponseEntity<CompanySettingsResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading logo for company: $companyId")

        try {
            val response = companyLogoService.uploadLogo(companyId, logoFile)

            logger.info("Successfully uploaded logo for company: $companyId")
            return ok(response)
        } catch (e: Exception) {
            return throw IllegalStateException("Error uploading logo", e)
        }
    }
    
    @GetMapping("/logo/{logoFileId}")
    @Operation(summary = "Get company logo", description = "Retrieves company logo by file ID")
    fun getLogo(
        @Parameter(description = "Logo file ID", required = true)
        @PathVariable logoFileId: String
    ): ResponseEntity<org.springframework.core.io.Resource> {
        logger.debug("Getting logo file: $logoFileId")
        
        val companyId = securityContext.getCurrentCompanyId()

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
}