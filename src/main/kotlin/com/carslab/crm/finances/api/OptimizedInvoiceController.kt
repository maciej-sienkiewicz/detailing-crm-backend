package com.carslab.crm.finances.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.api.model.response.InvoiceDataResponse
import com.carslab.crm.domain.finances.invoices.ParallelInvoiceExtractionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@RestController
@RequestMapping("/api/invoice")
@Tag(name = "Invoices", description = "Invoice management endpoints")
class OptimizedInvoiceController(
    private val invoiceExtractionService: ParallelInvoiceExtractionService
) : BaseController() {

    @PostMapping("/extract", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Extract data from invoice", description = "Extracts structured data from a PDF invoice or image using AI")
    fun extractInvoiceData(
        @Parameter(description = "Invoice file (PDF or image)", required = true)
        @RequestParam("file") file: MultipartFile,

        @Parameter(description = "Enable image preprocessing (resize/compress)")
        @RequestParam(name = "preprocess", required = false, defaultValue = "true") preprocess: Boolean
    ): ResponseEntity<InvoiceDataResponse> {
        logger.info("Received request to extract data from invoice: ${file.originalFilename}, size: ${file.size} bytes, content type: ${file.contentType}")

        try {
            validateInvoiceFile(file)

            // Preprocess image if requested
            val (processedBytes, processedContentType) = if (preprocess && file.contentType?.contains("image") == true) {
                logger.info("Preprocessing image before extraction")
                val optimizedImage = preprocessImage(file.bytes)
                Pair(optimizedImage, file.contentType ?: "image/jpeg")
            } else {
                Pair(file.bytes, file.contentType ?: "application/octet-stream")
            }

            val startTime = System.currentTimeMillis()

            // Process the file using AI service - conversion happens inside the service
            val extractedData = invoiceExtractionService.extractInvoiceData(
                fileBytes = processedBytes,
                contentType = processedContentType
            )

            val totalTime = System.currentTimeMillis() - startTime
            logger.info("Successfully extracted data from invoice in $totalTime ms")

            return ok(extractedData)
        } catch (e: Exception) {
            return logAndRethrow("Error processing invoice file: ${e.message}", e)
        }
    }

    private fun preprocessImage(imageBytes: ByteArray): ByteArray {
        try {
            // Read the image
            val originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: return imageBytes // Return original if can't read

            // Resize if image is very large (over 2000px wide)
            val maxWidth = 2000
            val resizedImage = if (originalImage.width > maxWidth) {
                val ratio = maxWidth.toDouble() / originalImage.width.toDouble()
                val newHeight = (originalImage.height * ratio).toInt()

                val scaledImage = originalImage.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH)
                val bufferedImage = BufferedImage(maxWidth, newHeight, BufferedImage.TYPE_INT_RGB)

                val g2d = bufferedImage.createGraphics()
                g2d.drawImage(scaledImage, 0, 0, null)
                g2d.dispose()

                bufferedImage
            } else {
                originalImage
            }

            // Write to output with controlled quality
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(resizedImage, "jpeg", outputStream)

            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.warn("Error preprocessing image: ${e.message}. Using original image.")
            return imageBytes
        }
    }

    private fun validateInvoiceFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("File cannot be empty")
        }

        val contentType = file.contentType?.lowercase() ?: ""
        if (!(contentType.contains("pdf") || contentType.contains("image"))) {
            throw IllegalArgumentException("Only PDF and image files are supported. Received: $contentType")
        }

        if (file.size > 10 * 1024 * 1024) { // 10MB
            throw IllegalArgumentException("File size exceeds maximum limit of 10MB")
        }
    }
}