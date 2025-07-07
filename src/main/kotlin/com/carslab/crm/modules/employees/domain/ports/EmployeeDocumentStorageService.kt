package com.carslab.crm.modules.employees.domain.ports

import com.carslab.crm.modules.employees.domain.model.*
import org.springframework.web.multipart.MultipartFile

interface EmployeeDocumentStorageService {

    fun storeEmployeeDocument(
        file: MultipartFile,
        employeeId: EmployeeId,
        companyId: Long,
        documentType: String
    ): String

    fun retrieveEmployeeDocument(storageId: String): ByteArray?

    fun deleteEmployeeDocument(storageId: String): Boolean

    fun documentExists(storageId: String): Boolean
}