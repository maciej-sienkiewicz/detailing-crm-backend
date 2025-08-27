package com.carslab.crm.production.modules.companysettings.application.service

import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.modules.company_settings.infrastructure.storage.FileLogoStorageService
import com.carslab.crm.production.modules.companysettings.application.dto.CompanyResponse
import com.carslab.crm.production.modules.companysettings.application.dto.CreateCompanyRequest
import com.carslab.crm.production.modules.companysettings.domain.command.CreateCompanyCommand
import com.carslab.crm.production.modules.companysettings.domain.service.CompanyDomainService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanyInitializationService(
    private val companyDomainService: CompanyDomainService,
    private val logoStorageService: FileLogoStorageService,
) {
    private val logger = LoggerFactory.getLogger(CompanyInitializationService::class.java)
 
    fun initializeCompany(request: CreateCompanyRequest): CompanyResponse {
        logger.info("Initializing new company: ${request.companyName} ")

        val createCompanyCommand = CreateCompanyCommand(
            companyName = request.companyName,
            taxId = request.taxId,
            address = request.address,
            phone = request.phone,
            website = request.website,
            bankAccountNumber = request.bankAccountNumber,
            bankName = request.bankName,
            swiftCode = request.swiftCode,
            accountHolderName = request.accountHolderName
        )

        val company = companyDomainService.createCompany(createCompanyCommand)
        logger.info("Created company with ID: ${company.id.value}")

        return CompanyResponse.Companion.from(company, logoStorageService)
    }
}