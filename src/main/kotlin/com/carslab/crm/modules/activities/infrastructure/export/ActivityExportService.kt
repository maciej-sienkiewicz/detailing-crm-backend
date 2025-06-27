// src/main/kotlin/com/carslab/crm/modules/activities/infrastructure/export/ActivityExportService.kt
package com.carslab.crm.modules.activities.infrastructure.export

import com.carslab.crm.modules.activities.application.queries.models.ActivityReadModel
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter

@Service
class ActivityExportService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Export activities to CSV format
     */
    fun exportToCsv(activities: List<ActivityReadModel>): ByteArray {
        val csvContent = StringBuilder()

        // Headers
        csvContent.append("ID,Timestamp,Category,Message,User,Status,Entity Type,Entity ID\n")

        // Data rows
        activities.forEach { activity ->
            csvContent.append("\"${activity.id}\",")
            csvContent.append("\"${activity.timestamp.format(dateTimeFormatter)}\",")
            csvContent.append("\"${activity.category.name}\",")
            csvContent.append("\"${escapeCsv(activity.message)}\",")
            csvContent.append("\"${activity.userName ?: ""}\",")
            csvContent.append("\"${activity.status?.name ?: ""}\",")
            csvContent.append("\"${activity.entityType?.name ?: ""}\",")
            csvContent.append("\"${activity.entityId ?: ""}\"\n")
        }

        return csvContent.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Export activities to Excel format
     */
    fun exportToExcel(activities: List<ActivityReadModel>): ByteArray {
        return ByteArray(0)
    }

    /**
     * Export activities to PDF format
     */
    fun exportToPdf(activities: List<ActivityReadModel>): ByteArray {
        // For simplicity, creating a basic PDF structure
        // In production, you might want to use libraries like iText or Flying Saucer

        val pdfContent = StringBuilder()
        pdfContent.append("Activity Report\n")
        pdfContent.append("Generated: ${java.time.LocalDateTime.now().format(dateTimeFormatter)}\n\n")

        activities.forEach { activity ->
            pdfContent.append("ID: ${activity.id}\n")
            pdfContent.append("Time: ${activity.timestamp.format(dateTimeFormatter)}\n")
            pdfContent.append("Category: ${activity.category.name}\n")
            pdfContent.append("Message: ${activity.message}\n")
            if (activity.userName != null) {
                pdfContent.append("User: ${activity.userName}\n")
            }
            if (activity.status != null) {
                pdfContent.append("Status: ${activity.status.name}\n")
            }
            if (activity.entityType != null && activity.entityId != null) {
                pdfContent.append("Entity: ${activity.entityType.name} (${activity.entityId})\n")
            }
            pdfContent.append("---\n\n")
        }

        // For this example, we'll return plain text as bytes
        // In production, use a proper PDF library
        return pdfContent.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}