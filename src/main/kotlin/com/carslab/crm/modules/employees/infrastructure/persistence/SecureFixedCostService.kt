package com.carslab.crm.modules.employees.infrastructure.persistence

import com.carslab.crm.finances.domain.model.fixedcosts.*
import com.carslab.crm.finances.infrastructure.entity.FixedCostEntity
import com.carslab.crm.finances.infrastructure.repository.fixedcosts.FixedCostJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class SecureFixedCostService(
    private val fixedCostJpaRepository: FixedCostJpaRepository
) {

    private val logger = LoggerFactory.getLogger(SecureFixedCostService::class.java)

    fun createFixedCostForCompany(fixedCost: FixedCost, companyId: Long): FixedCost {
        logger.debug("Creating fixed cost for company: $companyId")

        try {
            val entity = FixedCostEntity.fromDomain(fixedCost, companyId)

            val savedEntity = fixedCostJpaRepository.save(entity)
            logger.debug("Successfully created fixed cost ${savedEntity.id} for company $companyId")

            return savedEntity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to create fixed cost for company $companyId", e)
            throw e
        }
    }

    fun updateFixedCostForCompany(fixedCost: FixedCost, companyId: Long): FixedCost {
        logger.debug("Updating fixed cost ${fixedCost.id.value} for company: $companyId")

        try {
            val existingEntity = fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCost.id.value)
                .orElseThrow {
                    IllegalArgumentException("Fixed cost ${fixedCost.id.value} not found for company $companyId")
                }

            existingEntity.apply {
                name = fixedCost.name
                description = fixedCost.description
                category = fixedCost.category
                monthlyAmount = fixedCost.monthlyAmount
                frequency = fixedCost.frequency
                startDate = fixedCost.startDate
                endDate = fixedCost.endDate
                status = fixedCost.status
                autoRenew = fixedCost.autoRenew
                supplierName = fixedCost.supplierInfo?.name
                supplierTaxId = fixedCost.supplierInfo?.taxId
                contractNumber = fixedCost.contractNumber
                notes = fixedCost.notes
                updatedAt = LocalDateTime.now()
            }

            val savedEntity = fixedCostJpaRepository.save(existingEntity)
            logger.debug("Successfully updated fixed cost ${savedEntity.id} for company $companyId")

            return savedEntity.toDomain()
        } catch (e: Exception) {
            logger.error("Failed to update fixed cost ${fixedCost.id.value} for company $companyId", e)
            throw e
        }
    }

    fun findSalaryFixedCostsForEmployee(employeeId: String, companyId: Long): List<FixedCost> {
        logger.debug("Finding salary fixed costs for employee $employeeId in company $companyId")

        try {
            val entities = fixedCostJpaRepository.findByCompanyIdAndCategory(companyId, FixedCostCategory.PERSONNEL)
                .filter { entity ->
                    entity.status == FixedCostStatus.ACTIVE &&
                            entity.name.contains("Wynagrodzenie") == true
                }

            logger.debug("Found ${entities.size} salary fixed costs for employee $employeeId in company $companyId")
            return entities.map { it.toDomain() }
        } catch (e: Exception) {
            logger.error("Failed to find salary fixed costs for employee $employeeId in company $companyId", e)
            return emptyList()
        }
    }

    fun deactivateFixedCost(fixedCostId: FixedCostId, companyId: Long, reason: String) {
        logger.debug("Deactivating fixed cost ${fixedCostId.value} for company $companyId")

        try {
            val existingEntity = fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCostId.value)
                .orElseThrow {
                    IllegalArgumentException("Fixed cost ${fixedCostId.value} not found for company $companyId")
                }

            existingEntity.apply {
                status = FixedCostStatus.INACTIVE
                endDate = java.time.LocalDate.now()
                notes = "${notes ?: ""}\n\nDezaktywowano: ${java.time.LocalDate.now()} - $reason".trim()
                updatedAt = LocalDateTime.now()
            }

            fixedCostJpaRepository.save(existingEntity)
            logger.info("Deactivated fixed cost ${fixedCostId.value} for company $companyId: $reason")
        } catch (e: Exception) {
            logger.error("Failed to deactivate fixed cost ${fixedCostId.value} for company $companyId", e)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun findFixedCostForCompany(fixedCostId: FixedCostId, companyId: Long): FixedCost? {
        return try {
            fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCostId.value)
                .map { it.toDomain() }
                .orElse(null)
        } catch (e: Exception) {
            logger.error("Failed to find fixed cost ${fixedCostId.value} for company $companyId", e)
            null
        }
    }

    @Transactional(readOnly = true)
    fun existsForCompany(fixedCostId: FixedCostId, companyId: Long): Boolean {
        return try {
            fixedCostJpaRepository.findByCompanyIdAndId(companyId, fixedCostId.value).isPresent
        } catch (e: Exception) {
            logger.error("Failed to check existence of fixed cost ${fixedCostId.value} for company $companyId", e)
            false
        }
    }
}