package com.carslab.crm.signature.domain.service

import com.carslab.crm.signature.api.dto.SignaturePlacement
import com.carslab.crm.signature.api.dto.SignatureSessionStatus
import com.carslab.crm.signature.infrastructure.persistance.repository.DocumentPreviewCacheRepository
import com.carslab.crm.signature.infrastructure.persistance.repository.DocumentSignatureSessionRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.isNotEmpty

/**
 * Service for file storage operations
 */
@Service
class FileStorageService(
    @Value("\${app.document-signature.storage-path:/var/crm/documents}")
    private val basePath: String,
    @Value("\${app.document-signature.base-url:http://localhost:8080}")
    private val baseUrl: String
) {

    private val logger = LoggerFactory.getLogger(FileStorageService::class.java)

    init {
        // Ensure storage directories exist
    }

    /**
     * Store uploaded document
     */
    fun storeDocument(documentId: UUID, file: MultipartFile): String {
        val documentsDir = Paths.get(basePath, "documents")
        val fileName = "${documentId}.pdf"
        val filePath = documentsDir.resolve(fileName)

        try {
            Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
            logger.debug("Document stored: $filePath")
            return filePath.toString()
        } catch (e: Exception) {
            logger.error("Failed to store document $documentId", e)
            throw RuntimeException("Failed to store document", e)
        }
    }

    /**
     * Store signature image
     */
    fun storeSignatureImage(sessionId: UUID, base64Image: String): String {
        val signaturesDir = Paths.get(basePath, "signatures")
        val fileName = "${sessionId}_signature.png"
        val filePath = signaturesDir.resolve(fileName)

        try {
            // Remove base64 header
            val imageData = base64Image.substringAfter("base64,")
            val decodedBytes = Base64.getDecoder().decode(imageData)

            Files.write(filePath, decodedBytes)
            logger.debug("Signature image stored: $filePath")
            return filePath.toString()
        } catch (e: Exception) {
            logger.error("Failed to store signature image for session $sessionId", e)
            throw RuntimeException("Failed to store signature image", e)
        }
    }

    /**
     * Store document preview
     */
    fun storePreview(documentId: UUID, page: Int, width: Int, previewData: ByteArray): String {
        val previewsDir = Paths.get(basePath, "previews", documentId.toString())
        Files.createDirectories(previewsDir)

        val fileName = "page_${page}_${width}w.png"
        val filePath = previewsDir.resolve(fileName)

        try {
            Files.write(filePath, previewData)
            logger.debug("Preview stored: $filePath")
            return filePath.toString()
        } catch (e: Exception) {
            logger.error("Failed to store preview for document $documentId, page $page", e)
            throw RuntimeException("Failed to store preview", e)
        }
    }

    /**
     * Read file from storage
     */
    fun readFile(filePath: String): ByteArray {
        try {
            return Files.readAllBytes(Paths.get(filePath))
        } catch (e: Exception) {
            logger.error("Failed to read file: $filePath", e)
            throw RuntimeException("Failed to read file", e)
        }
    }

    /**
     * Generate public URL for file
     */
    fun generateUrl(filePath: String): String {
        val relativePath = Paths.get(basePath).relativize(Paths.get(filePath))
        return "$baseUrl/api/signature/files/$relativePath"
    }

    /**
     * Delete file
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            Files.deleteIfExists(Paths.get(filePath))
        } catch (e: Exception) {
            logger.error("Failed to delete file: $filePath", e)
            false
        }
    }

    /**
     * Clean up old files
     */
    fun cleanupOldFiles(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

        listOf("previews", "signatures").forEach { subDir ->
            val dirPath = Paths.get(basePath, subDir)
            if (Files.exists(dirPath)) {
                try {
                    Files.walk(dirPath)
                        .filter { Files.isRegularFile(it) }
                        .filter { Files.getLastModifiedTime(it).toMillis() < cutoffTime }
                        .forEach {
                            try {
                                Files.delete(it)
                                logger.debug("Cleaned up old file: $it")
                            } catch (e: Exception) {
                                logger.warn("Failed to delete old file: $it", e)
                            }
                        }
                } catch (e: Exception) {
                    logger.error("Error during cleanup of $subDir", e)
                }
            }
        }
    }

    private fun createDirectories() {
        val directories = listOf("documents", "signatures", "previews", "signed-documents")

        directories.forEach { dir ->
            val path = Paths.get(basePath, dir)
            try {
                Files.createDirectories(path)
                logger.info("Storage directory ensured: $path")
            } catch (e: Exception) {
                logger.error("Failed to create storage directory: $path", e)
                throw RuntimeException("Failed to initialize storage", e)
            }
        }
    }
}

/**
 * Service for PDF document processing
 */
@Service
class DocumentProcessingService {

    private val logger = LoggerFactory.getLogger(DocumentProcessingService::class.java)

    /**
     * Get page count from PDF document
     */
    fun getPageCount(filePath: String): Int {
        return try {
            PDDocument.load(File(filePath)).use { document ->
                document.numberOfPages
            }
        } catch (e: Exception) {
            logger.error("Failed to get page count from document: $filePath", e)
            throw RuntimeException("Failed to process document", e)
        }
    }

