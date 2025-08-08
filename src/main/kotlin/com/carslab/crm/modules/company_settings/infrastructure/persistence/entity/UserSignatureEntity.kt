// src/main/kotlin/com/carslab/crm/modules/company_settings/infrastructure/persistence/entity/UserSignatureEntity.kt
package com.carslab.crm.modules.company_settings.infrastructure.persistence.entity

import com.carslab.crm.modules.company_settings.domain.model.CreateUserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignature
import com.carslab.crm.modules.company_settings.domain.model.UserSignatureId
import com.carslab.crm.modules.company_settings.domain.model.shared.AuditInfo
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_signatures",
    indexes = [
        Index(name = "idx_user_signatures_user_company", columnList = "user_id,company_id", unique = true),
        Index(name = "idx_user_signatures_company_id", columnList = "company_id")
    ]
)
class UserSignatureEntity(
    @Id
    @Column(name = "id", length = 36)
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

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

    fun toDomain(): UserSignature = UserSignature(
        id = UserSignatureId.of(id),
        userId = userId,
        companyId = companyId,
        content = content,
        audit = AuditInfo(
            createdAt = createdAt,
            updatedAt = updatedAt,
            createdBy = createdBy,
            updatedBy = updatedBy,
            version = version
        )
    )

    companion object {
        fun fromDomain(signature: CreateUserSignature): UserSignatureEntity = UserSignatureEntity(
            id = UserSignatureId.generate().value,
            userId = signature.userId,
            companyId = signature.companyId,
            content = signature.content,
            createdAt = signature.audit.createdAt,
            updatedAt = signature.audit.updatedAt,
            createdBy = signature.audit.createdBy,
            updatedBy = signature.audit.updatedBy,
            version = signature.audit.version
        )
    }
}