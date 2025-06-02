package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.commands.*
import com.carslab.crm.api.model.request.ApiDiscountType
import com.carslab.crm.api.model.request.ApiReferralSource
import com.carslab.crm.api.model.request.ServiceApprovalStatus
import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.*
import com.carslab.crm.domain.model.view.calendar.CalendarColorId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Zrefaktoryzowany mapper dla modeli protokołu przyjęcia pojazdu.
 * Zawiera osobne metody dla różnych operacji CRUD.
 */
object CarReceptionDtoMapper {
    private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE
    val DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

    /**
     * Mapuje string na status domeny.
     * Metoda jest używana głównie do konwersji filtrów zapytań.
     */
    fun mapStatus(status: String): ProtocolStatus {
        return when (status.uppercase()) {
            "SCHEDULED" -> ProtocolStatus.SCHEDULED
            "PENDING_APPROVAL" -> ProtocolStatus.PENDING_APPROVAL
            "IN_PROGRESS" -> ProtocolStatus.IN_PROGRESS
            "READY_FOR_PICKUP" -> ProtocolStatus.READY_FOR_PICKUP
            "COMPLETED" -> ProtocolStatus.COMPLETED
            "CANCELLED" -> ProtocolStatus.CANCELLED
            else -> throw IllegalArgumentException("Unknown status: $status")
        }
    }

    /**
     * Mapuje status API na status domeny.
     * Metoda jest dostępna publicznie dla kontrolera.
     */
    fun mapApiStatusToDomain(apiStatus: ApiProtocolStatus?): ProtocolStatus {
        return when (apiStatus) {
            ApiProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
            ApiProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
            ApiProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
            ApiProtocolStatus.READY_FOR_PICKUP -> ProtocolStatus.READY_FOR_PICKUP
            ApiProtocolStatus.COMPLETED -> ProtocolStatus.COMPLETED
            ApiProtocolStatus.CANCELLED -> ProtocolStatus.CANCELLED
            null -> ProtocolStatus.SCHEDULED // Domyślna wartość
        }
    }

    /**
     * Konwertuje komendę utworzenia protokołu na model domenowy.
     */
    fun fromCreateCommand(command: CreateCarReceptionCommand): CreateProtocolRootModel {
        val protocolId = ProtocolId.generate()

        val startDateTime = try {
            LocalDateTime.parse(command.startDate)
        } catch (e: Exception) {
            LocalDateTime.of(LocalDate.parse(command.startDate), LocalTime.of(8, 0))
        }

        val endDateTime = command.endDate?.let {
            try {
                LocalDateTime.parse(it)
            } catch (e: Exception) {
                LocalDateTime.of(LocalDate.parse(it), LocalTime.of(23, 59, 59))
            }
        } ?: LocalDateTime.of(LocalDate.parse(command.startDate), LocalTime.of(23, 59, 59))

        val now = LocalDateTime.now()

        return CreateProtocolRootModel(
            id = protocolId,
            title = command.title,
            calendarColorId = CalendarColorId(command.calendarColorId),
            vehicle = CreateProtocolVehicleModel(
                brand = command.make,
                model = command.model,
                licensePlate = command.licensePlate,
                productionYear = command.productionYear ?: 0,
                vin = command.vin,
                mileage = command.mileage,
                color = command.color
            ),
            client = CreateProtocolClientModel(
                name = command.ownerName,
                email = command.email,
                phone = command.phone,
                companyName = command.companyName,
                taxId = command.taxId
            ),
            period = ServicePeriod(
                startDate = startDateTime,
                endDate = endDateTime
            ),
            status = mapApiStatusToDomain(command.status),
            services = command.selectedServices?.map { mapCreateServiceCommandToService(it) } ?: emptyList(),
            notes = command.notes,
            referralSource = mapApiReferralSourceToDomain(command.referralSource),
            otherSourceDetails = command.otherSourceDetails,
            documents = Documents(
                keysProvided = if(command.status == ApiProtocolStatus.SCHEDULED) true else (command.keysProvided ?: false),
                documentsProvided = command.documentsProvided ?: false
            ),
            mediaItems = command.vehicleImages
                ?.map { it.fromCreateImageCommand() } ?: emptyList(),
            audit = AuditInfo(
                createdAt = now,
                updatedAt = now,
                statusUpdatedAt = now,
                appointmentId = command.appointmentId
            )
        )
    }


