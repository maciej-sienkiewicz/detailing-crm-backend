package com.carslab.crm.production.modules.visits.domain.service

import com.carslab.crm.api.model.DocumentItemDTO
import com.carslab.crm.api.model.request.CreateUnifiedDocumentRequest
import com.carslab.crm.finances.domain.UnifiedDocumentService
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.visits.api.commands.ReleaseVehicleRequest
import com.carslab.crm.production.modules.clients.application.service.ClientQueryService
import com.carslab.crm.production.modules.clients.application.service.ClientStatisticsCommandService
import com.carslab.crm.production.modules.visits.domain.command.*
import com.carslab.crm.production.modules.clients.domain.model.ClientId
import com.carslab.crm.production.modules.companysettings.application.service.CompanyDetailsFetchService
import com.carslab.crm.production.modules.vehicles.application.service.VehicleQueryService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.visits.application.dto.AddCommentRequest
import com.carslab.crm.production.modules.visits.application.service.command.VisitCommentCommandService
import com.carslab.crm.production.modules.visits.application.service.query.VisitDetailQueryService
import com.carslab.crm.production.modules.visits.domain.models.aggregates.Visit
import com.carslab.crm.production.modules.visits.domain.models.entities.VisitService
import com.carslab.crm.production.modules.visits.domain.models.enums.VisitStatus
import com.carslab.crm.production.modules.visits.domain.models.value_objects.ServiceDiscount
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitDocuments
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitPeriod
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.modules.visits.domain.service.activity.VisitActivitySender
import com.carslab.crm.production.modules.visits.infrastructure.utils.CalculationUtils
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class VisitDomainService(
    private val visitRepository: VisitRepository,
    private val clientStatisticsCommandService: ClientStatisticsCommandService,
    private val clientQueryService: ClientQueryService,
    private val vehicleQueryService: VehicleQueryService,
    private val companyDetailsFetchService: CompanyDetailsFetchService,
    private val visitDetailQueryService: VisitDetailQueryService,
    private val visitActivitySender: VisitActivitySender,
    private val unifiedDocumentService: UnifiedDocumentService,
    private val securityContext: SecurityContext,
    private val visitCommentCommandService: VisitCommentCommandService,
) {
    fun createVisit(command: CreateVisitCommand): Visit {
        validateCreateCommand(command)
        
        val visit = Visit(
            id = null,
            companyId = command.companyId,
            title = command.title.trim(),
            clientId = ClientId(command.client.id.toLong()),
            vehicleId = VehicleId(command.vehicle.id.toLong()),
            period = VisitPeriod(command.startDate, command.endDate),
            status = command.status,
            services = command.services.map { createVisitService(it) },
            documents = VisitDocuments(command.keysProvided, command.documentsProvided),
            notes = command.notes?.trim(),
            referralSource = command.referralSource,
            appointmentId = command.appointmentId,
            calendarColorId = command.calendarColorId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            deliveryPerson = command.deliveryPerson,
        )

        return visitRepository.save(visit)
            .also { clientStatisticsCommandService.recordVisit(visit.clientId.value.toString()) }
            .also { visitActivitySender.onVisitCreated(visit, command.client, command.vehicle) }
    }

    fun updateVisit(visitId: VisitId, command: UpdateVisitCommand, companyId: Long): Visit {
        val existingVisit = getVisitForCompany(visitId, companyId)
        validateUpdateCommand(command, existingVisit)

        val client = clientQueryService.getClient(existingVisit.clientId.value.toString())
        val vehicle = vehicleQueryService.getVehicle(existingVisit.vehicleId.value.toString())

        val updatedVisit = existingVisit.copy(
            title = command.title.trim(),
            period = VisitPeriod(command.startDate, command.endDate),
            services = command.services.map { updateVisitService(it) },
            documents = VisitDocuments(command.keysProvided, command.documentsProvided),
            notes = command.notes?.trim(),
            referralSource = command.referralSource,
            appointmentId = command.appointmentId,
            calendarColorId = command.calendarColorId,
            updatedAt = LocalDateTime.now(),
            status = command.status,
            deliveryPerson = command.deliveryPerson,
        )

        return visitRepository.save(updatedVisit)
            .also { visitActivitySender.onVisitUpdated(it, existingVisit, client.client, vehicle.vehicle) }
            .also {
                if (command.deliveryPerson != null && existingVisit.deliveryPerson == null) {
                    visitCommentCommandService.addComment(
                        AddCommentRequest(
                            visitId = it.id!!.value.toString(),
                            type = "SYSTEM",
                            content = "Pojazd dostarczył/a: ${command.deliveryPerson.name}, tel: ${command.deliveryPerson.phone}"
                        )
                    )
                }
            }
    }

    fun changeVisitStatus(command: ChangeVisitStatusCommand): Visit {
        val visit = getVisitForCompany(command.visitId, command.companyId)
        val updatedVisit = visit.changeStatus(command.newStatus)
        return visitRepository.save(updatedVisit)
    }

    fun deleteVisit(visitId: VisitId, companyId: Long): Boolean {
        val visit = getVisitForCompany(visitId, companyId)

        if (visit.status == VisitStatus.IN_PROGRESS) {
            throw BusinessException("Cannot delete visit that is in progress")
        }

        return visitRepository.deleteById(visitId, companyId)
    }

    fun getVisitForCompany(visitId: VisitId, companyId: Long): Visit {
        return visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")
    }

    fun getVisitsForCompany(companyId: Long, pageable: Pageable): Page<Visit> {
        return visitRepository.findByCompanyId(companyId, pageable)
    }

    fun getVisitCountByStatus(companyId: Long, status: VisitStatus): Long {
        return visitRepository.countByStatus(companyId, status)
    }

    fun getVisitsForClient(clientId: ClientId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitRepository.findByClientId(clientId, companyId, pageable)
    }

    fun getVisitsForVehicle(vehicleId: VehicleId, companyId: Long, pageable: Pageable): Page<Visit> {
        return visitRepository.findByVehicleId(vehicleId, companyId, pageable)
    }

    private fun validateCreateCommand(command: CreateVisitCommand) {
        if (command.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
        if (command.startDate.isAfter(command.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }
        if (command.calendarColorId.isBlank()) {
            throw BusinessException("Calendar color ID cannot be blank")
        }
    }

    private fun validateUpdateCommand(command: UpdateVisitCommand, existingVisit: Visit) {
        if (command.title.isBlank()) {
            throw BusinessException("Visit title cannot be blank")
        }
        if (command.startDate.isAfter(command.endDate)) {
            throw BusinessException("Start date cannot be after end date")
        }
        if (existingVisit.status == VisitStatus.COMPLETED) {
            throw BusinessException("Cannot update completed visit")
        }
    }

    private fun createVisitService(command: CreateServiceCommand): VisitService {
        val discount = if (command.discountType != null && command.discountValue != null) {
            ServiceDiscount(command.discountType, command.discountValue)
        } else null

        val basePrice = command.basePrice
        val quantity = BigDecimal.valueOf(command.quantity)
        val totalBase = basePrice.multiply(quantity)

        val finalPrice = command.finalPrice ?: discount?.applyTo(totalBase) ?: totalBase

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = basePrice,
            quantity = command.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    private fun updateVisitService(command: UpdateServiceCommand): VisitService {
        val discount = if (command.discountType != null && command.discountValue != null) {
            ServiceDiscount(command.discountType, command.discountValue)
        } else null

        val basePrice = command.basePrice
        val quantity = BigDecimal.valueOf(command.quantity)
        val totalBase = basePrice.multiply(quantity)

        val finalPrice = command.finalPrice ?: discount?.applyTo(totalBase) ?: totalBase

        return VisitService(
            id = command.id,
            name = command.name,
            basePrice = basePrice,
            quantity = command.quantity,
            discount = discount,
            finalPrice = finalPrice,
            approvalStatus = command.approvalStatus,
            note = command.note
        )
    }

    fun releaseVehicle(visitId: String, request: ReleaseVehicleRequest) {
        val companyDetails = companyDetailsFetchService.getCompanySettings(
            companyId = securityContext.getCurrentCompanyId()
        )

        val visit = visitDetailQueryService.getVisitDetail(visitId)

        val items = if (!request.overridenItems.isNullOrEmpty()) {
            request.overridenItems.map { item ->
                val finalPrice = CalculationUtils.anyToBigDecimal(item.finalPrice ?: item.price)
                val quantity = CalculationUtils.anyToBigDecimal(item.quantity)
                val totalGross = finalPrice.multiply(quantity)
                val totalNet = CalculationUtils.calculateNetAmount(totalGross)

                DocumentItemDTO(
                    id = null,
                    name = item.name,
                    description = null,
                    quantity = quantity,
                    unitPrice = finalPrice,
                    taxRate = BigDecimal("23"),
                    totalNet = totalNet,
                    totalGross = totalGross
                )
            }
        } else {
            visit.selectedServices.map { service ->
                val finalPrice = CalculationUtils.anyToBigDecimal(service.finalPrice)
                val totalNet = CalculationUtils.calculateNetAmount(finalPrice)

                DocumentItemDTO(
                    id = null,
                    name = service.name,
                    description = service.note,
                    quantity = BigDecimal.ONE,
                    unitPrice = finalPrice,
                    taxRate = BigDecimal("23"),
                    totalNet = totalNet,
                    totalGross = finalPrice
                )
            }
        }

        val existingVisit = getVisitForCompany(VisitId(visitId.toLong()), securityContext.getCurrentCompanyId())
        visitRepository.save(existingVisit.copy(status = VisitStatus.COMPLETED))
        
        val totalGross = items.sumOf { it.totalGross }
        val totalNet = items.sumOf { it.totalNet }
        val totalTax = CalculationUtils.calculateVatFromGrossNet(totalGross, totalNet)
        
        
        unifiedDocumentService.createDocument(
            request = CreateUnifiedDocumentRequest(
                type = request.documentType,
                title = "Opłata za wizytę - ${visit.title}",
                description = request.additionalNotes,
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(request.paymentDays),
                sellerName = companyDetails.basicInfo?.companyName ?: "",
                sellerTaxId = companyDetails.basicInfo?.taxId ?: "",
                sellerAddress = companyDetails.basicInfo?.address ?: "",
                buyerName = visit.ownerName,
                buyerTaxId = visit.taxId,
                buyerAddress = visit.address,
                status = "NOT_PAID",
                direction = "INCOME",
                paymentMethod = request.paymentMethod,
                totalNet = totalNet,
                totalTax = totalTax,
                totalGross = totalGross,
                paidAmount = BigDecimal.ZERO,
                currency = "PLN",
                items = items,
                notes = request.additionalNotes ?: "",
                protocolId = null,
                protocolNumber = null,
                visitId = visitId,
            ),
            attachmentFile = null
        )
    }
}