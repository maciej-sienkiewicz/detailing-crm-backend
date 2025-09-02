// src/main/kotlin/com/carslab/crm/infrastructure/storage/provider/StorageProvider.kt
package com.carslab.crm.infrastructure.storage.provider

import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.time.LocalDateTime

interface StorageProvider {

    /**
     * Przechowuje plik i zwraca unikalny identyfikator
     */
    fun store(request: StoreFileRequest): StoreFileResponse

    /**
     * Pobiera plik jako strumień bajtów
     */
    fun retrieve(storageKey: String): ByteArray?

    /**
     * Pobiera plik jako InputStream (lepsze dla dużych plików)
     */
    fun retrieveAsStream(storageKey: String): InputStream?

    /**
     * Usuwa plik
     */
    fun delete(storageKey: String): Boolean

    /**
     * Sprawdza czy plik istnieje
     */
    fun exists(storageKey: String): Boolean

    /**
     * Generuje podpisany URL do pobrania (dla AWS S3/CloudFront)
     */
    fun generatePresignedUrl(storageKey: String, expirationMinutes: Int = 60): String?

    /**
     * Pobiera metadane pliku
     */
    fun getMetadata(storageKey: String): StorageMetadata?

    /**
     * Kopiuje plik między lokalizacjami
     */
    fun copy(sourceKey: String, targetKey: String): Boolean

    /**
     * Przenosi plik między lokalizacjami
     */
    fun move(sourceKey: String, targetKey: String): Boolean
}

data class StoreFileRequest(
    val file: MultipartFile? = null,
    val inputStream: InputStream? = null,
    val fileName: String,
    val metadata: Map<String, String> = emptyMap(),
    val tags: Map<String, String> = emptyMap()
)

data class StoreFileResponse(
    val storageKey: String,
    val size: Long,
    val etag: String? = null,
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)

data class StorageMetadata(
    val size: Long,
    val contentType: String,
    val lastModified: LocalDateTime,
    val etag: String? = null,
    val customMetadata: Map<String, String> = emptyMap()
)

enum class StorageClass {
    STANDARD,          // AWS S3 Standard / Local disk
    INFREQUENT_ACCESS, // AWS S3 IA
    ARCHIVE,           // AWS S3 Glacier
    DEEP_ARCHIVE       // AWS S3 Deep Archive
}