    fun CreateVehicleImageCommand.fromCreateImageCommand() =
        CreateMediaTypeModel(
            type = MediaType.PHOTO,
            name = name ?: "Unknown",
            description = description,
            location = location,
            tags = tags
        )

    fun UpdateVehicleImageCommand.fromCreateImageCommand() =
        UpdateMediaTypeMode(
            name = name ?: "Unknown",
            description = description,
            location = location,
            tags = tags
        )

    /**
     * Konwertuje komendę aktualizacji protokołu na model domenowy.
     */
    fun fromUpdateCommand(command: UpdateCarReceptionCommand, existingProtocol: CarReceptionProtocol? = null): CarReceptionProtocol {
        val protocolId = ProtocolId(command.id)

        val startDate = try {
            LocalDateTime.parse(command.startDate)
        } catch (e: Exception) {
            // Jeśli format to tylko data bez czasu, dodaj domyślną godzinę 8:00
            LocalDateTime.of(LocalDate.parse(command.startDate), LocalTime.of(8, 0))
        }

        val endDate = command.endDate?.let {
            try {
                LocalDateTime.parse(it)
            } catch (e: Exception) {
                // Jeśli format to tylko data bez czasu, dodaj czas końca dnia 23:59:59
                LocalDateTime.of(LocalDate.parse(it), LocalTime.of(23, 59, 59))
            }
        } ?: LocalDateTime.of(LocalDate.parse(command.startDate), LocalTime.of(23, 59, 59))

        // Tworzymy informacje audytowe
        val now = LocalDateTime.now()

        // Jeśli istnieje oryginalny protokół, zachowujemy jego oryginalne dane audytowe
        val createdAt = existingProtocol?.audit?.createdAt ?: now
        val statusUpdatedAt = if (existingProtocol != null &&
            mapApiStatusToDomain(command.status) != existingProtocol.status) {
            now
        } else {
            existingProtocol?.audit?.statusUpdatedAt ?: now
        }

        return CarReceptionProtocol(
            id = protocolId,
            title = command.title,
            vehicle = VehicleDetails(
                make = command.make,
                model = command.model,
                licensePlate = command.licensePlate,
                productionYear = command.productionYear ?: 0,
                vin = command.vin,
                mileage = command.mileage,
                color = command.color
            ),
            client = Client(
                id = command.ownerId,
                name = command.ownerName,
                email = command.email,
                phone = command.phone,
                companyName = command.companyName,
                taxId = command.taxId
            ),
            period = ServicePeriod(
                startDate = startDate,
                endDate = endDate
            ),
            status = mapApiStatusToDomain(command.status),
            protocolServices = command.selectedServices?.map { mapUpdateServiceCommandToService(it) } ?: emptyList(),
            notes = command.notes,
            referralSource = mapApiReferralSourceToDomain(command.referralSource),
            otherSourceDetails = command.otherSourceDetails,
            documents = Documents(
                keysProvided = command.keysProvided ?: false,
                documentsProvided = command.documentsProvided ?: false
            ),
            mediaItems = emptyList(),
            audit = AuditInfo(
                createdAt = createdAt,
                updatedAt = now,
                statusUpdatedAt = statusUpdatedAt,
                appointmentId = command.appointmentId
            ),
            calendarColorId = CalendarColorId(command.calendarColorId),
        )
    }

