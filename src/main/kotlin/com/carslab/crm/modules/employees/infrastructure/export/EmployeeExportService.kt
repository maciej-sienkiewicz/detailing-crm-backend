// src/main/kotlin/com/carslab/crm/modules/employees/infrastructure/export/EmployeeExportService.kt
package com.carslab.crm.modules.employees.infrastructure.export

import com.carslab.crm.modules.employees.application.queries.models.EmployeeReadModel
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class EmployeeExportService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun exportToCsv(employees: List<EmployeeReadModel>): ByteArray {
        val csvContent = StringBuilder()

        // Headers
        csvContent.append("ID,Full Name,Position,Email,Phone,Role,Status,Hire Date,Hourly Rate,Bonus %,Hours/Week,Contract Type\n")

        // Data rows
        employees.forEach { employee ->
            csvContent.append("\"${employee.id}\",")
            csvContent.append("\"${escapeCsv(employee.fullName)}\",")
            csvContent.append("\"${escapeCsv(employee.position)}\",")
            csvContent.append("\"${employee.email}\",")
            csvContent.append("\"${employee.phone}\",")
            csvContent.append("\"${employee.role.name}\",")
            csvContent.append("\"${if (employee.isActive) "Active" else "Inactive"}\",")
            csvContent.append("\"${employee.hireDate}\",")
            csvContent.append("\"${employee.hourlyRate ?: ""}\",")
            csvContent.append("\"${employee.bonusFromRevenue ?: ""}\",")
            csvContent.append("\"${employee.workingHoursPerWeek ?: ""}\",")
            csvContent.append("\"${employee.contractType?.name ?: ""}\"\n")
        }

        return csvContent.toString().toByteArray(Charsets.UTF_8)
    }

    fun exportToExcel(employees: List<EmployeeReadModel>): ByteArray {
        // For production use Apache POI
        // This is a simplified version
        return exportToCsv(employees) // Fallback to CSV for now
    }

    fun exportToPdf(employees: List<EmployeeReadModel>): ByteArray {
        // For production use iText or similar PDF library
        val pdfContent = StringBuilder()
        pdfContent.append("Employee Report\n")
        pdfContent.append("Generated: ${LocalDateTime.now().format(dateTimeFormatter)}\n\n")

        employees.forEach { employee ->
            pdfContent.append("Name: ${employee.fullName}\n")
            pdfContent.append("Position: ${employee.position}\n")
            pdfContent.append("Email: ${employee.email}\n")
            pdfContent.append("Phone: ${employee.phone}\n")
            pdfContent.append("Role: ${employee.role.name}\n")
            pdfContent.append("Status: ${if (employee.isActive) "Active" else "Inactive"}\n")
            pdfContent.append("Hire Date: ${employee.hireDate}\n")
            if (employee.hourlyRate != null) {
                pdfContent.append("Hourly Rate: ${employee.hourlyRate}\n")
            }
            if (employee.contractType != null) {
                pdfContent.append("Contract: ${employee.contractType.name}\n")
            }
            pdfContent.append("---\n\n")
        }

        return pdfContent.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}