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
    private val universalStorageService: UniversalStorageService
) : InMemoryImageStorageService() {

    override fun storeFile(
        file: MultipartFile,
        protocolId: ProtocolId,
        createMediaTypeModel: CreateMediaTypeModel
    ): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Failed to store empty file")
            }

            val protocolIdLong = protocolId.value.toLong()
            if (!protocolJpaRepository.existsById(protocolIdLong)) {
                throw RuntimeException("Protocol not found: ${protocolId.value}")
            }

            val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

            val storageId = universalStorageService.storeFile(
                UniversalStoreRequest(
                    file = file,
                    originalFileName = file.originalFilename ?: "image.jpg",
                    contentType = file.contentType ?: "image/jpeg",
                    companyId = companyId,
                    entityId = protocolId.value,
                    entityType = "protocol",
                    category = "protocols",
                    subCategory = "images",
                    description = createMediaTypeModel.description,
                    tags = mapOf(
                        "location" to (createMediaTypeModel.location ?: ""),
                        "protocol" to protocolId.value
                    ) + createMediaTypeModel.tags.mapIndexed { index, tag -> "tag_$index" to tag }
                )
            )

            // Zachowujemy kompatybilność z istniejącą strukturą
            val imageEntity = VehicleImageEntity(
                id = storageId,
                companyId = companyId,
                protocolId = protocolIdLong,
                name = createMediaTypeModel.name,
                contentType = file.contentType ?: "application/octet-stream",
                size = file.size,
                description = createMediaTypeModel.description,
                location = createMediaTypeModel.location,
                storagePath = storageId // Teraz to jest storage ID
            )

            vehicleImageJpaRepository.save(imageEntity)

            createMediaTypeModel.tags.forEach { tag ->
                val tagEntity = ImageTagEntity(
                    imageId = storageId,
                    companyId = companyId,
                    tag = tag
                )
                imageTagJpaRepository.save(tagEntity)
            }

            return storageId

        } catch (e: Exception) {
            throw RuntimeException("Failed to store file", e)
        }
    }

    @Transactional
    override fun deleteFile(fileId: String, protocolId: ProtocolId) {
        try {
            vehicleImageJpaRepository.findById(fileId).ifPresent { imageEntity ->
                universalStorageService.deleteFile(fileId)

                val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
                imageTagJpaRepository.deleteAllByImageIdAndCompanyId(fileId, companyId)
                vehicleImageJpaRepository.delete(imageEntity)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete file", e)
        }
    }

    fun find(files: List<ImageTagEntity>): List<VehicleImageEntity> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        return vehicleImageJpaRepository.findByCompanyIdAndIdIn(companyId, files.mapNotNull { it.imageId }.toSet())
    }

    override fun getFileData(fileId: String): ByteArray? {
        return universalStorageService.retrieveFile(fileId)
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
}