    /**
     * Konwertuje model domenowy na DTO podstawowych informacji.
     */
    fun toBasicDto(protocol: CarReceptionProtocol): CarReceptionBasicDto {
        return CarReceptionBasicDto(
            id = protocol.id.value,
            createdAt = protocol.audit.createdAt.format(DATETIME_FORMATTER),
            updatedAt = protocol.audit.updatedAt.format(DATETIME_FORMATTER),
            statusUpdatedAt = protocol.audit.statusUpdatedAt.format(DATETIME_FORMATTER),
            status = mapDomainStatusToApi(protocol.status)
        )
    }

    /**
     * Konwertuje model domenowy na DTO listy.
     */
    fun toListDto(protocol: CarReceptionProtocol): CarReceptionListDto {
        return CarReceptionListDto(
            id = protocol.id.value,
            vehicle = VehicleBasicDto(
                make = protocol.vehicle.make,
                model = protocol.vehicle.model,
                licensePlate = protocol.vehicle.licensePlate,
                productionYear = protocol.vehicle.productionYear,
                color = protocol.vehicle.color
            ),
            period = PeriodDto(
                startDate = protocol.period.startDate.format(DATE_FORMATTER),
                endDate = protocol.period.endDate.format(DATE_FORMATTER)
            ),
            owner = OwnerBasicDto(
                name = protocol.client.name,
                companyName = protocol.client.companyName
            ),
            status = mapDomainStatusToApi(protocol.status),
            totalServiceCount = protocol.protocolServices.size,
            totalAmount = protocol.protocolServices.sumOf { it.finalPrice.amount },
            calendarColorId = protocol.calendarColorId.value,
            selectedServices = protocol.protocolServices.map { service ->
                ServiceDto(
                    id = service.id,
                    name = service.name,
                    price = service.basePrice.amount,
                    discountType = mapDomainDiscountTypeToApi(service.discount?.type),
                    discountValue = service.discount?.value ?: 0.0,
                    finalPrice = service.finalPrice.amount,
                    approvalStatus = mapDomainApprovalStatusToApi(service.approvalStatus),
                    note = service.note,
                    quantity = service.quantity
                )
            },
            title = protocol.title,
            lastUpdate = protocol.audit.updatedAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
        )
    }

    /**
     * Konwertuje model domenowy na DTO szczegółów.
     */
    fun toDetailDto(protocol: CarReceptionProtocol): CarReceptionDetailDto {
        return CarReceptionDetailDto(
            id = protocol.id.value,
            title = protocol.title,
            startDate = protocol.period.startDate.format(DATETIME_FORMATTER),
            endDate = protocol.period.endDate.format(DATETIME_FORMATTER),
            licensePlate = protocol.vehicle.licensePlate,
            make = protocol.vehicle.make,
            model = protocol.vehicle.model,
            productionYear = protocol.vehicle.productionYear,
            mileage = protocol.vehicle.mileage,
            vin = protocol.vehicle.vin,
            color = protocol.vehicle.color,
            keysProvided = protocol.documents.keysProvided,
            documentsProvided = protocol.documents.documentsProvided,
            ownerId = protocol.client.id,
            ownerName = protocol.client.name,
            companyName = protocol.client.companyName,
            taxId = protocol.client.taxId,
            email = protocol.client.email,
            phone = protocol.client.phone,
            notes = protocol.notes,
            selectedServices = protocol.protocolServices.map { service ->
                ServiceDto(
                    id = service.id,
                    name = service.name,
                    price = service.basePrice.amount,
                    discountType = mapDomainDiscountTypeToApi(service.discount?.type),
                    discountValue = service.discount?.value ?: 0.0,
                    finalPrice = service.finalPrice.amount,
                    approvalStatus = mapDomainApprovalStatusToApi(service.approvalStatus),
                    note = service.note,
                    quantity = service.quantity
                )
            },
            status = mapDomainStatusToApi(protocol.status),
            referralSource = mapDomainReferralSourceToApi(protocol.referralSource),
            otherSourceDetails = protocol.otherSourceDetails,
            vehicleImages = protocol.mediaItems.filter { it.type == MediaType.PHOTO }
                .map { VehicleImageDto(
                    id = it.id,
                    name = it.name,
                    size = it.size,
                    type = it.type.toString(),
                    storageId = it.id,
                    createdAt = Instant.now(),
                    description = it.description,
                    tags = it.tags
                ) },
            createdAt = protocol.audit.createdAt.format(DATETIME_FORMATTER),
            updatedAt = protocol.audit.updatedAt.format(DATETIME_FORMATTER),
            statusUpdatedAt = protocol.audit.statusUpdatedAt.format(DATETIME_FORMATTER),
            appointmentId = protocol.audit.appointmentId,
            calendarColorId = protocol.calendarColorId.value,
        )
    }

