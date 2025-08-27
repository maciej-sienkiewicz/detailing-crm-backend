package com.carslab.crm.production.modules.companysettings.domain.service

import com.carslab.crm.modules.company_settings.domain.LogoStorageService
import com.carslab.crm.production.modules.companysettings.domain.command.CreateCompanyCommand
import com.carslab.crm.production.modules.companysettings.domain.command.UploadLogoCommand
import com.carslab.crm.production.modules.companysettings.domain.model.BankSettings
import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.companysettings.domain.repository.CompanyRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.CompanyNotFoundException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CompanyDomainService(
    private val repository: CompanyRepository,
    private val logoStorageService: LogoStorageService
) {

    fun createCompany(command: CreateCompanyCommand): Company {
        if (repository.existsByTaxId(command.taxId)) {
            throw BusinessException("Company with tax ID ${command.taxId} already exists")
        }

        val company = Company(
            id = CompanyId.Companion.of(0),
            name = command.companyName,
            taxId = command.taxId,
            address = command.address,
            phone = command.phone,
            website = command.website,
            logoId = null,
            bankSettings = BankSettings(
                bankAccountNumber = command.bankAccountNumber,
                bankName = command.bankName,
                swiftCode = command.swiftCode,
                accountHolderName = command.accountHolderName
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 0
        )

        return repository.save(company)
    }

    fun getCompanyById(companyId: Long): Company {
        return repository.findById(companyId)
            ?: throw CompanyNotFoundException("Company with ID $companyId not found")
    }

    fun uploadLogo(command: UploadLogoCommand): Company {
        val company = getCompanyById(command.companyId)

        company.logoId?.let { oldLogoId ->
            logoStorageService.deleteLogo(oldLogoId)
        }

        val logoMetadata = logoStorageService.storeLogo(command.companyId, command.logoFile)

        // Update company with new logo ID
        val updatedCompany = company.copy(
            logoId = logoMetadata.fileId,
            updatedAt = LocalDateTime.now()
        )

        return repository.save(updatedCompany)
    }
}