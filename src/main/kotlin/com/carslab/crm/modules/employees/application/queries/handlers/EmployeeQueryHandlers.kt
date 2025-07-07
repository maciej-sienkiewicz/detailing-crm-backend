// src/main/kotlin/com/carslab/crm/modules/employees/application/queries/handlers/EmployeeQueryHandlers.kt
package com.carslab.crm.modules.employees.application.queries.handlers

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.infrastructure.persistence.read.EmployeeReadRepository
import com.carslab.crm.modules.employees.infrastructure.export.EmployeeExportService
import com.carslab.crm.infrastructure.cqrs.QueryHandler
import com.carslab.crm.api.model.response.PaginatedResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetEmployeesQueryHandler(
    private val employeeReadRepository: EmployeeReadRepository
) : QueryHandler<GetEmployeesQuery, PaginatedResponse<EmployeeReadModel>> {

    private val logger = LoggerFactory.getLogger(GetEmployeesQueryHandler::class.java)

    override fun handle(query: GetEmployeesQuery): PaginatedResponse<EmployeeReadModel> {
        logger.debug("Fetching employees with filters: role=${query.role}, isActive=${query.isActive}")

        return employeeReadRepository.findEmployees(
            searchQuery = query.searchQuery,
            position = query.position,
            role = query.role,
            isActive = query.isActive,
            contractType = query.contractType,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            page = query.page,
            size = query.size
        )
    }
}

@Service
class GetEmployeeByIdQueryHandler(
    private val employeeReadRepository: EmployeeReadRepository
) : QueryHandler<GetEmployeeByIdQuery, EmployeeDetailReadModel?> {

    private val logger = LoggerFactory.getLogger(GetEmployeeByIdQueryHandler::class.java)

    override fun handle(query: GetEmployeeByIdQuery): EmployeeDetailReadModel? {
        logger.debug("Fetching employee by ID: ${query.employeeId}")
        return employeeReadRepository.findById(query.employeeId)
    }
}

class GetEmployeeDocumentsQueryHandler(
    private val employeeReadRepository: EmployeeReadRepository
) : QueryHandler<GetEmployeeDocumentsQuery, List<EmployeeDocumentReadModel>> {

    private val logger = LoggerFactory.getLogger(GetEmployeeDocumentsQueryHandler::class.java)

    override fun handle(query: GetEmployeeDocumentsQuery): List<EmployeeDocumentReadModel> {
        logger.debug("Fetching documents for employee: ${query.employeeId}")
        return employeeReadRepository.findDocumentsByEmployeeId(query.employeeId)
    }
}

@Service
class GetEmployeeStatisticsQueryHandler(
    private val employeeReadRepository: EmployeeReadRepository
) : QueryHandler<GetEmployeeStatisticsQuery, EmployeeStatisticsReadModel> {

    private val logger = LoggerFactory.getLogger(GetEmployeeStatisticsQueryHandler::class.java)

    override fun handle(query: GetEmployeeStatisticsQuery): EmployeeStatisticsReadModel {
        logger.debug("Generating employee statistics")
        return employeeReadRepository.getStatistics()
    }
}

@Service
class ExportEmployeesQueryHandler(
    private val employeeReadRepository: EmployeeReadRepository,
    private val employeeExportService: EmployeeExportService
) : QueryHandler<ExportEmployeesQuery, ByteArray> {

    private val logger = LoggerFactory.getLogger(ExportEmployeesQueryHandler::class.java)

    override fun handle(query: ExportEmployeesQuery): ByteArray {
        logger.info("Exporting employees in format: ${query.format}")

        val employees = employeeReadRepository.findEmployeesForExport(
            searchQuery = query.searchQuery,
            position = query.position,
            role = query.role,
            isActive = query.isActive,
            contractType = query.contractType
        )

        return when (query.format.lowercase()) {
            "xlsx" -> employeeExportService.exportToExcel(employees)
            "pdf" -> employeeExportService.exportToPdf(employees)
            else -> employeeExportService.exportToCsv(employees)
        }
    }
}