package com.carslab.crm.production.modules.companysettings.domain.service

import com.carslab.crm.production.modules.companysettings.domain.command.CreateCompanyCommand
import com.carslab.crm.production.modules.companysettings.domain.model.BankSettings
import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.companysettings.domain.repository.CompanyRepository
import com.carslab.crm.production.shared.exception.BusinessException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CompanyDomainService(
    private val repository: CompanyRepository
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
}