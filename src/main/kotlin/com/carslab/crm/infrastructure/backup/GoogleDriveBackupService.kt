package com.carslab.crm.infrastructure.backup

import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.infrastructure.storage.UniversalStorageService
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveClientFactory
import com.carslab.crm.infrastructure.backup.googledrive.GoogleDriveFileUploader
import com.carslab.crm.modules.invoice_templates.domain.InvoiceTemplateService
import com.carslab.crm.modules.invoice_templates.domain.model.InvoiceGenerationData
import com.carslab.crm.modules.company_settings.domain.CompanySettingsDomainService
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
    private val googleDriveFileUploader: GoogleDriveFileUploader,
    private val invoiceTemplateService: InvoiceTemplateService,
    private val companySettingsService: CompanySettingsDomainService
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

            val driveService = googleDriveClientFactory.createDriveService(companyId)
            var successCount = 0
            var errorCount = 0

            invoices.forEach { document ->
                try {
                    val (fileData, fileName) = prepareInvoiceForBackup(document, companyId)
                    val folderPath = generateDriveFolderPath(document.issuedDate, document.direction)

                    val uploadResult = googleDriveFileUploader.uploadFile(
                        driveService = driveService,
                        fileData = fileData,
                        fileName = fileName,
                        folderPath = folderPath,
                        mimeType = "application/pdf",
                        metadata = mapOf(
                            "documentId" to document.id.value,
                            "documentNumber" to (document.number),
                            "issueDate" to document.issuedDate.toString(),
                            "companyId" to companyId.toString(),
                            "generatedFromTemplate" to if (document.attachment == null) "true" else "false"
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

    /**
     * Przygotowuje fakturę do backup - używa załącznika lub generuje z szablonu
     */
    private fun prepareInvoiceForBackup(document: UnifiedFinancialDocument, companyId: Long): Pair<ByteArray, String> {
        val fileName = generateFileName(document)

        // Sprawdź czy dokument ma załącznik
        val attachment = document.attachment
        if (attachment != null) {
            logger.debug("Using existing attachment for document: {}", document.id.value)

            val fileData = universalStorageService.retrieveFile(attachment.storageId)
            if (fileData != null) {
                return Pair(fileData, fileName)
            } else {
                logger.warn("Could not retrieve attachment for document {}, generating from template", document.id.value)
            }
        }

        // Generuj PDF z szablonu
        logger.debug("Generating PDF from template for document: {}", document.id.value)
        return try {
            val companySettings = companySettingsService.getCompanySettings(companyId)
                ?: throw IllegalStateException("Company settings not found")

            val generationData = InvoiceGenerationData(
                document = document,
                companySettings = companySettings,
                logoData = null
            )

            val pdfBytes = invoiceTemplateService.generateInvoicePdf(
                companyId = companyId,
                invoiceData = generationData
            )

            Pair(pdfBytes, fileName)
        } catch (e: Exception) {
            logger.error("Failed to generate PDF from template for document: {}", document.id.value, e)
            throw RuntimeException("Could not prepare invoice for backup", e)
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

        val documentNumber = document.number.replace("/", "_").replace(" ", "_")
        val date = document.issuedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        return "${prefix}_${documentNumber}_${date}.pdf"
    }
}