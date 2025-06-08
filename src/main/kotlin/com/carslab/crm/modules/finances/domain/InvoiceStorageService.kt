package com.carslab.crm.finances.domain

import com.carslab.crm.domain.model.view.finance.InvoiceId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

/**
 * Serwis do przechowywania plików faktur.
 */
@Service
class InvoiceStorageService(
    @Value("\${file.upload-dir:uploads/invoices}")
    private val invoicesDir: String
) {
    private val logger = LoggerFactory.getLogger(InvoiceStorageService::class.java)
    private val rootLocation: Path = Paths.get(invoicesDir)

    init {
        try {
            Files.createDirectories(rootLocation)
            logger.info("Invoice storage directory initialized at: {}", rootLocation.toAbsolutePath())
        } catch (e: IOException) {
            logger.error("Could not initialize invoice storage", e)
            throw RuntimeException("Could not initialize invoice storage location", e)
        }
    }

    /**
     * Przechowuje plik faktury i zwraca identyfikator przechowywania.
     */
    fun storeInvoiceFile(file: MultipartFile, invoiceId: InvoiceId): String {
        try {
            if (file.isEmpty) {
                throw RuntimeException("Failed to store empty file")
            }

            // Tworzenie unikalnego identyfikatora dla pliku
            val storageId = UUID.randomUUID().toString()

            // Tworzenie katalogu dla faktury, jeśli nie istnieje
            val invoiceDir = rootLocation.resolve(invoiceId.value)
            Files.createDirectories(invoiceDir)

            // Przechowywanie pliku z wygenerowanym ID jako nazwą pliku (zachowując oryginalne rozszerzenie)
            val originalFilename = file.originalFilename ?: "unknown"
            val extension = originalFilename.substringAfterLast('.', "")
            val filename = "$storageId.$extension"
            val targetPath = invoiceDir.resolve(filename)

            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            logger.info("Stored invoice file for invoice {}: {}", invoiceId.value, targetPath)
            return storageId
        } catch (e: Exception) {
            logger.error("Failed to store invoice file", e)
            throw RuntimeException("Failed to store invoice file", e)
        }
    }

    /**
     * Pobiera plik faktury.
     */
    fun getInvoiceFile(storageId: String): ByteArray {
        try {
            // Znajdujemy plik na podstawie identyfikatora przechowywania
            val file = findInvoiceFileByStorageId(storageId)
            if (file != null) {
                return Files.readAllBytes(file)
            }
            throw RuntimeException("Invoice file not found: $storageId")
        } catch (e: Exception) {
            logger.error("Failed to read invoice file", e)
            throw RuntimeException("Failed to read invoice file", e)
        }
    }

    /**
     * Pobiera plik faktury jako zasób.
     */
    fun getInvoiceFileAsResource(storageId: String): Resource {
        try {
            val file = findInvoiceFileByStorageId(storageId)
                ?: throw RuntimeException("Invoice file not found: $storageId")

            val resource = UrlResource(file.toUri())
            if (resource.exists() || resource.isReadable) {
                return resource
            } else {
                throw RuntimeException("Could not read invoice file: $storageId")
            }
        } catch (e: MalformedURLException) {
            logger.error("Failed to read invoice file", e)
            throw RuntimeException("Could not read invoice file: $storageId", e)
        }
    }

    /**
     * Usuwa plik faktury.
     */
    fun deleteInvoiceFile(storageId: String): Boolean {
        try {
            val file = findInvoiceFileByStorageId(storageId)
            if (file != null) {
                return Files.deleteIfExists(file)
            }
            logger.warn("Invoice file not found for deletion: {}", storageId)
            return false
        } catch (e: Exception) {
            logger.error("Failed to delete invoice file", e)
            throw RuntimeException("Failed to delete invoice file", e)
        }
    }

    /**
     * Znajduje plik faktury na podstawie identyfikatora przechowywania.
     */
    private fun findInvoiceFileByStorageId(storageId: String): Path? {
        try {
            // Przeszukujemy wszystkie podkatalogi, aby znaleźć plik faktury
            val invoiceFiles = Files.walk(rootLocation)
                .filter { path ->
                    Files.isRegularFile(path) &&
                            path.fileName.toString().startsWith("$storageId.")
                }
                .toList()

            return if (invoiceFiles.isNotEmpty()) {
                invoiceFiles[0]
            } else {
                null
            }
        } catch (e: IOException) {
            logger.error("Failed to search for invoice file", e)
            return null
        }
    }

    /**
     * Usuwa wszystkie pliki faktur.
     */
    fun deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile())
        Files.createDirectories(rootLocation)
    }
}