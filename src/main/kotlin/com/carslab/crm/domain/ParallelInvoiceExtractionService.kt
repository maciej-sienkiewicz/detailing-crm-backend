package com.carslab.crm.service

import com.carslab.crm.api.model.response.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

/**
 * Service for extracting data from invoices using AI with parallel processing
 */
@Service
class ParallelInvoiceExtractionService(
    private val objectMapper: ObjectMapper,
    @Value("\${openai.api.key}") private val apiKey: String,
    @Value("\${openai.api.model:gpt-4o}") private val model: String,
    @Value("\${openai.api.url:https://api.openai.com/v1/chat/completions}") private val apiUrl: String,
    @Value("\${invoice.parallel.enabled:true}") private val parallelEnabled: Boolean = true,
    @Value("\${invoice.compress.image:true}") private val compressImage: Boolean = true,
    @Value("\${invoice.image.quality:0.8}") private val imageQuality: Float = 0.8f,
    @Value("\${invoice.image.max-width:2000}") private val maxImageWidth: Int = 2000
) {
    private val logger = LoggerFactory.getLogger(ParallelInvoiceExtractionService::class.java)

    // Create a custom RestTemplate with proper message converters
    private val restTemplate: RestTemplate by lazy {
        RestTemplate().apply {
            // Ensure proper UTF-8 handling
            messageConverters.add(0, StringHttpMessageConverter(StandardCharsets.UTF_8))
        }
    }

    /**
     * Extracts data from an invoice file using AI
     *
     * @param fileBytes The invoice file content
     * @param contentType The content type of the file
     * @return Structured invoice data
     */
    fun extractInvoiceData(fileBytes: ByteArray, contentType: String): InvoiceDataResponse {
        logger.debug("Starting invoice data extraction with content type: $contentType")
        val startTime = System.currentTimeMillis()

        // Check if we need to convert PDF to image
        val (processedBytes, processedContentType) = if (contentType.contains("pdf", ignoreCase = true)) {
            logger.debug("Converting PDF to image for AI processing")
            val imageBytes = convertPdfToImage(fileBytes)

            // Optionally compress the image to reduce size and processing time
            val finalImageBytes = if (compressImage) compressImage(imageBytes) else imageBytes

            Pair(finalImageBytes, "image/png")
        } else {
            // For existing images, still consider compression
            val finalImageBytes = if (compressImage) compressImage(fileBytes) else fileBytes
            Pair(finalImageBytes, contentType)
        }

        // Convert file to Base64
        val base64File = Base64.getEncoder().encodeToString(processedBytes)

        // Create image URL for OpenAI API
        val imageUrl = "data:$processedContentType;base64,$base64File"

        if (parallelEnabled) {
            // Parallel extraction using CompletableFuture
            logger.debug("Using parallel processing for invoice extraction")
            return extractInParallel(imageUrl)
        } else {
            // Single extraction
            logger.debug("Using single request for invoice extraction")
            val jsonContent = getChatCompletion(imageUrl, getFullPrompt())
                ?: throw RuntimeException("Failed to get response from AI service")

            logger.debug("Received JSON response from AI service")

            // Parse the JSON response into our model
            try {
                val result = objectMapper.readValue(jsonContent, InvoiceDataResponse::class.java)
                val totalTime = System.currentTimeMillis() - startTime
                logger.info("Total invoice extraction time: $totalTime ms")
                return result
            } catch (e: Exception) {
                logger.error("Failed to parse JSON response: $jsonContent", e)
                throw RuntimeException("Failed to parse AI response: ${e.message}", e)
            }
        }
    }

    /**
     * Extracts invoice data by making parallel requests to the AI service
     */
    private fun extractInParallel(imageUrl: String): InvoiceDataResponse {
        val startTime = System.currentTimeMillis()

        try {
            // Create two futures for parallel execution
            val headersFuture = CompletableFuture.supplyAsync {
                getChatCompletion(imageUrl, getHeadersPrompt())
            }

            val itemsFuture = CompletableFuture.supplyAsync {
                getChatCompletion(imageUrl, getItemsPrompt())
            }

            // Wait for both futures to complete
            val headersJson = headersFuture.get()
                ?: throw RuntimeException("Failed to get headers response from AI service")

            val itemsJson = itemsFuture.get()
                ?: throw RuntimeException("Failed to get items response from AI service")

            logger.debug("Received both parallel responses from AI service")

            // Parse responses
            val headersResponse = parseHeadersResponse(headersJson)
            val itemsResponse = parseItemsResponse(itemsJson)

            // Merge the responses
            val mergedResponse = mergeResponses(headersResponse, itemsResponse)

            val totalTime = System.currentTimeMillis() - startTime
            logger.info("Total parallel invoice extraction time: $totalTime ms")

            return mergedResponse
        } catch (e: Exception) {
            logger.error("Error in parallel extraction: ${e.message}", e)
            throw RuntimeException("Failed in parallel invoice extraction: ${e.message}", e)
        }
    }

    /**
     * Parses the headers response JSON into a partial data model
     */
    private fun parseHeadersResponse(json: String): HeadersResponse {
        try {
            return objectMapper.readValue(json.getOnlyJson(), HeadersResponse::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse headers response: $json", e)
            throw RuntimeException("Failed to parse headers response: ${e.message}", e)
        }
    }

    /**
     * Parses the items response JSON into a partial data model
     */
    private fun parseItemsResponse(json: String): ItemsResponse {
        try {
            return objectMapper.readValue(json.getOnlyJson(), ItemsResponse::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse items response: $json", e)
            throw RuntimeException("Failed to parse items response: ${e.message}", e)
        }
    }

    /**
     * Merges the two partial responses into a complete invoice data response
     */
    private fun mergeResponses(headersResponse: HeadersResponse, itemsResponse: ItemsResponse): InvoiceDataResponse {
        return InvoiceDataResponse(
            extractedInvoiceData = ExtractedInvoiceData(
                generalInfo = headersResponse.extractedInvoiceHeaders.generalInfo,
                seller = headersResponse.extractedInvoiceHeaders.seller,
                buyer = headersResponse.extractedInvoiceHeaders.buyer,
                items = itemsResponse.extractedInvoiceItems.items,
                summary = itemsResponse.extractedInvoiceItems.summary,
                notes = itemsResponse.extractedInvoiceItems.additionalInfo?.notes
            )
        )
    }

    /**
     * Converts a PDF to an image (first page only)
     *
     * @param pdfBytes The PDF file as bytes
     * @return The image bytes (PNG format)
     */
    private fun convertPdfToImage(pdfBytes: ByteArray): ByteArray {
        try {
            PDDocument.load(pdfBytes).use { document ->
                // Check if document has pages
                if (document.numberOfPages < 1) {
                    throw RuntimeException("PDF has no pages")
                }

                val renderer = PDFRenderer(document)
                val image: BufferedImage = renderer.renderImageWithDPI(0, 300f) // First page at 300 DPI

                ByteArrayOutputStream().use { output ->
                    ImageIO.write(image, "png", output)
                    return output.toByteArray()
                }
            }
        } catch (e: Exception) {
            logger.error("Error converting PDF to image", e)
            throw RuntimeException("Failed to convert PDF to image: ${e.message}", e)
        }
    }

    /**
     * Compresses an image to reduce size
     */
    private fun compressImage(imageBytes: ByteArray): ByteArray {
        try {
            // Read the original image
            val originalImage = ImageIO.read(imageBytes.inputStream())
            if (originalImage == null) {
                logger.warn("Could not read image for compression, returning original")
                return imageBytes
            }

            // Resize if needed
            val resizedImage = if (originalImage.width > maxImageWidth) {
                val ratio = maxImageWidth.toDouble() / originalImage.width.toDouble()
                val newHeight = (originalImage.height * ratio).toInt()

                val scaledImage = BufferedImage(maxImageWidth, newHeight, originalImage.type)
                val g = scaledImage.createGraphics()
                g.drawImage(originalImage, 0, 0, maxImageWidth, newHeight, null)
                g.dispose()

                scaledImage
            } else {
                originalImage
            }

            // Write to output stream with compression
            ByteArrayOutputStream().use { output ->
                val writers = ImageIO.getImageWritersByFormatName("jpeg")
                if (!writers.hasNext()) {
                    logger.warn("No JPEG image writers found, returning original image")
                    return imageBytes
                }

                val writer = writers.next()
                val param = writer.defaultWriteParam

                param.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                param.compressionQuality = imageQuality // 0.0-1.0, lower means more compression

                val ios = ImageIO.createImageOutputStream(output)
                writer.output = ios
                writer.write(null, javax.imageio.IIOImage(resizedImage, null, null), param)
                ios.close()
                writer.dispose()

                return output.toByteArray()
            }
        } catch (e: Exception) {
            logger.warn("Error compressing image: ${e.message}. Using original image instead.")
            return imageBytes
        }
    }

    /**
     * Sends a request to the OpenAI API and gets the completion response
     *
     * @param imageUrl The base64 encoded image URL
     * @param prompt The system prompt to use
     * @return JSON string with the extracted data
     */
    private fun getChatCompletion(imageUrl: String, prompt: String): String? {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Bearer $apiKey")
        }

        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to listOf(
                        mapOf(
                            "type" to "text",
                            "text" to prompt
                        )
                    )
                ),
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf(
                                "url" to imageUrl
                            )
                        )
                    )
                )
            ),
            "max_tokens" to 4000
        )

        // Convert the request to JSON string
        val jsonRequest = objectMapper.writeValueAsString(requestBody)
        logger.debug("Sending request to OpenAI API with model: $model and prompt type: ${prompt.take(20)}...")

        try {
            // Use the modified approach for API calls
            val httpEntity = HttpEntity(jsonRequest, headers)
            val response: ResponseEntity<String> = restTemplate.postForEntity(apiUrl, httpEntity, String::class.java)

            if (response.statusCode.is2xxSuccessful) {
                val responseBody = response.body?.getOnlyJson()
                if (responseBody != null) {
                    return extractContent(responseBody)
                } else {
                    logger.error("Empty response body from OpenAI API")
                    return null
                }
            } else {
                logger.error("OpenAI API returned error status: ${response.statusCode}")
                return null
            }
        } catch (e: Exception) {
            logger.error("Error calling OpenAI API: ${e.message}", e)
            throw RuntimeException("Failed to communicate with AI service: ${e.message}", e)
        }
    }

    private fun String.getOnlyJson(): String =
        this.substring(this.indexOfFirst { c -> c == '{' }, this.indexOfLast { c -> c == '}' }+1)

    /**
     * Extracts the content from the OpenAI API response
     *
     * @param jsonString The OpenAI API response
     * @return The content from the response
     */
    private fun extractContent(jsonString: String): String {
        try {
            val rootNode: JsonNode = objectMapper.readTree(jsonString)
            val content = rootNode["choices"]?.get(0)?.get("message")?.get("content")?.asText()

            if (content.isNullOrBlank()) {
                logger.warn("Could not extract content from API response: $jsonString")
                return "No content found in response"
            }

            return content
        } catch (e: Exception) {
            logger.error("Error parsing OpenAI API response: ${e.message}", e)
            throw RuntimeException("Failed to parse API response: ${e.message}", e)
        }
    }

    /**
     * Gets the complete system prompt for the AI
     */
    private fun getFullPrompt(): String {
        return """
            Extract data from the invoice and respond with JSON using the following structure:
            {
              "extractedInvoiceData": {
                "generalInfo": {
                  "invoiceNumber": "string or null",
                  "title": "string or null",
                  "type": "INCOME or EXPENSE",
                  "issuedDate": "YYYY-MM-DD",
                  "dueDate": "YYYY-MM-DD",
                  "currency": "PLN",
                  "status": "PAID, ISSUED, etc.",
                  "paymentMethod": "CASH, BANK_TRANSFER, etc."
                },
                "seller": {
                  "name": "string",
                  "taxId": "string or null",
                  "address": "string or null"
                },
                "buyer": {
                  "name": "string",
                  "taxId": "string or null",
                  "address": "string or null"
                },
                "items": [
                  {
                    "name": "string",
                    "description": "string or null",
                    "quantity": number,
                    "unitPrice": number,
                    "taxRate": number,
                    "totalNet": number,
                    "totalGross": number
                  }
                ],
                "summary": {
                  "totalNet": number,
                  "totalTax": number,
                  "totalGross": number
                },
                "additionalInfo": {
                  "notes": "string or null",
                  "protocolNumber": "string or null",
                  "protocolId": "string or null"
                }
              }
            }
            
            If you cannot determine a specific field, provide a reasonable default or null value. Make sure the response is valid JSON.
        """.trimIndent()
    }

    /**
     * Gets the system prompt for extracting header information
     */
    private fun getHeadersPrompt(): String {
        return """
            Extract ONLY the header information from the invoice and respond with JSON using the following structure:
            {
              "extractedInvoiceHeaders": {
                "generalInfo": {
                  "invoiceNumber": "string or null",
                  "title": "string or null",
                  "type": "INCOME or EXPENSE",
                  "issuedDate": "YYYY-MM-DD",
                  "dueDate": "YYYY-MM-DD",
                  "currency": "PLN",
                  "status": "PAID, ISSUED, etc.",
                  "paymentMethod": "CASH, BANK_TRANSFER, etc."
                },
                "seller": {
                  "name": "string",
                  "taxId": "string or null",
                  "address": "string or null"
                },
                "buyer": {
                  "name": "string",
                  "taxId": "string or null",
                  "address": "string or null"
                }
              }
            }
            
            Focus only on the header information - do NOT extract line items or totals. If you cannot determine a specific field, provide a reasonable default or null value. Make sure the response is valid JSON.
        """.trimIndent()
    }

    /**
     * Gets the system prompt for extracting line items and summary information
     */
    private fun getItemsPrompt(): String {
        return """
            Extract ONLY the line items, summary and additional information from the invoice and respond with JSON using the following structure:
            {
              "extractedInvoiceItems": {
                "items": [
                  {
                    "name": "string",
                    "description": "string or null",
                    "quantity": number,
                    "unitPrice": number,
                    "taxRate": number,
                    "totalNet": number,
                    "totalGross": number
                  }
                ],
                "summary": {
                  "totalNet": number,
                  "totalTax": number,
                  "totalGross": number
                },
                "additionalInfo": {
                  "notes": "string or null",
                  "protocolNumber": "string or null",
                  "protocolId": "string or null"
                }
              }
            }
            
            Focus only on the line items and totals - do NOT extract header information like invoice number, buyer or seller details. If you cannot determine a specific field, provide a reasonable default or null value. Make sure the response is valid JSON.
        """.trimIndent()
    }
}