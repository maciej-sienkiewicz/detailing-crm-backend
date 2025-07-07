package com.carslab.crm.modules.employees.application.commands.models

import com.carslab.crm.infrastructure.cqrs.Command
import com.carslab.crm.modules.employees.domain.model.*
import org.springframework.web.multipart.MultipartFile

data class UploadEmployeeDocumentCommand(
    val file: MultipartFile,
    val employeeId: String,
    val name: String,
    val type: EmployeeDocumentType,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val isConfidential: Boolean = false
) : Command<String>

data class UpdateEmployeeDocumentCommand(
    val documentId: String,
    val name: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val isConfidential: Boolean? = null
) : Command<String>

data class DeleteEmployeeDocumentCommand(
    val documentId: String
) : Command<Boolean>