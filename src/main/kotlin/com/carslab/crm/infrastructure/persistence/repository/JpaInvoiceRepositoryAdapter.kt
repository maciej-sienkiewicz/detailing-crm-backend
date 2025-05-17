package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.api.model.InvoiceFilterDTO
import com.carslab.crm.domain.model.view.finance.*
import com.carslab.crm.domain.port.InvoiceRepository
import com.carslab.crm.infrastructure.persistence.entity.InvoiceAttachmentEntity
import com.carslab.crm.infrastructure.persistence.entity.InvoiceEntity
import com.carslab.crm.infrastructure.persistence.entity.InvoiceItemEntity
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.infrastructure.persistence.repository.InvoiceJpaRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class JpaInvoiceRepositoryAdapter(
    private val invoiceJpaRepository: InvoiceJpaRepository
) : InvoiceRepository {

    @Transactional
    override fun save(invoice: Invoice): Invoice {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        val entity = if (invoiceJpaRepository.findByCompanyIdAndId(companyId, invoice.id.value).isPresent) {
            val existingEntity = invoiceJpaRepository.findByCompanyIdAndId(companyId, invoice.id.value).get()

            // Aktualizacja podstawowych pól
            existingEntity.title = invoice.title
            existingEntity.issuedDate = invoice.issuedDate
            existingEntity.dueDate = invoice.dueDate
            existingEntity.sellerName = invoice.sellerName
            existingEntity.sellerTaxId = invoice.sellerTaxId
            existingEntity.sellerAddress = invoice.sellerAddress
            existingEntity.buyerName = invoice.buyerName
            existingEntity.buyerTaxId = invoice.buyerTaxId
            existingEntity.buyerAddress = invoice.buyerAddress
            existingEntity.clientId = invoice.clientId?.value
            existingEntity.status = invoice.status
            existingEntity.type = invoice.type
            existingEntity.paymentMethod = invoice.paymentMethod
            existingEntity.totalNet = invoice.totalNet
            existingEntity.totalTax = invoice.totalTax
            existingEntity.totalGross = invoice.totalGross
            existingEntity.currency = invoice.currency
            existingEntity.notes = invoice.notes
            existingEntity.protocolId = invoice.protocolId
            existingEntity.protocolNumber = invoice.protocolNumber
            existingEntity.updatedAt = LocalDateTime.now()

            // Czyszczenie istniejących pozycji
            existingEntity.items.clear()

            existingEntity
        } else {
            val newEntity = InvoiceEntity.fromDomain(invoice)
            // Jeśli numer nie został wygenerowany, generujemy go
            if (newEntity.number.isBlank()) {
                newEntity.number = generateInvoiceNumber(invoice.issuedDate.year, invoice.issuedDate.month.value, invoice.type.name)
            }
            newEntity
        }

        // Zapisanie faktury aby uzyskać identyfikator (lub aktualizować istniejącą)
        val savedEntity = invoiceJpaRepository.save(entity)

        // Dodanie pozycji
        invoice.items.forEach { item ->
            val itemEntity = InvoiceItemEntity.fromDomain(item, savedEntity)
            savedEntity.items.add(itemEntity)
        }

        // Dodanie załącznika (jeśli istnieje)
        invoice.attachment?.let { attachment ->
            val attachmentEntity = InvoiceAttachmentEntity.fromDomain(attachment, savedEntity)
            savedEntity.attachment = attachmentEntity
        }

        // Zapisanie faktury z pozycjami i załącznikiem
        val finalEntity = invoiceJpaRepository.save(savedEntity)
        return finalEntity.toDomain()
    }

    override fun findById(id: InvoiceId): Invoice? {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return invoiceJpaRepository.findByCompanyIdAndId(companyId, id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findAll(filter: InvoiceFilterDTO?): List<Invoice> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId

        if (filter == null) {
            return invoiceJpaRepository.findByCompanyId(companyId)
                .map { it.toDomain() }
        }

        val status = filter.status?.let { try { InvoiceStatus.valueOf(it) } catch (e: IllegalArgumentException) { null } }
        val type = filter.type?.let { try { InvoiceType.valueOf(it) } catch (e: IllegalArgumentException) { null } }

        val results = invoiceJpaRepository.searchInvoicesAndCompanyId(
            number = filter.number,
            title = filter.title,
            buyerName = filter.buyerName,
            status = status,
            type = type,
            dateFrom = filter.dateFrom,
            dateTo = filter.dateTo,
            protocolId = filter.protocolId,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            companyId = companyId
        )

        return results.map { it.toDomain() }
    }

    @Transactional
    override fun deleteById(id: InvoiceId): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        val entity = invoiceJpaRepository.findByCompanyIdAndId(companyId, id.value).orElse(null) ?: return false
        invoiceJpaRepository.delete(entity)
        return true
    }

    @Transactional
    override fun updateStatus(id: InvoiceId, status: String): Boolean {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        try {
            val invoiceStatus = InvoiceStatus.valueOf(status)
            return invoiceJpaRepository.updateStatusAndCompanyId(id.value, invoiceStatus, companyId) > 0
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid status: $status")
        }
    }

    override fun generateInvoiceNumber(year: Int, month: Int, type: String): String {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        // Przykładowy format: FV/RRRR/XXXX dla przychodów, KSZ/RRRR/XXXX dla kosztów
        val prefix = when (type) {
            InvoiceType.INCOME.name -> "FV/$year/$month/"
            InvoiceType.EXPENSE.name -> "KSZ/$year/$month"
            else -> "FV/$year/$month/"
        }

        // Pobierz ostatni numer faktury z podanym prefixem dla danej firmy
        val lastNumber = invoiceJpaRepository.findLastNumberByPrefixAndCompanyId(prefix, companyId)

        // Jeśli nie ma jeszcze faktur z tym prefixem, rozpocznij od 1
        if (lastNumber == null) {
            return "${prefix}0001"
        }

        // Wyciągnij numer z ostatniej faktury i zwiększ o 1
        val lastNumberValue = lastNumber.substring(prefix.length).toIntOrNull() ?: 0
        val newNumberValue = lastNumberValue + 1

        // Formatuj numer z wiodącymi zerami (np. 0001, 0012, 0123)
        return "$prefix${newNumberValue.toString().padStart(4, '0')}"
    }

    override fun findOverdueBefore(date: LocalDate): List<Invoice> {
        val companyId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
        return invoiceJpaRepository.findByDueDateBeforeAndCompanyId(date, companyId)
            .filter { it.status == InvoiceStatus.NOT_PAID || it.status == InvoiceStatus.PARTIALLY_PAID }
            .map { it.toDomain() }
    }
}