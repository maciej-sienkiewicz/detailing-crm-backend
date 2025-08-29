package com.carslab.crm.production.modules.companysettings.infrastructure.mapper

import com.carslab.crm.production.modules.companysettings.domain.model.BankSettings
import com.carslab.crm.production.modules.companysettings.domain.model.Company
import com.carslab.crm.production.modules.companysettings.domain.model.CompanyId
import com.carslab.crm.production.modules.companysettings.domain.model.GoogleDriveSettings
import com.carslab.crm.production.modules.companysettings.domain.model.MailConfiguration
import com.carslab.crm.production.modules.companysettings.infrastructure.entity.CompanyEntity

fun Company.toEntity(): CompanyEntity {
    return CompanyEntity(
        id = if (this.id.value == 0L) null else this.id.value,
        name = this.name,
        taxId = this.taxId,
        address = this.address,
        phone = this.phone,
        website = this.website,
        logoId = this.logoId,
        // Bank settings
        bankAccountNumber = this.bankSettings.bankAccountNumber,
        bankName = this.bankSettings.bankName,
        swiftCode = this.bankSettings.swiftCode,
        accountHolderName = this.bankSettings.accountHolderName,
        // Mail configuration
        smtpServer = this.mailConfiguration.smtpServer,
        smtpPort = this.mailConfiguration.smtpPort,
        email = this.mailConfiguration.email,
        emailPassword = this.mailConfiguration.emailPassword,
        useTls = this.mailConfiguration.useTls,
        useSsl = this.mailConfiguration.useSsl,
        fromName = this.mailConfiguration.fromName,
        mailEnabled = this.mailConfiguration.enabled,
        // Google Drive settings
        googleClientId = this.googleDriveSettings.clientId,
        googleClientSecret = this.googleDriveSettings.clientSecret,
        googleRefreshToken = this.googleDriveSettings.refreshToken,
        googleDefaultFolderId = this.googleDriveSettings.defaultFolderId,
        googleDefaultFolderName = this.googleDriveSettings.defaultFolderName,
        googleDriveEnabled = this.googleDriveSettings.enabled,
        googleAutoUploadInvoices = this.googleDriveSettings.autoUploadInvoices,
        googleAutoCreateFolders = this.googleDriveSettings.autoCreateFolders,
        // Meta fields
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version
    )
}
fun CompanyEntity.toDomain(): Company {
    return Company(
        id = CompanyId.Companion.of(this.id!!),
        name = this.name,
        taxId = this.taxId,
        address = this.address,
        phone = this.phone,
        website = this.website,
        logoId = this.logoId,
        bankSettings = BankSettings(
            bankAccountNumber = this.bankAccountNumber,
            bankName = this.bankName,
            swiftCode = this.swiftCode,
            accountHolderName = this.accountHolderName
        ),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        version = this.version,
        mailConfiguration = MailConfiguration(
            smtpServer = this.smtpServer,
            smtpPort = this.smtpPort,
            email = this.email,
            emailPassword = this.emailPassword,
            useTls = this.useTls,
            useSsl = this.useSsl,
            fromName = this.fromName,
            enabled = this.mailEnabled  // uwaga: pole w encji to mailEnabled
        ),
        googleDriveSettings = GoogleDriveSettings(
            clientId = this.googleClientId,
            clientSecret = this.googleClientSecret,
            refreshToken = this.googleRefreshToken,
            defaultFolderId = this.googleDefaultFolderId,
            defaultFolderName = this.googleDefaultFolderName,
            enabled = this.googleDriveEnabled,
            autoUploadInvoices = this.googleAutoUploadInvoices,
            autoCreateFolders = this.googleAutoCreateFolders
        )
    )
}