    /**
     * Konwertuje model domenowy na DTO historii protokołów klienta.
     */
    fun toClientHistoryDto(protocol: CarReceptionProtocol): ClientProtocolHistoryDto {
        return ClientProtocolHistoryDto(
            id = protocol.id.value,
            startDate = protocol.period.startDate.toString(),
            endDate = protocol.period.endDate.toString(),
            status = mapDomainStatusToApi(protocol.status),
            carMake = protocol.vehicle.make,
            carModel = protocol.vehicle.model,
            licensePlate = protocol.vehicle.licensePlate,
            totalAmount = protocol.protocolServices.sumOf { it.finalPrice.amount }
        )
    }

    // Metody pomocnicze do mapowania usług

     fun mapCreateServiceCommandToService(command: CreateServiceCommand): CreateServiceModel {
        val basePrice = command.price
        val discountValue = command.discountValue ?: 0.0
        val discountType = command.discountType?.let { mapApiDiscountTypeToDomain(it) }

        // Obliczanie ostatecznej ceny
        val finalPrice = if (command.finalPrice != null) {
            command.finalPrice
        } else if (discountValue > 0 && discountType != null) {
            calculateDiscountedPrice(basePrice, discountValue, discountType)
        } else {
            basePrice
        }

        return CreateServiceModel(
            name = command.name,
            basePrice = Money(basePrice),
            discount = if (discountValue > 0 && discountType != null) {
                Discount(
                    type = discountType,
                    value = discountValue,
                    calculatedAmount = Money(basePrice - finalPrice)
                )
            } else null,
            finalPrice = Money(finalPrice),
            approvalStatus = command.approvalStatus?.let { mapApiApprovalStatusToDomain(it) } ?: ApprovalStatus.PENDING,
            note = command.note,
            quantity = command.quantity
        )
    }

    private fun mapUpdateServiceCommandToService(command: UpdateServiceCommand): ProtocolService {
        val basePrice = command.price
        val discountValue = command.discountValue ?: 0.0
        val discountType = command.discountType?.let { mapApiDiscountTypeToDomain(it) }

        // Obliczanie ostatecznej ceny
        val finalPrice = if (command.finalPrice != null) {
            command.finalPrice
        } else if (discountValue > 0 && discountType != null) {
            calculateDiscountedPrice(basePrice, discountValue, discountType)
        } else {
            basePrice
        }

        return ProtocolService(
            id = command.id,
            name = command.name,
            basePrice = Money(basePrice),
            discount = if (discountValue > 0 && discountType != null) {
                Discount(
                    type = discountType,
                    value = discountValue,
                    calculatedAmount = Money(basePrice - finalPrice)
                )
            } else null,
            finalPrice = Money(finalPrice),
            approvalStatus = command.approvalStatus?.let { mapApiApprovalStatusToDomain(it) } ?: ApprovalStatus.PENDING,
            note = command.note,
            quantity = command.quantity
        )
    }

    private fun calculateDiscountedPrice(basePrice: Double, discountValue: Double, discountType: DiscountType): Double {
        return when (discountType) {
            DiscountType.PERCENTAGE -> basePrice * (1 - discountValue / 100)
            DiscountType.AMOUNT -> basePrice - discountValue
            DiscountType.FIXED_PRICE -> discountValue
        }
    }

    // Mappery konwertujące między typami API i domeny

