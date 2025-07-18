// src/main/kotlin/com/carslab/crm/infrastructure/backup/GoogleDriveBackupService.kt
package com.carslab.crm.infrastructure.backup

import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveClientFactory
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveFileUploader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class GoogleDriveBackupService(
    private val documentRepository: UnifiedDocumentRepository,
    private val universalStorageService: UniversalStorageService,
    private val googleDriveClientFactory: GoogleDriveClientFactory,
    private val googleDriveFileUploader: GoogleDriveFileUploader
) {
    private val logger = LoggerFactory.getLogger(GoogleDriveBackupService::class.java)

    @Transactional(readOnly = true)
    fun backupCurrentMonthInvoicesToGoogleDrive(companyId: Long) {
        logger.info("Starting Google Drive backup for company: {} for current month", companyId)

        try {
            val currentMonth = YearMonth.now()
            val startDate = currentMonth.atDay(1)
            val endDate = currentMonth.atEndOfMonth()

            logger.debug("Backing up invoices from {} to {} for company {}", startDate, endDate, companyId)

            // Pobierz wszystkie faktury dla danego companyId w bieżącym miesiącu
            val invoices = documentRepository.findInvoicesByCompanyAndDateRange(
                companyId = companyId,
                startDate = startDate,
                endDate = endDate
            )

            if (invoices.isEmpty()) {
                logger.info("No invoices found for company {} in current month", companyId)
                return
            }

            logger.info("Found {} invoices to backup for company {}", invoices.size, companyId)

            // Inicjalizuj Google Drive client dla tego company
            val driveService = googleDriveClientFactory.createDriveService(companyId)

            var successCount = 0
            var errorCount = 0

            invoices.forEach { document ->
                try {
                    // Sprawdź czy dokument ma załącznik
                    val attachment = document.attachment
                    if (attachment == null) {
                        logger.warn("Document {} has no attachment, skipping", document.id.value)
                        return@forEach
                    }

                    // Pobierz plik z storage
                    val fileData = universalStorageService.retrieveFile(attachment.storageId)
                    if (fileData == null) {
                        logger.error("Failed to retrieve file for document {} from storage", document.id.value)
                        errorCount++
                        return@forEach
                    }

                    // Wygeneruj ścieżkę w Google Drive: faktury/rok/miesiac
                    val folderPath = generateDriveFolderPath(document.issuedDate, document.direction)

                    // Wygeneruj nazwę pliku
                    val fileName = generateFileName(document)

                    // Upload do Google Drive
                    val uploadResult = googleDriveFileUploader.uploadFile(
                        driveService = driveService,
                        fileData = fileData,
                        fileName = fileName,
                        folderPath = folderPath,
                        mimeType = attachment.type,
                        metadata = mapOf(
                            "documentId" to document.id.value,
                            "documentNumber" to (document.number ?: ""),
                            "issueDate" to document.issuedDate.toString(),
                            "companyId" to companyId.toString(),
                            "originalFileName" to attachment.name
                        )
                    )

                    if (uploadResult.success) {
                        logger.debug("Successfully uploaded document {} to Google Drive: {}",
                            document.id.value, uploadResult.fileId)
                        successCount++
                    } else {
                        logger.error("Failed to upload document {} to Google Drive: {}",
                            document.id.value, uploadResult.error)
                        errorCount++
                    }

                } catch (e: Exception) {
                    logger.error("Error processing document {} for Google Drive backup",
                        document.id.value, e)
                    errorCount++
                }
            }

            logger.info("Google Drive backup completed for company {}. Success: {}, Errors: {}",
                companyId, successCount, errorCount)

        } catch (e: Exception) {
            logger.error("Failed to backup invoices to Google Drive for company {}", companyId, e)
            throw RuntimeException("Google Drive backup failed for company $companyId", e)
        }
    }

    private fun generateDriveFolderPath(issuedDate: LocalDate, direction: TransactionDirection): String {
        val year = issuedDate.year
        val month = issuedDate.format(DateTimeFormatter.ofPattern("MM-MMMM"))
        val directionFolder = when (direction) {
            TransactionDirection.INCOME -> "przychodzace"
            TransactionDirection.EXPENSE -> "wychodzace"
        }

        return "faktury/$year/$month/$directionFolder"
    }

    private fun generateFileName(document: UnifiedFinancialDocument): String {
        val prefix = when (document.direction) {
            TransactionDirection.INCOME -> "FAKT_PRZYCH"
            TransactionDirection.EXPENSE -> "FAKT_WYCH"
        }

        val documentNumber = document.number?.replace("/", "_")?.replace(" ", "_") ?: "BRAK_NUMERU"
        val date = document.issuedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        return "${prefix}_${documentNumber}_${date}.pdf"
    }
}