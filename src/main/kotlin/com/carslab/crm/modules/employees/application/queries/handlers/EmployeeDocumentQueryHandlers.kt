package com.carslab.crm.modules.employees.application.queries.handlers

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.services.EmployeeDocumentDomainService
import com.carslab.crm.modules.employees.infrastructure.persistence.read.EmployeeDocumentReadRepository
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.api.model.response.PaginatedResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetEmployeeDocumentsQueryHandler(
    private val documentReadRepository: EmployeeDocumentReadRepository,
    private val securityContext: SecurityContext
) : QueryHandler<GetEmployeeDocumentsQuery, List<EmployeeDocumentReadModel>> {

    private val logger = LoggerFactory.getLogger(GetEmployeeDocumentsQueryHandler::class.java)

    override fun handle(query: GetEmployeeDocumentsQuery): List<EmployeeDocumentReadModel> {
        logger.debug("Fetching documents for employee: ${query.employeeId}")

        val companyId = securityContext.getCurrentCompanyId()

        return documentReadRepository.findDocumentsByEmployee(
            employeeId = query.employeeId,
            companyId = companyId,
            type = query.type,
            isConfidential = query.isConfidential,
            searchQuery = query.searchQuery
        )
    }
}

@Service
class GetEmployeeDocumentByIdQueryHandler(
    private val documentReadRepository: EmployeeDocumentReadRepository,
    private val securityContext: SecurityContext
) : QueryHandler<GetEmployeeDocumentByIdQuery, EmployeeDocumentReadModel?> {

    private val logger = LoggerFactory.getLogger(GetEmployeeDocumentByIdQueryHandler::class.java)

    override fun handle(query: GetEmployeeDocumentByIdQuery): EmployeeDocumentReadModel? {
        logger.debug("Fetching document by ID: ${query.documentId}")

        val companyId = securityContext.getCurrentCompanyId()

        return documentReadRepository.findById(query.documentId, companyId)
    }
}

@Service
class GetCompanyDocumentsQueryHandler(
    private val documentReadRepository: EmployeeDocumentReadRepository,
    private val securityContext: SecurityContext
) : QueryHandler<GetCompanyDocumentsQuery, PaginatedResponse<EmployeeDocumentReadModel>> {

    private val logger = LoggerFactory.getLogger(GetCompanyDocumentsQueryHandler::class.java)

    override fun handle(query: GetCompanyDocumentsQuery): PaginatedResponse<EmployeeDocumentReadModel> {
        logger.debug("Fetching company documents with filters")

        val companyId = securityContext.getCurrentCompanyId()

        return documentReadRepository.findCompanyDocuments(
            companyId = companyId,
            type = query.type,
            isConfidential = query.isConfidential,
            searchQuery = query.searchQuery,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            page = query.page,
            size = query.size
        )
    }
}

@Service
class DownloadEmployeeDocumentQueryHandler(
    private val documentDomainService: EmployeeDocumentDomainService,
    private val securityContext: SecurityContext
) : QueryHandler<DownloadEmployeeDocumentQuery, DocumentDownloadData?> {

    private val logger = LoggerFactory.getLogger(DownloadEmployeeDocumentQueryHandler::class.java)

    override fun handle(query: DownloadEmployeeDocumentQuery): DocumentDownloadData? {
        logger.debug("Processing download request for document: ${query.documentId}")

        val companyId = securityContext.getCurrentCompanyId()

        return try {
            val result = documentDomainService.downloadDocument(
                documentId = com.carslab.crm.modules.employees.domain.model.EmployeeDocumentId.of(query.documentId),
                companyId = companyId
            )

            result?.let { (document, data) ->
                DocumentDownloadData(
                    fileName = document.originalFileName,
                    contentType = document.mimeType,
                    data = data,
                    size = document.fileSize
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to download document: ${query.documentId}", e)
            null
        }
    }
}