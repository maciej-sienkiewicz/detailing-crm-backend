// src/main/kotlin/com/carslab/crm/modules/company_settings/domain/port/UserSignatureRepository.kt
package com.carslab.crm.modules.company_settings.domain.port

import com.carslab.crm.modules.company_settings.domain.model.CreateUserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignatureId

interface UserSignatureRepository {
    fun save(signature: UserSignature): UserSignature
    fun saveNew(signature: CreateUserSignature): UserSignature
    fun findByUserIdAndCompanyId(userId: Long, companyId: Long): UserSignature?
    fun findById(id: UserSignatureId): UserSignature?
    fun existsByUserIdAndCompanyId(userId: Long, companyId: Long): Boolean
    fun deleteByUserIdAndCompanyId(userId: Long, companyId: Long): Boolean
}