    /**
     * Generate preview image for document page
     */
    fun generatePreview(filePath: String, page: Int, width: Int): ByteArray {
        return try {
            PDDocument.load(File(filePath)).use { document ->
                val renderer = PDFRenderer(document)
                val pageIndex = page - 1 // Convert to 0-based index

                if (pageIndex < 0 || pageIndex >= document.numberOfPages) {
                    throw IllegalArgumentException("Invalid page number: $page")
                }

                // Calculate DPI to achieve desired width
                val pdfPage = document.getPage(pageIndex)
                val pageWidth = pdfPage.cropBox.width
                val dpi = (width * 72f) / pageWidth

                val image = renderer.renderImageWithDPI(pageIndex, dpi)

                // Convert to PNG bytes
                val outputStream = ByteArrayOutputStream()
                ImageIO.write(image, "PNG", outputStream)
                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            logger.error("Failed to generate preview for document: $filePath, page: $page", e)
            throw RuntimeException("Failed to generate preview", e)
        }
    }

    /**
     * Apply signature to PDF document
     */
    fun applySignatureToDocument(
        originalDocumentPath: String,
        signatureImagePath: String,
        signaturePlacement: SignaturePlacement?,
        sessionId: UUID
    ): String {
        val signedDocumentPath = originalDocumentPath.replace("documents", "signed-documents")
            .replace(".pdf", "_signed_${sessionId}.pdf")

        try {
            // Ensure signed documents directory exists
            Files.createDirectories(Paths.get(signedDocumentPath).parent)

            PDDocument.load(File(originalDocumentPath)).use { document ->
                val signatureImage = PDImageXObject.createFromFile(signatureImagePath, document)

                // Determine signature placement
                val placement = signaturePlacement ?: getDefaultSignaturePlacement(document)
                val pageIndex = placement.page - 1

                if (pageIndex < 0 || pageIndex >= document.numberOfPages) {
                    throw IllegalArgumentException("Invalid page number: ${placement.page}")
                }

                val page = document.getPage(pageIndex)

                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true).use { contentStream ->
                    // Apply signature to page
                    contentStream.drawImage(
                        signatureImage,
                        placement.x,
                        page.cropBox.height - placement.y - placement.height, // PDF coordinate system
                        placement.width,
                        placement.height
                    )
                }

                // Save signed document
                document.save(signedDocumentPath)
                logger.info("Signature applied to document: $signedDocumentPath")

                return signedDocumentPath
            }
        } catch (e: Exception) {
            logger.error("Failed to apply signature to document: $originalDocumentPath", e)
            throw RuntimeException("Failed to apply signature", e)
        }
    }

    /**
     * Extract text content from PDF for indexing/search
     */
    fun extractTextContent(filePath: String): String {
        return try {
            PDDocument.load(File(filePath)).use { document ->
                val textStripper = PDFTextStripper()
                textStripper.getText(document)
            }
        } catch (e: Exception) {
            logger.error("Failed to extract text from document: $filePath", e)
            ""
        }
    }

    /**
     * Validate PDF document integrity
     */
    fun validateDocument(filePath: String): Boolean {
        return try {
            PDDocument.load(File(filePath)).use { document ->
                document.numberOfPages > 0
            }
        } catch (e: Exception) {
            logger.error("Document validation failed: $filePath", e)
            false
        }
    }

    private fun getDefaultSignaturePlacement(document: PDDocument): SignaturePlacement {
        val lastPage = document.numberOfPages
        val page = document.getPage(lastPage - 1)
        val pageWidth = page.cropBox.width
        val pageHeight = page.cropBox.height

        // Default: bottom right corner of last page
        return SignaturePlacement(
            page = lastPage,
            x = pageWidth - 200f, // 200 points from right edge
            y = pageHeight - 100f, // 100 points from bottom
            width = 150f,
            height = 50f
        )
    }
}

/**
 * Service for cleaning up expired sessions and files
 */
@Service
class DocumentCleanupService(
    private val sessionRepository: DocumentSignatureSessionRepository,
    private val previewCacheRepository: DocumentPreviewCacheRepository,
    private val fileStorageService: FileStorageService
) {

    private val logger = LoggerFactory.getLogger(DocumentCleanupService::class.java)

    /**
     * Clean up expired sessions
     */
    fun cleanupExpiredSessions() {
        try {
            val expiredSessions = sessionRepository.findExpiredSessions(Instant.now())

            expiredSessions.forEach { session ->
                try {
                    // Update session status
                    sessionRepository.save(session.updateStatus(SignatureSessionStatus.EXPIRED))
                    logger.debug("Marked session as expired: ${session.sessionId}")
                } catch (e: Exception) {
                    logger.error("Failed to mark session as expired: ${session.sessionId}", e)
                }
            }

            if (expiredSessions.isNotEmpty()) {
                logger.info("Marked ${expiredSessions.size} sessions as expired")
            }
        } catch (e: Exception) {
            logger.error("Error during expired sessions cleanup", e)
        }
    }

    /**
     * Clean up old preview cache
     */
    fun cleanupOldPreviews(olderThanDays: Int = 7) {
        try {
            val cutoffDate = Instant.now().minusSeconds(olderThanDays * 24 * 60 * 60L)
            val stalePreviews = previewCacheRepository.findStalePreviewsOlderThan(cutoffDate)

            stalePreviews.forEach { preview ->
                try {
                    // Delete file
                    fileStorageService.deleteFile(preview.previewPath)
                    // Delete database record
                    previewCacheRepository.delete(preview)
                    logger.debug("Cleaned up preview: ${preview.id}")
                } catch (e: Exception) {
                    logger.error("Failed to cleanup preview: ${preview.id}", e)
                }
            }

            if (stalePreviews.isNotEmpty()) {
                logger.info("Cleaned up ${stalePreviews.size} old preview files")
            }
        } catch (e: Exception) {
            logger.error("Error during preview cleanup", e)
        }
    }

    /**
     * Clean up orphaned files
     */
    fun cleanupOrphanedFiles(olderThanDays: Int = 30) {
        try {
            fileStorageService.cleanupOldFiles(olderThanDays)
            logger.info("Completed orphaned files cleanup")
        } catch (e: Exception) {
            logger.error("Error during orphaned files cleanup", e)
        }
    }
}