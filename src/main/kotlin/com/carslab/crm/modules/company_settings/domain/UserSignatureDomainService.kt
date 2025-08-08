// src/main/kotlin/com/carslab/crm/modules/company_settings/domain/UserSignatureDomainService.kt
package com.carslab.crm.modules.company_settings.domain

import com.carslab.crm.modules.company_settings.domain.model.CreateUserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignature
import com.carslab.crm.modules.company_settings.domain.port.UserSignatureRepository
import com.carslab.crm.modules.company_settings.domain.port.EncryptionService
import com.carslab.crm.domain.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSignatureDomainService(
    private val userSignatureRepository: UserSignatureRepository,
    private val encryptionService: EncryptionService
) {
    private val logger = LoggerFactory.getLogger(UserSignatureDomainService::class.java)

    fun createSignature(createRequest: CreateUserSignature): UserSignature {
        logger.debug("Creating signature for user: ${createRequest.userId} in company: ${createRequest.companyId}")

        // Check if user already has a signature (only one signature per user per company)
        if (userSignatureRepository.existsByUserIdAndCompanyId(createRequest.userId, createRequest.companyId)) {
            throw DomainException("User signature already exists for user: ${createRequest.userId}")
        }

        // Encrypt the signature content before storing
        val encryptedContent = encryptionService.encrypt(createRequest.content)

        val createWithEncryption = createRequest.copy(content = encryptedContent)
        val savedSignature = userSignatureRepository.saveNew(createWithEncryption)

        logger.info("Created user signature with ID: ${savedSignature.id.value} for user: ${createRequest.userId}")

        // Decrypt for response
        return savedSignature.copy(content = encryptionService.decrypt(savedSignature.content))
    }

    fun updateSignature(userId: Long, companyId: Long, newContent: String): UserSignature {
        logger.debug("Updating signature for user: $userId in company: $companyId")

        val existingSignature = userSignatureRepository.findByUserIdAndCompanyId(userId, companyId)
            ?: throw DomainException("User signature not found for user: $userId")

        // Encrypt the new content
        val encryptedContent = encryptionService.encrypt(newContent)

        val updatedSignature = existingSignature.updateContent(encryptedContent)
        val savedSignature = userSignatureRepository.save(updatedSignature)

        logger.info("Updated signature for user: $userId")

        // Decrypt for response
        return savedSignature.copy(content = encryptionService.decrypt(savedSignature.content))
    }

    @Transactional(readOnly = true)
    fun getUserSignature(userId: Long, companyId: Long): UserSignature? {
        logger.debug("Getting signature for user: $userId in company: $companyId")

        val signature = userSignatureRepository.findByUserIdAndCompanyId(userId, companyId)

        // Decrypt content if signature exists
        return signature?.let {
            it.copy(content = encryptionService.decrypt(it.content))
        }
    }

    fun deleteSignature(userId: Long, companyId: Long): Boolean {
        logger.info("Deleting signature for user: $userId in company: $companyId")

        val deleted = userSignatureRepository.deleteByUserIdAndCompanyId(userId, companyId)

        if (deleted) {
            logger.info("Successfully deleted signature for user: $userId")
        } else {
            logger.warn("Signature not found for deletion: user $userId in company $companyId")
        }

        return deleted
    }
}