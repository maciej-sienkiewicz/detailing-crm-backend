package com.carslab.crm.production.modules.companysettings.api.controller

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.company_settings.api.responses.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.application.dto.CompanyResponse
import com.carslab.crm.production.modules.companysettings.application.dto.CreateCompanyRequest
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.companysettings.application.service.CompanyInitializationService
import com.carslab.crm.production.shared.presentation.BaseController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/company")
@Tag(name = "Company Settings", description = "Company settings management endpoints")
class CompanyController(
    private val companyInitializationService: CompanyInitializationService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val securityContext: SecurityContext,
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
}