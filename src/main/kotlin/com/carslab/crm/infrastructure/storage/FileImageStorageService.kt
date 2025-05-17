package com.carslab.crm.infrastructure.storage

import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.view.protocol.MediaTypeView
import com.carslab.crm.infrastructure.persistence.entity.ImageTagEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleImageEntity
import com.carslab.crm.infrastructure.persistence.repository.ImageTagJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleImageJpaRepository
import com.carslab.crm.infrastructure.repository.InMemoryImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

@Service
class FileImageStorageService(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val vehicleImageJpaRepository: VehicleImageJpaRepository,
    private val imageTagJpaRepository: ImageTagJpaRepository,
    @Value("\${file.upload-dir:uploads}") private val uploadDir: String
) : InMemoryImageStorageService() {

    private val rootLocation: Path = Paths.get(uploadDir)

    init {
        try {
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw RuntimeException("Could not initialize storage location", e)
        }
    }

    override fun storeFile(
        file: MultipartFile,
        protocolId: ProtocolId,
        createMediaTypeModel: CreateMediaTypeModel
    ): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Failed to store empty file")
            }

            // Sprawdzamy czy protokół istnieje
            if (!protocolJpaRepository.existsById(protocolId.value.toLong())) {
                throw RuntimeException("Protocol not found: ${protocolId.value}")
            }

            val protocolIdLong = protocolId.value.toLong()

            // Generujemy unikalne ID dla pliku
            val fileId = UUID.randomUUID().toString()

            // Tworzymy katalog dla protokołu, jeśli nie istnieje
            val protocolDir = rootLocation.resolve(protocolId.value)
            Files.createDirectories(protocolDir)

            // Przechowujemy plik z wygenerowanym ID jako nazwą pliku (zachowując oryginalne rozszerzenie)
            val originalFilename = file.originalFilename ?: "unknown"
            val extension = originalFilename.substringAfterLast('.', "")
            val targetFilename = "$fileId.$extension"
            val targetPath = protocolDir.resolve(targetFilename)

            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            // Tworzymy i zapisujemy encję obrazu z metadanymi
            val imageEntity = VehicleImageEntity(
                id = fileId,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                protocolId = protocolIdLong,
                name = createMediaTypeModel.name,
                contentType = file.contentType ?: "application/octet-stream",
                size = file.size,
                description = createMediaTypeModel.description,
                location = createMediaTypeModel.location,
                storagePath = targetPath.toString()
            )

            // Zapisujemy encję obrazu
            vehicleImageJpaRepository.save(imageEntity)

            // Dodajemy tagi jako osobne encje
            createMediaTypeModel.tags.forEach { tag ->
                val tagEntity = ImageTagEntity(
                    imageId = fileId,
                    companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                    tag = tag
                )
                imageTagJpaRepository.save(tagEntity)
            }

            return fileId
        } catch (e: Exception) {
            throw RuntimeException("Failed to store file", e)
        }
    }

    @Transactional
    override fun deleteFile(fileId: String, protocolId: ProtocolId) {
        try {
            vehicleImageJpaRepository.findById(fileId).ifPresent { imageEntity ->
                // Usuwamy fizyczny plik
                val filePath = Paths.get(imageEntity.storagePath)
                Files.deleteIfExists(filePath)

                // Usuwamy tagi
                val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
                imageTagJpaRepository.deleteAllByImageIdAndCompanyId(fileId, companyId)

                // Usuwamy rekord z bazy danych
                vehicleImageJpaRepository.delete(imageEntity)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete file", e)
        }
    }

    override fun getFileData(fileId: String): ByteArray? {
        try {
            val imageEntity = vehicleImageJpaRepository.findById(fileId).orElse(null) ?: return null
            val filePath = Paths.get(imageEntity.storagePath)
            return Files.readAllBytes(filePath)
        } catch (e: Exception) {
            throw RuntimeException("Failed to read file", e)
        }
    }

    override fun getFileMetadata(fileId: String): ImageMetadata? {
        try {
            val imageEntity = vehicleImageJpaRepository.findById(fileId).orElse(null) ?: return null
            val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
            val tags = imageTagJpaRepository.findByImageIdAndCompanyId(fileId, companyId).map { it.tag }

            return ImageMetadata(
                originalName = imageEntity.name,
                contentType = imageEntity.contentType,
                size = imageEntity.size,
                uploadTime = imageEntity.createdAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
                tags = tags
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to get file metadata", e)
        }
    }

    override fun fileExists(fileId: String): Boolean {
        return vehicleImageJpaRepository.existsById(fileId)
    }

    override fun getAllFileIds(): Set<String> {
        return vehicleImageJpaRepository.findAll().map { it.id }.toSet()
    }

    override fun getImagesByProtocol(protocolId: ProtocolId): Set<MediaTypeView> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val images = vehicleImageJpaRepository.findByProtocolIdAndCompanyId(protocolId.value.toLong(), companyId)

        // Dla każdego obrazu pobieramy tagi
        val result = images.map { image ->
            val tags = imageTagJpaRepository.findByImageIdAndCompanyId(image.id, companyId).map { it.tag }
            image.setTags(tags)
            image.toDomain()
        }.toSet()

        return result
    }

    override fun updateImageMetadata(
        protocolId: ProtocolId,
        imageId: String,
        name: String,
        tags: List<String>,
        description: String?,
        location: String?
    ): MediaTypeView {
        val imageEntity = vehicleImageJpaRepository.findById(imageId).orElseThrow {
            RuntimeException("Image not found: $imageId")
        }
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId


        // Sprawdzamy czy obraz należy do tego protokołu
        if (imageEntity.protocolId != protocolId.value.toLong()) {
            throw RuntimeException("Image does not belong to protocol $protocolId")
        }

        imageEntity.name = name
        imageEntity.description = description
        imageEntity.location = location

        // Aktualizujemy tagi - usuwamy wszystkie i dodajemy nowe
        imageTagJpaRepository.deleteAllByImageIdAndCompanyId(imageId, companyId)
        tags.forEach { tag ->
            val tagEntity = ImageTagEntity(
                imageId = imageId,
                companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId,
                tag = tag
            )
            imageTagJpaRepository.save(tagEntity)
        }

        // Zapisujemy encję obrazu
        val updatedEntity = vehicleImageJpaRepository.save(imageEntity)

        // Pobieramy tagi dla odpowiedzi
        val updatedTags = imageTagJpaRepository.findByImageIdAndCompanyId(imageId, companyId).map { it.tag }
        updatedEntity.setTags(updatedTags)

        return updatedEntity.toDomain()
    }

    fun getFileAsResource(fileId: String): Resource {
        try {
            val imageEntity = vehicleImageJpaRepository.findById(fileId).orElseThrow {
                RuntimeException("Image not found: $fileId")
            }

            val filePath = Paths.get(imageEntity.storagePath)
            val resource = UrlResource(filePath.toUri())

            if (resource.exists() || resource.isReadable) {
                return resource
            } else {
                throw RuntimeException("Could not read file: $fileId")
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException("Could not read file: $fileId", e)
        }
    }

    fun init() {
        try {
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw RuntimeException("Could not initialize storage", e)
        }
    }

    fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile())
    }

    fun loadAll(): Stream<Path> {
        try {
            return Files.walk(rootLocation, 1)
                .filter { path -> !path.equals(rootLocation) }
                .map { rootLocation.relativize(it) }
        } catch (e: IOException) {
            throw RuntimeException("Failed to read stored files", e)
        }
    }
}