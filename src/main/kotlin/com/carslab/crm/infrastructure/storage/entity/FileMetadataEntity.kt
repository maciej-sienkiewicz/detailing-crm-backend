// src/main/kotlin/com/carslab/crm/infrastructure/storage/entity/FileMetadataEntity.kt
package com.carslab.crm.infrastructure.storage.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "file_metadata", indexes = [
    Index(name = "idx_file_metadata_company_entity", columnList = "companyId,entityId,entityType"),
    Index(name = "idx_file_metadata_created_at", columnList = "createdAt"),
    Index(name = "idx_file_metadata_storage_id", columnList = "storageId", unique = true)
])
class FileMetadataEntity(
    @Id
    @Column(nullable = false)
    val storageId: String,

    @Column(nullable = false)
    val originalName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val filePath: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val contentType: String,

    @Column(nullable = false)
    val companyId: Long,

    @Column(nullable = false)
    val entityId: String, // documentId, protocolId, etc.

    @Column(nullable = false)
    val entityType: String, // "document", "protocol", "visit", etc.

    @Column(nullable = false)
    val category: String, // "finances", "protocols", "visits"

    @Column
    val subCategory: String?, // "invoices/income", "images", etc.

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val description: String? = null
)