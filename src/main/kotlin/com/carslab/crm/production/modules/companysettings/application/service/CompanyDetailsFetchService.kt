package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.CompanySettingsResponse
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanyDetailsFetchService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: LogoStorageService
) {

    private val logger = LoggerFactory.getLogger(CompanyDetailsFetchService::class.java)

    @Transactional(readOnly = true)
    fun getCompanySettings(companyId: Long): CompanySettingsResponse {
        logger.info("Fetching company settings for company ID: $companyId")
        val companyDetails = companyDomainService.getCompanyById(companyId)
        logger.info("Fetched company settings for company ID: ${companyDetails.id.value}")
        return CompanySettingsResponse.from(companyDetails, logoStorageService)
    }
}