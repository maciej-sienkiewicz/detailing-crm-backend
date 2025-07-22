package com.carslab.crm.domain.model.events

import java.time.LocalDateTime

sealed class CompanySettingsEvent {
    abstract val companyId: Long
    abstract val timestamp: LocalDateTime

    data class BasicInfoUpdated(
        override val companyId: Long,
        val companyName: String,
        val taxId: String,
        val address: String?,
        val phone: String?,
        val website: String?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : CompanySettingsEvent()

    data class BankSettingsUpdated(
        override val companyId: Long,
        val bankAccountNumber: String?,
        val bankName: String?,
        val swiftCode: String?,
        val accountHolderName: String?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : CompanySettingsEvent()

    data class LogoUpdated(
        override val companyId: Long,
        val logoFileId: String?,
        val logoFileName: String?,
        val logoContentType: String?,
        val logoSize: Long?,
        override val timestamp: LocalDateTime = LocalDateTime.now()
    ) : CompanySettingsEvent()
}