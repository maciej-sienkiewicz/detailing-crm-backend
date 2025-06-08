package com.carslab.crm.modules.company_settings.infrastructure.security

import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

@Service
class AESEncryptionService(
    @Value("\${app.encryption.secret-key:MySecretKey12345}") private val secretKey: String
) : EncryptionService {

    private val algorithm = "AES"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagLength = 16
    private val ivLength = 12
    private val encryptionPrefix = "ENC:"

    override fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return plainText
        if (isEncrypted(plainText)) return plainText

        try {
            val cipher = Cipher.getInstance(transformation)
            val secretKeySpec = SecretKeySpec(secretKey.padEnd(32).take(32).toByteArray(), algorithm)

            val iv = ByteArray(ivLength)
            SecureRandom().nextBytes(iv)
            val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec)
            val encryptedData = cipher.doFinal(plainText.toByteArray())

            val encryptedWithIv = iv + encryptedData
            return encryptionPrefix + Base64.getEncoder().encodeToString(encryptedWithIv)
        } catch (e: Exception) {
            throw RuntimeException("Failed to encrypt data", e)
        }
    }

    override fun decrypt(encryptedText: String): String {
        if (encryptedText.isBlank()) return encryptedText
        if (!isEncrypted(encryptedText)) return encryptedText

        try {
            val encryptedData = Base64.getDecoder().decode(encryptedText.removePrefix(encryptionPrefix))

            val iv = encryptedData.sliceArray(0 until ivLength)
            val cipherText = encryptedData.sliceArray(ivLength until encryptedData.size)

            val cipher = Cipher.getInstance(transformation)
            val secretKeySpec = SecretKeySpec(secretKey.padEnd(32).take(32).toByteArray(), algorithm)
            val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec)
            val decryptedData = cipher.doFinal(cipherText)

            return String(decryptedData)
        } catch (e: Exception) {
            throw RuntimeException("Failed to decrypt data", e)
        }
    }

    override fun isEncrypted(text: String): Boolean {
        return text.startsWith(encryptionPrefix)
    }
}