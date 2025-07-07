package com.carslab.crm.modules.employees.infrastructure.persistence.read

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.model.EmployeeDocumentType
import com.carslab.crm.modules.employees.infrastructure.persistence.entity.EmployeeDocumentEntity
import com.carslab.crm.modules.employees.infrastructure.persistence.repository.*
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import java.time.format.DateTimeFormatter

@Repository
class JpaEmployeeDocumentReadRepositoryImpl(
    private val documentJpaRepository: EmployeeDocumentJpaRepository,
    private val employeeJpaRepository: com.carslab.crm.modules.employees.infrastructure.persistence.repository.EmployeeJpaRepository
) : EmployeeDocumentReadRepository {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findDocumentsByEmployee(
        employeeId: String,
        companyId: Long,
        type: EmployeeDocumentType?,
        isConfidential: Boolean?,
        searchQuery: String?
    ): List<EmployeeDocumentReadModel> {

        val specification = Specification<EmployeeDocumentEntity> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<String>("employeeId"), employeeId))
            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            type?.let { predicates.add(cb.equal(root.get<EmployeeDocumentType>("type"), it)) }
            isConfidential?.let { predicates.add(cb.equal(root.get<Boolean>("isConfidential"), it)) }

            searchQuery?.let { search ->
                val searchPattern = "%${search.lowercase()}%"
                val nameSearch = cb.like(cb.lower(root.get("name")), searchPattern)
                val descriptionSearch = cb.like(cb.lower(root.get("description")), searchPattern)
                val fileNameSearch = cb.like(cb.lower(root.get("originalFileName")), searchPattern)
                predicates.add(cb.or(nameSearch, descriptionSearch, fileNameSearch))
            }

            cb.and(*predicates.toTypedArray())
        }

        val sort = Sort.by(Sort.Direction.DESC, "createdAt")
        return documentJpaRepository.findAll(specification, sort)
            .map { entity -> mapToReadModel(entity) }
    }

    override fun findById(documentId: String, companyId: Long): EmployeeDocumentReadModel? {
        return documentJpaRepository.findById(documentId)
            .filter { it.companyId == companyId }
            .map { mapToReadModel(it) }
            .orElse(null)
    }

    override fun findCompanyDocuments(
        companyId: Long,
        type: EmployeeDocumentType?,
        isConfidential: Boolean?,
        searchQuery: String?,
        sortBy: String,
        sortOrder: String,
        page: Int,
        size: Int
    ): PaginatedResponse<EmployeeDocumentReadModel> {

        val sort = if (sortOrder == "asc") {
            Sort.by(Sort.Direction.ASC, sortBy)
        } else {
            Sort.by(Sort.Direction.DESC, sortBy)
        }

        val pageable = PageRequest.of(page, size, sort)

        val specification = Specification<EmployeeDocumentEntity> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            type?.let { predicates.add(cb.equal(root.get<EmployeeDocumentType>("type"), it)) }
            isConfidential?.let { predicates.add(cb.equal(root.get<Boolean>("isConfidential"), it)) }

            searchQuery?.let { search ->
                val searchPattern = "%${search.lowercase()}%"
                val nameSearch = cb.like(cb.lower(root.get("name")), searchPattern)
                val descriptionSearch = cb.like(cb.lower(root.get("description")), searchPattern)
                val fileNameSearch = cb.like(cb.lower(root.get("originalFileName")), searchPattern)
                predicates.add(cb.or(nameSearch, descriptionSearch, fileNameSearch))
            }

            cb.and(*predicates.toTypedArray())
        }

        val documentPage = documentJpaRepository.findAll(specification, pageable)
        val documents = documentPage.content.map { mapToReadModel(it) }

        return PaginatedResponse(
            data = documents,
            page = page,
            size = size,
            totalItems = documentPage.totalElements,
            totalPages = documentPage.totalPages.toLong()
        )
    }

    override fun getDocumentStats(companyId: Long): EmployeeDocumentStatsReadModel {
        val totalDocuments = documentJpaRepository.countByCompanyId(companyId).toInt()
        val confidentialDocuments = documentJpaRepository.countByCompanyIdAndIsConfidential(companyId, true).toInt()

        val documentsPerType = EmployeeDocumentType.values().associateWith { type ->
            documentJpaRepository.countByCompanyIdAndType(companyId, type).toInt()
        }

        // Oblicz statystyki rozmiaru (w rzeczywistej implementacji użyj native query)
        val documents = documentJpaRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
        val totalSizeBytes = documents.sumOf { it.fileSize }
        val averageSizeBytes = if (documents.isNotEmpty()) totalSizeBytes / documents.size else 0L

        return EmployeeDocumentStatsReadModel(
            totalDocuments = totalDocuments,
            documentsPerType = documentsPerType,
            confidentialDocuments = confidentialDocuments,
            totalSizeBytes = totalSizeBytes,
            averageSizeBytes = averageSizeBytes
        )
    }

    private fun mapToReadModel(entity: EmployeeDocumentEntity): EmployeeDocumentReadModel {
        // Pobierz nazwę pracownika
        val employeeName = employeeJpaRepository.findById(entity.employeeId)
            .map { it.fullName }
            .orElse("Unknown Employee")

        // Parse tags from JSON
        val tags = parseTagsFromJson(entity.tagsJson)

        return EmployeeDocumentReadModel(
            id = entity.id,
            employeeId = entity.employeeId,
            employeeName = employeeName,
            name = entity.name,
            type = entity.type,
            originalFileName = entity.originalFileName,
            fileSize = entity.fileSize,
            mimeType = entity.mimeType,
            description = entity.description,
            tags = tags,
            isConfidential = entity.isConfidential,
            createdAt = entity.createdAt.format(dateTimeFormatter),
            updatedAt = entity.updatedAt.format(dateTimeFormatter),
            createdBy = entity.createdBy
        )
    }

    private fun parseTagsFromJson(json: String?): List<String> {
        return if (json.isNullOrBlank()) emptyList()
        else try {
            json.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}