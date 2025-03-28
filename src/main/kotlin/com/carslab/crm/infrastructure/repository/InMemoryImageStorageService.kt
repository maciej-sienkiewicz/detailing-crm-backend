package com.carslab.crm.infrastructure.repository

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Serwis przechowujący obrazy w pamięci zamiast na dysku.
 * Dobry do celów rozwojowych i testowych, ale nie zalecany do produkcji
 * (dane są tracone przy restarcie aplikacji).
 */
@Service
class InMemoryImageStorageService {

    // Mapa przechowująca dane obrazów jako bajty z kluczem będącym identyfikatorem pliku
    private val imagesStorage = ConcurrentHashMap<String, ByteArray>()

    // Mapa przechowująca metadane plików
    private val metadataStorage = ConcurrentHashMap<String, ImageMetadata>()

    /**
     * Zapisuje plik w pamięci i zwraca unikalny identyfikator pliku.
     */
    fun storeFile(file: MultipartFile): String {
        try {
            // Generowanie unikalnego ID dla pliku
            val fileId = UUID.randomUUID().toString()

            // Normalizacja nazwy pliku
            val safeName = file.originalFilename?.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                ?: "${fileId}.bin"

            // Przechowywanie metadanych pliku
            val metadata = ImageMetadata(
                originalName = file.originalFilename ?: "unknown",
                contentType = file.contentType ?: "application/octet-stream",
                size = file.size
            )

            // Zapisywanie danych pliku
            imagesStorage[fileId] = file.bytes
            metadataStorage[fileId] = metadata

            return fileId
        } catch (e: Exception) {
            throw RuntimeException("Could not store file. Please try again!", e)
        }
    }

    /**
     * Usuwa plik o podanym identyfikatorze.
     */
    fun deleteFile(fileId: String) {
        imagesStorage.remove(fileId)
        metadataStorage.remove(fileId)
    }

    /**
     * Pobiera dane pliku o podanym identyfikatorze.
     */
    fun getFileData(fileId: String): ByteArray? {
        return imagesStorage[fileId]
    }

    /**
     * Pobiera metadane pliku o podanym identyfikatorze.
     */
    fun getFileMetadata(fileId: String): ImageMetadata? {
        return metadataStorage[fileId]
    }

    /**
     * Sprawdza, czy plik o podanym identyfikatorze istnieje.
     */
    fun fileExists(fileId: String): Boolean {
        return imagesStorage.containsKey(fileId)
    }

    /**
     * Zwraca wszystkie przechowywane identyfikatory plików.
     */
    fun getAllFileIds(): Set<String> {
        return imagesStorage.keys
    }

    /**
     * Klasa do przechowywania metadanych pliku.
     */
    data class ImageMetadata(
        val originalName: String,
        val contentType: String,
        val size: Long,
        val uploadTime: Long = System.currentTimeMillis()
    )
}