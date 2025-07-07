// src/main/kotlin/com/carslab/crm/modules/employees/infrastructure/persistence/read/JpaEmployeeReadRepositoryImpl.kt
package com.carslab.crm.modules.employees.infrastructure.persistence.read

import com.carslab.crm.modules.employees.application.queries.models.*
import com.carslab.crm.modules.employees.domain.model.*
import com.carslab.crm.modules.employees.infrastructure.persistence.entity.EmployeeEntity
import com.carslab.crm.modules.employees.infrastructure.persistence.repository.*
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import jakarta.persistence.criteria.Predicate
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@Repository
class JpaEmployeeReadRepositoryImpl(
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val documentJpaRepository: EmployeeDocumentJpaRepository,
    private val securityContext: SecurityContext
) : EmployeeReadRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findEmployees(
        searchQuery: String?,
        position: String?,
        role: UserRole?,
        isActive: Boolean?,
        contractType: ContractType?,
        sortBy: String,
        sortOrder: String,
        page: Int,
        size: Int
    ): PaginatedResponse<EmployeeReadModel> {
        val companyId = securityContext.getCurrentCompanyId()

        val sort = if (sortOrder == "asc") {
            Sort.by(Sort.Direction.ASC, sortBy)
        } else {
            Sort.by(Sort.Direction.DESC, sortBy)
        }

        val pageable = PageRequest.of(page, size, sort)

        val specification = Specification<EmployeeEntity> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            searchQuery?.let { search ->
                val searchPattern = "%${search.lowercase()}%"
                val nameSearch = cb.like(cb.lower(root.get("fullName")), searchPattern)
                val emailSearch = cb.like(cb.lower(root.get("email")), searchPattern)
                val phoneSearch = cb.like(cb.lower(root.get("phone")), searchPattern)
                val positionSearch = cb.like(cb.lower(root.get("position")), searchPattern)
                predicates.add(cb.or(nameSearch, emailSearch, phoneSearch, positionSearch))
            }

            position?.let {
                predicates.add(cb.like(cb.lower(root.get("position")), "%${it.lowercase()}%"))
            }

            role?.let {
                predicates.add(cb.equal(root.get<UserRole>("role"), it))
            }

            isActive?.let {
                predicates.add(cb.equal(root.get<Boolean>("isActive"), it))
            }

            contractType?.let {
                predicates.add(cb.equal(root.get<ContractType>("contractType"), it))
            }

            cb.and(*predicates.toTypedArray())
        }

        val employeePage = employeeJpaRepository.findAll(specification, pageable)
        val employees = employeePage.content.map { entity ->
            EmployeeReadModel(
                id = entity.id,
                fullName = entity.fullName,
                position = entity.position,
                email = entity.email,
                phone = entity.phone,
                role = entity.role,
                isActive = entity.isActive,
                hireDate = entity.hireDate.format(dateFormatter),
                lastLoginDate = null, // TODO: Implement if needed
                hourlyRate = entity.hourlyRate,
                bonusFromRevenue = entity.bonusFromRevenue,
                workingHoursPerWeek = entity.workingHoursPerWeek,
                contractType = entity.contractType
            )
        }

        return PaginatedResponse(
            data = employees,
            page = page,
            size = size,
            totalItems = employeePage.totalElements,
            totalPages = employeePage.totalPages.toLong()
        )
    }

    override fun findById(employeeId: String): EmployeeDetailReadModel? {
        val companyId = securityContext.getCurrentCompanyId()

        return employeeJpaRepository.findById(employeeId)
            .filter { it.companyId == companyId }
            .map { entity ->
                val employee = entity.toDomain()

                val emergencyContact = if (entity.emergencyContactName != null && entity.emergencyContactPhone != null) {
                    EmergencyContactReadModel(entity.emergencyContactName!!, entity.emergencyContactPhone!!)
                } else null

                EmployeeDetailReadModel(
                    id = entity.id,
                    fullName = entity.fullName,
                    birthDate = entity.birthDate.format(dateFormatter),
                    hireDate = entity.hireDate.format(dateFormatter),
                    position = entity.position,
                    email = entity.email,
                    phone = entity.phone,
                    role = entity.role,
                    hourlyRate = entity.hourlyRate,
                    bonusFromRevenue = entity.bonusFromRevenue,
                    isActive = entity.isActive,
                    workingHoursPerWeek = entity.workingHoursPerWeek,
                    contractType = entity.contractType,
                    emergencyContact = emergencyContact,
                    notes = entity.notes,
                    age = employee.calculateAge(),
                    tenureInMonths = employee.calculateTenureInMonths(),
                    createdAt = entity.createdAt.format(dateTimeFormatter),
                    updatedAt = entity.updatedAt.format(dateTimeFormatter)
                )
            }
            .orElse(null)
    }

    override fun findDocumentsByEmployeeId(employeeId: String): List<EmployeeDocumentReadModel> {
        return documentJpaRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
            .map { entity ->
                EmployeeDocumentReadModel(
                    id = entity.id,
                    employeeId = entity.employeeId,
                    name = entity.name,
                    type = entity.type,
                    uploadDate = entity.createdAt.format(dateTimeFormatter),
                    fileUrl = entity.fileUrl,
                    fileSize = entity.fileSize,
                    mimeType = entity.mimeType
                )
            }
    }

    override fun getStatistics(): EmployeeStatisticsReadModel {
        val companyId = securityContext.getCurrentCompanyId()

        val totalEmployees = employeeJpaRepository.countByCompanyId(companyId).toInt()
        val activeEmployees = employeeJpaRepository.countByCompanyIdAndIsActive(companyId, true).toInt()
        val inactiveEmployees = totalEmployees - activeEmployees

        val averageAge = employeeJpaRepository.getAverageAge(companyId) ?: 0.0
        val averageTenure = employeeJpaRepository.getAverageTenureInMonths(companyId) ?: 0.0

        val roleDistribution = try {
            employeeJpaRepository.getRoleDistribution(companyId)
                .associate { row ->
                    val roleStr = row[0] as String
                    val count = (row[1] as Number).toInt()
                    UserRole.valueOf(roleStr) to count
                }
        } catch (e: Exception) {
            // Fallback w przypadku błędu
            mapOf(
                UserRole.ADMIN to 0,
                UserRole.MANAGER to 0,
                UserRole.EMPLOYEE to 0
            )
        }

        val contractTypeDistribution = try {
            employeeJpaRepository.getContractTypeDistribution(companyId)
                .associate { row ->
                    val contractTypeStr = row[0] as String
                    val count = (row[1] as Number).toInt()
                    val contractType = when (contractTypeStr) {
                        "EMPLOYMENT" -> ContractType.EMPLOYMENT
                        "B2B" -> ContractType.B2B
                        "MANDATE" -> ContractType.MANDATE
                        else -> ContractType.EMPLOYMENT // Default for unknown
                    }
                    contractType to count
                }
        } catch (e: Exception) {
            // Fallback w przypadku błędu
            mapOf(
                ContractType.EMPLOYMENT to 0,
                ContractType.B2B to 0,
                ContractType.MANDATE to 0
            )
        }

        return EmployeeStatisticsReadModel(
            totalEmployees = totalEmployees,
            activeEmployees = activeEmployees,
            inactiveEmployees = inactiveEmployees,
            averageAge = averageAge,
            averageTenure = averageTenure,
            roleDistribution = roleDistribution,
            contractTypeDistribution = contractTypeDistribution
        )
    }

    override fun findEmployeesForExport(
        searchQuery: String?,
        position: String?,
        role: UserRole?,
        isActive: Boolean?,
        contractType: ContractType?
    ): List<EmployeeReadModel> {
        val companyId = securityContext.getCurrentCompanyId()

        val specification = Specification<EmployeeEntity> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            searchQuery?.let { search ->
                val searchPattern = "%${search.lowercase()}%"
                val nameSearch = cb.like(cb.lower(root.get("fullName")), searchPattern)
                val emailSearch = cb.like(cb.lower(root.get("email")), searchPattern)
                predicates.add(cb.or(nameSearch, emailSearch))
            }

            position?.let {
                predicates.add(cb.like(cb.lower(root.get("position")), "%${it.lowercase()}%"))
            }

            role?.let {
                predicates.add(cb.equal(root.get<UserRole>("role"), it))
            }

            isActive?.let {
                predicates.add(cb.equal(root.get<Boolean>("isActive"), it))
            }

            contractType?.let {
                predicates.add(cb.equal(root.get<ContractType>("contractType"), it))
            }

            cb.and(*predicates.toTypedArray())
        }

        val sort = Sort.by(Sort.Direction.ASC, "fullName")
        return employeeJpaRepository.findAll(specification, sort).map { entity ->
            EmployeeReadModel(
                id = entity.id,
                fullName = entity.fullName,
                position = entity.position,
                email = entity.email,
                phone = entity.phone,
                role = entity.role,
                isActive = entity.isActive,
                hireDate = entity.hireDate.format(dateFormatter),
                lastLoginDate = null,
                hourlyRate = entity.hourlyRate,
                bonusFromRevenue = entity.bonusFromRevenue,
                workingHoursPerWeek = entity.workingHoursPerWeek,
                contractType = entity.contractType
            )
        }
    }
}