package com.carslab.crm.modules.company_settings.domain.port

interface EncryptionService {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedText: String): String
    fun isEncrypted(text: String): Boolean
}