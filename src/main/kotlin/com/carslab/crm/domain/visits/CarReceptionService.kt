package com.carslab.crm.domain.visits

import com.carslab.crm.api.model.DocumentStatus
import com.carslab.crm.api.model.TransactionDirection
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.domain.model.ApprovalStatus
import com.carslab.crm.domain.model.Audit
import com.carslab.crm.domain.model.AuditInfo
import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.clients.domain.model.Client
import com.carslab.crm.clients.domain.model.ClientId
import com.carslab.crm.domain.model.Documents
import com.carslab.crm.domain.model.MediaItem
import com.carslab.crm.domain.model.MediaType
import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolService
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.ReferralSource
import com.carslab.crm.domain.model.UserId
import com.carslab.crm.domain.model.VehicleDetails
import com.carslab.crm.clients.domain.model.VehicleId
import com.carslab.crm.domain.model.create.protocol.CreateMediaTypeModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolClientModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolVehicleModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import com.carslab.crm.domain.model.create.protocol.DocumentType
import com.carslab.crm.domain.model.create.protocol.PaymentMethod
import com.carslab.crm.domain.model.create.protocol.VehicleReleaseDetailsModel
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.model.stats.VehicleStats
import com.carslab.crm.domain.model.view.finance.CashTransaction
import com.carslab.crm.domain.model.view.finance.DocumentItem
import com.carslab.crm.domain.model.view.finance.TransactionId
import com.carslab.crm.domain.model.view.finance.TransactionType
import com.carslab.crm.domain.model.view.finance.UnifiedDocumentId
import com.carslab.crm.domain.model.view.finance.UnifiedFinancialDocument
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import com.carslab.crm.domain.port.CarReceptionRepository
import com.carslab.crm.finances.domain.ports.CashRepository
import com.carslab.crm.finances.domain.ports.InvoiceRepository
import com.carslab.crm.domain.port.ProtocolCommentsRepository
import com.carslab.crm.domain.port.ProtocolServicesRepository
import com.carslab.crm.finances.domain.ports.UnifiedDocumentRepository
import com.carslab.crm.clients.domain.port.VehicleRepository
import com.carslab.crm.clients.domain.port.VehicleStatisticsRepository
import com.carslab.crm.domain.utils.EnhancedChangeTracker
import com.carslab.crm.domain.utils.formatUserFriendlyEnhanced
import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import com.carslab.crm.infrastructure.storage.FileImageStorageService
import com.carslab.crm.clients.domain.ClientApplicationService
import com.carslab.crm.clients.domain.ClientDetailResponse
import com.carslab.crm.clients.domain.VehicleApplicationService
import com.carslab.crm.clients.domain.CreateClientRequest
import com.carslab.crm.clients.domain.CreateVehicleRequest
import com.carslab.crm.clients.domain.model.VehicleRelationshipType
import com.carslab.crm.clients.domain.ClientVehicleAssociationService
import com.carslab.crm.clients.domain.VehicleDetailResponse
import com.carslab.crm.domain.model.ClientDetails
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CarReceptionService(
    private val carReceptionRepository: CarReceptionRepository,
    private val clientApplicationService: ClientApplicationService,
    private val vehicleApplicationService: VehicleApplicationService,
    private val clientVehicleAssociationService: ClientVehicleAssociationService,
    private val protocolServicesRepository: ProtocolServicesRepository,
    private val imageStorageService: FileImageStorageService,
    private val protocolCommentsRepository: ProtocolCommentsRepository,
    private val cashRepository: CashRepository,
    private val invoiceRepository: InvoiceRepository,
    private val unifiedDocumentRepository: UnifiedDocumentRepository
) {
    private val logger = LoggerFactory.getLogger(CarReceptionService::class.java)

    fun createProtocol(createProtocolCommand: CreateProtocolRootModel): ProtocolId {
        logger.info("Creating new protocol for vehicle: ${createProtocolCommand.vehicle.brand} ${createProtocolCommand.vehicle.model} (${createProtocolCommand.vehicle.licensePlate})")

        val client = findOrCreateClient(createProtocolCommand.client)
        val vehicle = findExistingVehicle(createProtocolCommand.vehicle) ?: createNewVehicle(createProtocolCommand.vehicle)

        // Associate vehicle with client if not already associated
        val existingAssociation = clientVehicleAssociationService.getClientVehicles(ClientId.of(client.id))
            .any { it.id.value == vehicle.id }

        if (!existingAssociation) {
            clientVehicleAssociationService.associateClientWithVehicle(
                ClientId.of(client.id),
                VehicleId.of(vehicle.id),
                VehicleRelationshipType.OWNER
            )
        }

        val protocolWithFilledIds = createProtocolCommand.copy(
            client = createProtocolCommand.client.copy(
                id = client.id.toString()
            ),
            vehicle = createProtocolCommand.vehicle.copy(
                id = vehicle.id.toString()
            )
        )

        val savedProtocolId = carReceptionRepository.save(protocolWithFilledIds)
        protocolServicesRepository.saveServices(createProtocolCommand.services, savedProtocolId)

        protocolCommentsRepository.save(
            ProtocolComment(
                protocolId = savedProtocolId,
                author = "Administrator",
                content = "Zaplanowano wizytę",
                timestamp = Instant.now().toString(),
                type = "system"
            )
        )

        logger.info("Created protocol with ID: ${savedProtocolId.value}")
        return savedProtocolId
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        logger.info("Updating protocol with ID: ${protocol.id.value}")

        val existingProtocol = carReceptionRepository.findById(protocol.id)
            ?.enrichProtocol()
            ?: throw ResourceNotFoundException("Protocol", protocol.id.value)

        val updatedProtocol = protocol.copy(
            audit = protocol.audit.copy(
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = if (protocol.status != existingProtocol.status) {
                    protocolCommentsRepository.save(
                        ProtocolComment(
                            protocolId = protocol.id,
                            author = "Administrator",
                            content = "Zmieniono status protokołu z \"${existingProtocol.status.uiVale}\" na: \"${protocol.status.uiVale}\"",
                            timestamp = Instant.now().toString(),
                            type = "system"
                        )
                    )
                    LocalDateTime.now()
                } else {
                    existingProtocol.audit.statusUpdatedAt
                }
            ),
            calendarColorId = protocol.calendarColorId
        )

        val changeResult = EnhancedChangeTracker.trackChanges(
            oldObject = existingProtocol,
            newObject = updatedProtocol,
            idProperty = { it.id },
            ignoredFields = setOf("audit", "mediaItems")
        )

        if (changeResult.hasChanges()) {
            val friendlyFormatted = changeResult.formatUserFriendlyEnhanced(CarReceptionProtocol::class.java)

            protocolCommentsRepository.save(
                ProtocolComment(
                    protocolId = protocol.id,
                    author = "Administrator",
                    content = friendlyFormatted,
                    timestamp = Instant.now().toString(),
                    type = "system"
                )
            )
        }

        protocolServicesRepository.saveServices(
            services = updatedProtocol.protocolServices
                .map {
                    CreateServiceModel(
                        name = it.name,
                        basePrice = it.basePrice,
                        discount = it.discount,
                        finalPrice = it.finalPrice,
                        approvalStatus = it.approvalStatus,
                        note = it.note,
                        quantity = it.quantity
                    )
                },
            protocolId = updatedProtocol.id
        )

        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Updated protocol with ID: ${savedProtocol.id.value}")
        return savedProtocol
    }

    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        logger.info("Changing status of protocol ${protocolId.value} to $newStatus")

        val existingProtocol = carReceptionRepository.findById(protocolId)
            ?.enrichProtocol()
            ?: throw ResourceNotFoundException("Protocol", protocolId.value)

        val now = LocalDateTime.now()
        commentOnStatusChange(newStatus, existingProtocol.status, protocolId)
        val updatedProtocol = existingProtocol.copy(
            status = newStatus,
            audit = existingProtocol.audit.copy(
                updatedAt = now,
                statusUpdatedAt = now
            )
        )

        if (updatedProtocol.status == ProtocolStatus.COMPLETED) {
            updateVisitsStatistics(updatedProtocol)
        }

        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Status of protocol ${savedProtocol.id.value} changed to ${savedProtocol.status}")
        return savedProtocol
    }

    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        logger.debug("Getting protocol by ID: ${protocolId.value}")
        return carReceptionRepository.findById(protocolId)?.enrichProtocol()
    }

    private fun commentOnStatusChange(newStatus: ProtocolStatus, previousStatus: ProtocolStatus, protocolId: ProtocolId) =
        protocolCommentsRepository.save(
            ProtocolComment(
                protocolId = protocolId,
                author = "Administrator",
                content = "Zmieniono status protokołu z \"${previousStatus.uiVale}\" na: \"${newStatus.uiVale}\"",
                timestamp = Instant.now().toString(),
                type = "system"
            )
        )

    private fun ProtocolView.enrichProtocol(): CarReceptionProtocol {
        val vehicleDetail = vehicleApplicationService.getVehicleById(vehicleId.value)
        val clientDetail = clientApplicationService.getClientById(clientId.value)
        val services = protocolServicesRepository.findByProtocolId(id)
        val images = imageStorageService.getImagesByProtocol(id)

        return CarReceptionProtocol(
            id = id,
            title = title,
            vehicle = VehicleDetails(
                make = vehicleDetail?.make ?: "",
                model = vehicleDetail?.model ?: "",
                licensePlate = vehicleDetail?.licensePlate ?: "",
                productionYear = vehicleDetail?.year ?: 0,
                vin = vehicleDetail?.vin ?: "",
                color = vehicleDetail?.color ?: "",
                mileage = vehicleDetail?.mileage
            ),
            client = ClientDetails(
                id = clientId.value,
                name = clientDetail?.fullName ?: "",
                email = clientDetail?.email ?: "",
                phone = clientDetail?.phone ?: "",
                companyName = clientDetail?.company,
                taxId = clientDetail?.taxId
            ),
            period = period,
            status = status,
            protocolServices = services
                .map {
                    ProtocolService(
                        id = it.id.toString(),
                        name = it.name,
                        basePrice = it.basePrice,
                        discount = it.discount,
                        finalPrice = it.finalPrice,
                        approvalStatus = it.approvalStatus,
                        note = it.note,
                        quantity = it.quantity
                    )
                },
            notes = notes,
            referralSource = ReferralSource.SEARCH_ENGINE,
            otherSourceDetails = "",
            audit = AuditInfo(
                createdAt = createdAt,
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = LocalDateTime.now(),
                createdBy = LocalDateTime.now().toString(),
                updatedBy = LocalDateTime.now().toString(),
                appointmentId = ""
            ),
            mediaItems = images.map {
                MediaItem(
                    id = it.id,
                    type = MediaType.PHOTO,
                    name = it.name,
                    size = it.size,
                    description = "Zamockowany opis",
                    createdAt = LocalDateTime.now(),
                    tags = it.tags
                )
            },
            documents = Documents(keysProvided, documentsProvided),
            calendarColorId = calendarColorId,
        )
    }

    fun getAllProtocols(): List<CarReceptionProtocol> {
        logger.debug("Getting all protocols")
        return carReceptionRepository.findAll()
    }

    fun searchProtocols(
        clientName: String? = null,
        clientId: Long? = null,
        licensePlate: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): List<CarReceptionProtocol> {
        logger.debug("Searching protocols with filters: clientName=$clientName, clientId=$clientId, licensePlate=$licensePlate, status=$status, startDate=$startDate, endDate=$endDate")

        val protocolViews = carReceptionRepository.searchProtocols(
            clientName = clientName,
            clientId = clientId,
            licensePlate = licensePlate,
            status = status,
            startDate = startDate,
            endDate = endDate
        )

        return protocolViews.map { it.enrichProtocol() }
    }

    fun deleteProtocol(protocolId: ProtocolId): Boolean {
        logger.info("Deleting protocol with ID: ${protocolId.value}")
        return carReceptionRepository.deleteById(protocolId)
    }

    fun deleteImage(protocolId: ProtocolId, imageId: String) {
        protocolCommentsRepository.save(
            ProtocolComment(
                protocolId = protocolId,
                author = "Administrator",
                content = "Usunieto zdjecie",
                timestamp = Instant.now().toString(),
                type = "system"
            )
        )
        return imageStorageService.deleteFile(imageId, protocolId)
    }

    private fun findOrCreateClient(clientData: CreateProtocolClientModel): ClientDetailResponse {
        // Jeśli klient ma ID, próbujemy go znaleźć
        if (clientData.id != null) {
            clientApplicationService.getClientById(clientData.id.toLong())?.let {
                logger.debug("Found existing client with ID: ${clientData.id}")
                return it
            }
        }

        // Próbujemy znaleźć klienta po danych kontaktowych
        val existingClient = findClientByContactInfo(clientData.email, clientData.phone)
        if (existingClient != null) {
            logger.debug("Found existing client by contact information")
            return existingClient
        }

        // Jeśli nie znaleźliśmy klienta, tworzymy nowego
        logger.info("Creating new client: ${clientData.name}")
        return createNewClient(clientData)
    }

    private fun findClientByContactInfo(email: String?, phoneNumber: String?): ClientDetailResponse? {
        if (email.isNullOrBlank() && phoneNumber.isNullOrBlank()) {
            return null
        }

        val searchResults = clientApplicationService.searchClients(
            email = email,
            phone = phoneNumber,
            pageable = org.springframework.data.domain.PageRequest.of(0, 1)
        )

        return searchResults.content.firstOrNull()
    }

    private fun createNewClient(client: CreateProtocolClientModel): ClientDetailResponse {
        val nameParts = client.name.split(" ")
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""

        val newClientRequest = CreateClientRequest(
            firstName = firstName,
            lastName = lastName,
            email = client.email ?: "",
            phone = client.phone ?: "",
            company = client.companyName,
            taxId = client.taxId,
        )

        return clientApplicationService.createClient(newClientRequest)
    }

    private fun findExistingVehicle(protocol: CreateProtocolVehicleModel): VehicleDetailResponse? {
        val searchResults = vehicleApplicationService.searchVehicles(
            licensePlate = protocol.licensePlate,
            vin = protocol.vin,
            pageable = org.springframework.data.domain.PageRequest.of(0, 1)
        )
        return searchResults.content.firstOrNull()
    }

    private fun createNewVehicle(vehicle: CreateProtocolVehicleModel): VehicleDetailResponse {
        val newVehicleRequest = CreateVehicleRequest(
            make = vehicle.brand ?: "",
            model = vehicle.model ?: "",
            year = vehicle.productionYear,
            licensePlate = vehicle.licensePlate ?: "",
            color = vehicle.color,
            vin = vehicle.vin,
            mileage = vehicle.mileage,
            ownerIds = emptyList() // Will be associated separately
        )

        return vehicleApplicationService.createVehicle(newVehicleRequest)
    }

    private fun updateVisitsStatistics(protocol: CarReceptionProtocol) {
        clientApplicationService.updateClientStatistics(
            clientId = ClientId(protocol.client.id!!),
            totalGmv = protocol.protocolServices.sumOf { it.finalPrice.amount }.toBigDecimal()
        )
    }

    fun updateServices(protocolId: ProtocolId, services: List<CreateServiceModel>) {
        protocolCommentsRepository.save(
            ProtocolComment(
                protocolId = protocolId,
                author = "Administrator",
                content = "Dodano nowe usługi: ${services.joinToString(", ") { "${it.name} (${it.finalPrice})" }}",
                timestamp = Instant.now().toString(),
                type = "system"
            )
        )
        protocolServicesRepository.saveServices(services, protocolId)
    }

    fun storeUploadedImage(
        request: MultipartHttpServletRequest,
        protocolId: ProtocolId,
        image: CreateMediaTypeModel
    ) {
        request.fileMap.forEach { (paramName, file) ->
            if (extractImageIndex(paramName) != null) {
                imageStorageService.storeFile(file, protocolId, image)
                return
            }
        }
        protocolCommentsRepository.save(
            ProtocolComment(
                protocolId = protocolId,
                author = "Administrator",
                content = "Dodano nowe zdjęcie: ${image.name}",
                timestamp = Instant.now().toString(),
                type = "system"
            )
        )
    }

    private fun extractImageIndex(paramName: String): Int? {
        val imageIndexRegex = """images\[(\d+)\]""".toRegex()
        val matchResult = imageIndexRegex.find(paramName) ?: return null
        return matchResult.groupValues[1].toInt()
    }

    fun releaseVehicle(existingProtocol: CarReceptionProtocol, releaseDetails: VehicleReleaseDetailsModel): CarReceptionProtocol {
        val updatedProtocol = changeStatus(existingProtocol.id, ProtocolStatus.COMPLETED)

        if (releaseDetails.paymentMethod == PaymentMethod.CASH) {
            registerCashPayment(existingProtocol)
        }

        if (releaseDetails.documentType == DocumentType.INVOICE) {
            createInvoiceDocument(existingProtocol, releaseDetails)
        }

        return updatedProtocol
    }

    private fun createInvoiceDocument(existingProtocol: CarReceptionProtocol, releaseDetails: VehicleReleaseDetailsModel) {
        val gross = existingProtocol.protocolServices.filter { it.approvalStatus == ApprovalStatus.APPROVED }
            .sumOf { it.finalPrice.amount }.toBigDecimal()
        val nett = gross / 1.23.toBigDecimal()
        val totalTax = gross - nett
        val items = existingProtocol.protocolServices.filter { it.approvalStatus == ApprovalStatus.APPROVED }
            .map {
                DocumentItem(
                    name = it.name,
                    description = it.note,
                    quantity = 1.toBigDecimal(),
                    unitPrice = it.finalPrice.amount.toBigDecimal(),
                    taxRate = 23.toBigDecimal(),
                    totalNet = it.finalPrice.amount.toBigDecimal() / 1.23.toBigDecimal(),
                    totalGross = it.finalPrice.amount.toBigDecimal()
                )
            }
        unifiedDocumentRepository.save(
            UnifiedFinancialDocument(
                id = UnifiedDocumentId.generate(),
                number = "",
                type = com.carslab.crm.api.model.DocumentType.INVOICE,
                title = "Faktura za wizytę",
                description = "",
                issuedDate = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(14),
                sellerName = "Detailing Studio",
                sellerTaxId = "123456789",
                sellerAddress = "ul. Kowalskiego 4/14, 00-001 Warszawa",
                buyerName = existingProtocol.client.name,
                buyerTaxId = existingProtocol.client.taxId,
                buyerAddress = "ul. Kliencka 2/14, 00-001 Gdańsk",
                status = DocumentStatus.NOT_PAID,
                direction = TransactionDirection.INCOME,
                paymentMethod = when (releaseDetails.paymentMethod) {
                    PaymentMethod.CASH -> com.carslab.crm.domain.model.view.finance.PaymentMethod.CASH
                    PaymentMethod.CARD -> com.carslab.crm.domain.model.view.finance.PaymentMethod.CARD
                },
                totalNet = nett,
                totalTax = totalTax,
                totalGross = gross,
                paidAmount = BigDecimal.ZERO,
                currency = "PLN",
                notes = "",
                protocolId = existingProtocol.id.value,
                protocolNumber = existingProtocol.id.value,
                visitId = null,
                items = items,
                attachment = null,
                audit = Audit(
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        )
    }

    private fun registerCashPayment(existingProtocol: CarReceptionProtocol) {
        cashRepository.save(
            CashTransaction(
                id = TransactionId.Companion.generate(),
                type = TransactionType.INCOME,
                description = "Opłata za wizytę",
                date = LocalDate.now(),
                amount = existingProtocol.protocolServices.filter { it.approvalStatus == ApprovalStatus.APPROVED }
                    .sumOf { it.finalPrice.amount }.toBigDecimal(),
                visitId = existingProtocol.id.value,
                createdBy = UserId("0"),
                audit = Audit(
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            )
        )
    }

    fun searchProtocolsWithPagination(
        clientName: String? = null,
        clientId: Long? = null,
        licensePlate: String? = null,
        make: String? = null,
        status: ProtocolStatus? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        page: Int = 0,
        size: Int = 10
    ): PaginatedResponse<CarReceptionProtocol> {
        logger.debug("Searching protocols with filters and pagination: clientName=$clientName, clientId=$clientId, licensePlate=$licensePlate, status=$status, startDate=$startDate, endDate=$endDate, page=$page, size=$size")

        val (protocolViews, totalCount) = carReceptionRepository.searchProtocolsWithPagination(
            clientName = clientName,
            clientId = clientId,
            licensePlate = licensePlate,
            status = status,
            make = make,
            startDate = startDate,
            endDate = endDate,
            page = page,
            size = size
        )

        val totalPages = if (totalCount % size == 0L) totalCount / size else totalCount / size + 1

        return PaginatedResponse(
            data = protocolViews.map { it.enrichProtocol() },
            page = page,
            size = size,
            totalItems = totalCount,
            totalPages = totalPages
        )
    }

    fun countProtocolsByStatus(status: ProtocolStatus): Int {
        return carReceptionRepository.countProtocolsByStatus(status)
    }
}