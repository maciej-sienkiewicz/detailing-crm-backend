// src/main/kotlin/com/carslab/crm/infrastructure/storage/key/StorageKeyGenerator.kt
package com.carslab.crm.infrastructure.storage.key

import org.springframework.stereotype.Component
import java.time.LocalDate

interface StorageKeyGenerator {
    fun generateKey(request: StorageKeyRequest): String
}

@Component
class HierarchicalStorageKeyGenerator : StorageKeyGenerator {

    override fun generateKey(request: StorageKeyRequest): String {
        val pathParts = mutableListOf<String>()

        // Dodaj prefix dla środowiska (prod/staging/dev)
        request.environment?.let { pathParts.add(it) }

        // Dodaj company ID
        pathParts.add("companies")
        pathParts.add(request.companyId.toString())

        // Dodaj strukturę czasową
        pathParts.add(request.date.year.toString())
        pathParts.add(request.date.monthValue.toString().padStart(2, '0'))

        // Dodaj kategorię i podkategorię
        pathParts.add(request.category)
        request.subCategory?.let { pathParts.add(it) }

        // Dodaj unikalny identyfikator pliku
        pathParts.add(request.fileName)

        return pathParts.joinToString("/")
    }
}

data class StorageKeyRequest(
    val companyId: Long,
    val category: String,
    val subCategory: String? = null,
    val fileName: String,
    val date: LocalDate = LocalDate.now(),
    val environment: String? = null
)