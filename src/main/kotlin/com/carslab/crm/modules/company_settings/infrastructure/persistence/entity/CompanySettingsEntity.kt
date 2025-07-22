package com.carslab.crm.modules.company_settings.infrastructure.persistence.entity

import com.carslab.crm.modules.company_settings.domain.model.*
import com.carslab.crm.modules.company_settings.domain.model.shared.AuditInfo
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "company_settings",
    indexes = [
        Index(name = "idx_company_settings_company_id", columnList = "company_id", unique = true)
    ]
)
class CompanySettingsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "company_id", nullable = false, unique = true)
    val companyId: Long,

    @Column(name = "company_name", nullable = false, length = 200)
    var companyName: String,

    @Column(name = "tax_id", nullable = false, length = 20)
    var taxId: String,

    @Column(name = "address", length = 500)
    var address: String? = null,

    @Column(name = "phone", length = 20)
    var phone: String? = null,

    @Column(name = "website", length = 255)
    var website: String? = null,

    @Column(name = "bank_account_number", length = 50)
    var bankAccountNumber: String? = null,

    @Column(name = "bank_name", length = 100)
    var bankName: String? = null,

    @Column(name = "swift_code", length = 20)
    var swiftCode: String? = null,

    @Column(name = "account_holder_name", length = 200)
    var accountHolderName: String? = null,

    @Column(name = "logo_file_id", length = 100)
    var logoFileId: String? = null,

    @Column(name = "logo_file_name", length = 255)
    var logoFileName: String? = null,

    @Column(name = "logo_content_type", length = 100)
    var logoContentType: String? = null,

    @Column(name = "logo_size")
    var logoSize: Long? = null,

    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) {

    fun toDomain(): CompanySettings = CompanySettings(
        id = CompanySettingsId.of(id!!),
        companyId = companyId,
        basicInfo = CompanyBasicInfo(
            companyName = companyName,
            taxId = taxId,
            address = address,
            phone = phone,
            website = website
        ),
        bankSettings = BankSettings(
            bankAccountNumber = bankAccountNumber,
            bankName = bankName,
            swiftCode = swiftCode,
            accountHolderName = accountHolderName
        ),
        logoSettings = LogoSettings(
            logoFileId = logoFileId,
            logoFileName = logoFileName,
            logoContentType = logoContentType,
            logoSize = logoSize,
            logoUrl = logoUrl
        ),
        audit = AuditInfo(
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy,
            version = version
        )
    )

    companion object {
        fun fromDomain(settings: CreateCompanySettings): CompanySettingsEntity = CompanySettingsEntity(
            companyId = settings.companyId,
            companyName = settings.basicInfo.companyName,
            taxId = settings.basicInfo.taxId,
            address = settings.basicInfo.address,
            phone = settings.basicInfo.phone,
            website = settings.basicInfo.website,
            bankAccountNumber = settings.bankSettings.bankAccountNumber,
            bankName = settings.bankSettings.bankName,
            swiftCode = settings.bankSettings.swiftCode,
            accountHolderName = settings.bankSettings.accountHolderName,
            logoFileId = settings.logoSettings.logoFileId,
            logoFileName = settings.logoSettings.logoFileName,
            logoContentType = settings.logoSettings.logoContentType,
            logoSize = settings.logoSettings.logoSize,
            logoUrl = settings.logoSettings.logoUrl,
            createdAt = settings.audit.createdAt,
            updatedAt = settings.audit.updatedAt,
            createdBy = settings.audit.createdBy,
            updatedBy = settings.audit.updatedBy,
            version = settings.audit.version
        )
    }
}