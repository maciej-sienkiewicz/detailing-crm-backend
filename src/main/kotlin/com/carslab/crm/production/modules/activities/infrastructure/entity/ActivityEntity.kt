package com.carslab.crm.production.modules.activities.infrastructure.entity

import com.carslab.crm.production.modules.activities.domain.model.ActivityCategory
import com.carslab.crm.production.modules.activities.domain.model.ActivityStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "activities",
    indexes = [
        Index(name = "idx_activities_company_timestamp", columnList = "company_id,timestamp"),
        Index(name = "idx_activities_category", columnList = "category"),
        Index(name = "idx_activities_primary_entity", columnList = "primary_entity_type,primary_entity_id"),
        Index(name = "idx_activities_user", columnList = "user_id")
    ]
)
class ActivityEntity(
    @Id
    @Column(nullable = false, length = 36)
    val id: String,

    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: ActivityCategory,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,
    
    @Column(nullable = true, columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "user_id", nullable = true)
    val userId: String? = null,

    @Column(name = "user_name", nullable = true)
    val userName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val status: ActivityStatus? = null,

    @Column(name = "status_text", nullable = true)
    val statusText: String? = null,

    @Column(name = "primary_entity_id", nullable = true)
    val primaryEntityId: String? = null,

    @Column(name = "primary_entity_type", nullable = true)
    val primaryEntityType: String? = null,

    @Column(name = "primary_entity_name", nullable = true)
    val primaryEntityName: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val metadataJson: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime
)