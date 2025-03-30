package com.carslab.crm.infrastructure.repository

import com.carslab.crm.domain.model.MediaType
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.view.protocol.MediaTypeView
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ImageId(
    val protocolId: ProtocolId,
    val fileId: String
)

/**
 * Serwis przechowujący obrazy w pamięci zamiast na dysku.
 * Dobry do celów rozwojowych i testowych, ale nie zalecany do produkcji
 * (dane są tracone przy restarcie aplikacji).
 */
@Service
class InMemoryImageStorageService {

    // Mapa przechowująca dane obrazów jako bajty z kluczem będącym identyfikatorem pliku
    private val imagesStorage = ConcurrentHashMap<ImageId, ByteArray>()

    // Mapa przechowująca metadane plików
    private val metadataStorage = ConcurrentHashMap<ImageId, ImageMetadata>()

    /**
     * Zapisuje plik w pamięci i zwraca unikalny identyfikator pliku.
     */
    fun storeFile(file: MultipartFile, protocolId: ProtocolId, createMediaTypeModel: CreateMediaTypeModel): String {
        try {
            // Generowanie unikalnego ID dla pliku
            val fileId = UUID.randomUUID().toString()

            // Normalizacja nazwy pliku
            val safeName = file.originalFilename?.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                ?: "${fileId}.bin"

            // Przechowywanie metadanych pliku
            val metadata = ImageMetadata(
                originalName = createMediaTypeModel.name,
                contentType = file.contentType ?: "application/octet-stream",
                size = file.size
            )

            // Zapisywanie danych pliku
            imagesStorage[ImageId(protocolId, fileId)] = file.bytes
            metadataStorage[ImageId(protocolId, fileId)] = metadata

            return fileId
        } catch (e: Exception) {
            throw RuntimeException("Could not store file. Please try again!", e)
        }
    }

    /**
     * Usuwa plik o podanym identyfikatorze.
     */
    fun deleteFile(fileId: String, protocolId: ProtocolId) {
        imagesStorage.remove(ImageId(protocolId, fileId))
        metadataStorage.remove(ImageId(protocolId, fileId))
    }

    /**
     * Pobiera dane pliku o podanym identyfikatorze.
     */
    fun getFileData(fileId: String): ByteArray? {
        return imagesStorage.filter { (k, v) -> k.fileId == fileId }
            .map { it.value }
            .first()
    }

    /**
     * Pobiera metadane pliku o podanym identyfikatorze.
     */
    fun getFileMetadata(fileId: String): ImageMetadata? {
        return metadataStorage.filter { (k, v) -> k.fileId == fileId }
            .map { it.value }
            .first()
    }

    /**
     * Sprawdza, czy plik o podanym identyfikatorze istnieje.
     */
    fun fileExists(fileId: String): Boolean {
        return !imagesStorage.filter { (k, v) -> k.fileId == fileId }.isEmpty()
    }

    /**
     * Zwraca wszystkie przechowywane identyfikatory plików.
     */
    fun getAllFileIds(): Set<String> {
        return imagesStorage.keys.map { it.fileId }.toSet()
    }

    fun getImagesByProtocol(protocolId: ProtocolId): Set<MediaTypeView> {
        return imagesStorage.keys.filter { it.protocolId == protocolId }
            .associate { it to metadataStorage[it] }
            .map { MediaTypeView(
                id = it.key.fileId,
                name = it.value?.originalName ?: "Dupa",
                size = it.value?.size ?: 0
            ) }.toSet()
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