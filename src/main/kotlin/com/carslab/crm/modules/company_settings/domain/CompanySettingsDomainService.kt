package com.carslab.crm.modules.company_settings.domain

import com.carslab.crm.modules.company_settings.api.requests.UpdateCompanySettingsRequest
import com.carslab.crm.modules.company_settings.domain.model.CompanySettings
import com.carslab.crm.modules.company_settings.domain.model.CompanySettingsId
import com.carslab.crm.modules.company_settings.domain.model.CreateCompanySettings
import com.carslab.crm.modules.company_settings.domain.model.EmailSettings
import com.carslab.crm.modules.company_settings.domain.port.CompanySettingsRepository
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import com.carslab.crm.domain.exception.DomainException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompanySettingsDomainService(
    private val companySettingsRepository: CompanySettingsRepository,
    private val encryptionService: EncryptionService
) {

    fun createCompanySettings(createRequest: CreateCompanySettings): CompanySettings {
        // Sprawdź czy ustawienia dla firmy już istnieją
        if (companySettingsRepository.existsByCompanyId(createRequest.companyId)) {
            throw DomainException("Company settings already exist for company: ${createRequest.companyId}")
        }

        validateBasicInfo(createRequest.basicInfo.companyName, createRequest.basicInfo.taxId)

        val settingsWithEncryptedPasswords = createRequest.copy(
            emailSettings = encryptEmailPasswords(createRequest.emailSettings)
        )

        return companySettingsRepository.saveNew(settingsWithEncryptedPasswords)
    }

    fun updateCompanySettings(companyId: Long, updateRequest: UpdateCompanySettingsRequest): CompanySettings {
        val existingSettings = companySettingsRepository.findByCompanyId(companyId)
            ?: throw DomainException("Company settings not found for company: $companyId")

        validateBasicInfo(updateRequest.basicInfo.companyName, updateRequest.basicInfo.taxId)

        val updatedSettings = existingSettings.copy(
            basicInfo = updateRequest.basicInfo,
            bankSettings = updateRequest.bankSettings,
            emailSettings = encryptEmailPasswords(updateRequest.emailSettings),
            logoSettings = updateRequest.logoSettings,
            audit = existingSettings.audit.updated()
        )

        return companySettingsRepository.save(updatedSettings)
    }

    @Transactional(readOnly = true)
    fun getCompanySettings(companyId: Long): CompanySettings? {
        val settings = companySettingsRepository.findByCompanyId(companyId)
        return settings?.copy(
            emailSettings = decryptEmailPasswords(settings.emailSettings)
        )
    }

    @Transactional(readOnly = true)
    fun getCompanySettingsById(id: CompanySettingsId): CompanySettings? {
        val settings = companySettingsRepository.findById(id)
        return settings?.copy(
            emailSettings = decryptEmailPasswords(settings.emailSettings)
        )
    }

    fun deleteCompanySettings(companyId: Long): Boolean {
        return companySettingsRepository.deleteByCompanyId(companyId)
    }

    private fun validateBasicInfo(companyName: String, taxId: String) {
        require(companyName.isNotBlank()) { "Company name cannot be blank" }
        require(taxId.isNotBlank()) { "Tax ID (NIP) cannot be blank" }
        require(isValidPolishNIP(taxId)) { "Invalid Polish NIP format" }
    }

    private fun isValidPolishNIP(nip: String): Boolean {
        val nipDigits = nip.replace("-", "").replace(" ", "")
        if (nipDigits.length != 10 || !nipDigits.all { it.isDigit() }) {
            return false
        }

        // Sprawdzenie sumy kontrolnej NIP
        val weights = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        val digits = nipDigits.map { it.toString().toInt() }

        val sum = weights.zip(digits.take(9)).sumOf { it.first * it.second }
        val checkDigit = sum % 11

        return checkDigit == digits[9]
    }

    private fun encryptEmailPasswords(emailSettings: EmailSettings): EmailSettings {
        return emailSettings.copy(
            smtpPassword = emailSettings.smtpPassword?.let {
                if (encryptionService.isEncrypted(it)) it else encryptionService.encrypt(it)
            },
            imapPassword = emailSettings.imapPassword?.let {
                if (encryptionService.isEncrypted(it)) it else encryptionService.encrypt(it)
            }
        )
    }

    private fun decryptEmailPasswords(emailSettings: EmailSettings): EmailSettings {
        return emailSettings.copy(
            smtpPassword = emailSettings.smtpPassword?.let {
                if (encryptionService.isEncrypted(it)) encryptionService.decrypt(it) else it
            },
            imapPassword = emailSettings.imapPassword?.let {
                if (encryptionService.isEncrypted(it)) encryptionService.decrypt(it) else it
            }
        )
    }
}