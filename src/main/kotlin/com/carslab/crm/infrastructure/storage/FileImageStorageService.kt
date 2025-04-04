package com.carslab.crm.infrastructure.storage

import com.carslab.crm.domain.model.MediaType
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.view.protocol.MediaTypeView
import com.carslab.crm.infrastructure.persistence.entity.ImageTagEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleImageEntity
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleImageJpaRepository
import com.carslab.crm.infrastructure.repository.InMemoryImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

@Service
@Transactional
class FileImageStorageService(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val vehicleImageJpaRepository: VehicleImageJpaRepository,
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

    override fun storeFile(file: MultipartFile, protocolId: ProtocolId, createMediaTypeModel: CreateMediaTypeModel): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Failed to store empty file")
            }

            val protocolEntity = protocolJpaRepository.findById(protocolId.value).orElseThrow {
                RuntimeException("Protocol not found: ${protocolId.value}")
            }

            // Generate a unique ID for the file
            val fileId = UUID.randomUUID().toString()

            // Create directory for protocol if it doesn't exist
            val protocolDir = rootLocation.resolve(protocolId.value)
            Files.createDirectories(protocolDir)

            // Store file with the generated ID as filename (preserving original extension)
            val originalFilename = file.originalFilename ?: "unknown"
            val extension = originalFilename.substringAfterLast('.', "")
            val targetFilename = "$fileId.$extension"
            val targetPath = protocolDir.resolve(targetFilename)

            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            // Create and save image entity with metadata
            val imageEntity = VehicleImageEntity(
                id = fileId,
                protocol = protocolEntity,
                name = createMediaTypeModel.name,
                contentType = file.contentType ?: "application/octet-stream",
                size = file.size,
                description = createMediaTypeModel.description,
                location = createMediaTypeModel.location,
                storagePath = targetPath.toString()
            )

            // Add tags
            createMediaTypeModel.tags.forEach { tag ->
                val tagEntity = ImageTagEntity(imageEntity, tag)
                imageEntity.tags.add(tagEntity)
            }

            vehicleImageJpaRepository.save(imageEntity)

            return fileId
        } catch (e: Exception) {
            throw RuntimeException("Failed to store file", e)
        }
    }

    override fun deleteFile(fileId: String, protocolId: ProtocolId) {
        try {
            vehicleImageJpaRepository.findById(fileId).ifPresent { imageEntity ->
                // Delete the physical file
                val filePath = Paths.get(imageEntity.storagePath)
                Files.deleteIfExists(filePath)

                // Delete the database record
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

            return ImageMetadata(
                originalName = imageEntity.name,
                contentType = imageEntity.contentType,
                size = imageEntity.size,
                uploadTime = imageEntity.createdAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
                tags = imageEntity.tags.map { it.tag }
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
        return vehicleImageJpaRepository.findByProtocol_Id(protocolId.value.toLong())
            .map { it.toDomain() }
            .toSet()
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

        imageEntity.name = name
        imageEntity.description = description
        imageEntity.location = location

        // Update tags
        imageEntity.tags.clear()
        tags.forEach { tag ->
            val tagEntity = ImageTagEntity(imageEntity, tag)
            imageEntity.tags.add(tagEntity)
        }

        val updatedEntity = vehicleImageJpaRepository.save(imageEntity)
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