    private fun mapDomainStatusToApi(domainStatus: ProtocolStatus): ApiProtocolStatus {
        return when (domainStatus) {
            ProtocolStatus.SCHEDULED -> ApiProtocolStatus.SCHEDULED
            ProtocolStatus.PENDING_APPROVAL -> ApiProtocolStatus.PENDING_APPROVAL
            ProtocolStatus.IN_PROGRESS -> ApiProtocolStatus.IN_PROGRESS
            ProtocolStatus.READY_FOR_PICKUP -> ApiProtocolStatus.READY_FOR_PICKUP
            ProtocolStatus.COMPLETED -> ApiProtocolStatus.COMPLETED
            ProtocolStatus.CANCELLED -> ApiProtocolStatus.CANCELLED
        }
    }

    private fun mapApiDiscountTypeToDomain(apiDiscountType: ApiDiscountType): DiscountType {
        return when (apiDiscountType) {
            ApiDiscountType.PERCENTAGE -> DiscountType.PERCENTAGE
            ApiDiscountType.AMOUNT -> DiscountType.AMOUNT
            ApiDiscountType.FIXED_PRICE -> DiscountType.FIXED_PRICE
        }
    }

    private fun mapDomainDiscountTypeToApi(discountType: DiscountType?): ApiDiscountType? {
        return discountType?.let {
            when (it) {
                DiscountType.PERCENTAGE -> ApiDiscountType.PERCENTAGE
                DiscountType.AMOUNT -> ApiDiscountType.AMOUNT
                DiscountType.FIXED_PRICE -> ApiDiscountType.FIXED_PRICE
            }
        }
    }

    private fun mapApiReferralSourceToDomain(apiReferralSource: ApiReferralSource?): ReferralSource? {
        return apiReferralSource?.let {
            when (it) {
                ApiReferralSource.REGULAR_CUSTOMER -> ReferralSource.REGULAR_CUSTOMER
                ApiReferralSource.RECOMMENDATION -> ReferralSource.RECOMMENDATION
                ApiReferralSource.SEARCH_ENGINE -> ReferralSource.SEARCH_ENGINE
                ApiReferralSource.SOCIAL_MEDIA -> ReferralSource.SOCIAL_MEDIA
                ApiReferralSource.LOCAL_AD -> ReferralSource.LOCAL_AD
                ApiReferralSource.OTHER -> ReferralSource.OTHER
            }
        }
    }

    private fun mapDomainReferralSourceToApi(referralSource: ReferralSource?): ApiReferralSource? {
        return referralSource?.let {
            when (it) {
                ReferralSource.REGULAR_CUSTOMER -> ApiReferralSource.REGULAR_CUSTOMER
                ReferralSource.RECOMMENDATION -> ApiReferralSource.RECOMMENDATION
                ReferralSource.SEARCH_ENGINE -> ApiReferralSource.SEARCH_ENGINE
                ReferralSource.SOCIAL_MEDIA -> ApiReferralSource.SOCIAL_MEDIA
                ReferralSource.LOCAL_AD -> ApiReferralSource.LOCAL_AD
                ReferralSource.OTHER -> ApiReferralSource.OTHER
            }
        }
    }

    private fun mapApiApprovalStatusToDomain(apiApprovalStatus: ServiceApprovalStatus): ApprovalStatus {
        return when (apiApprovalStatus) {
            ServiceApprovalStatus.PENDING -> ApprovalStatus.PENDING
            ServiceApprovalStatus.APPROVED -> ApprovalStatus.APPROVED
            ServiceApprovalStatus.REJECTED -> ApprovalStatus.REJECTED
        }
    }

    private fun mapDomainApprovalStatusToApi(approvalStatus: ApprovalStatus): ServiceApprovalStatus {
        return when (approvalStatus) {
            ApprovalStatus.PENDING -> ServiceApprovalStatus.PENDING
            ApprovalStatus.APPROVED -> ServiceApprovalStatus.APPROVED
            ApprovalStatus.REJECTED -> ServiceApprovalStatus.REJECTED
        }
    }
}