package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.ApiProtocolStatus
import com.carslab.crm.api.model.commands.*
import com.carslab.crm.api.model.request.ApiDiscountType
import com.carslab.crm.api.model.request.ApiReferralSource
import com.carslab.crm.api.model.request.ServiceApprovalStatus
import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.create.protocol.CreateProtocolClientModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.create.protocol.CreateProtocolVehicleModel
import com.carslab.crm.domain.model.create.protocol.CreateServiceModel
import java.time.LocalDate
import java.time.LocalDateTime
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
    fun mapStatus(status: String): com.carslab.crm.domain.model.ProtocolStatus {
        return when (status.uppercase()) {
            "SCHEDULED" -> com.carslab.crm.domain.model.ProtocolStatus.SCHEDULED
            "PENDING_APPROVAL" -> com.carslab.crm.domain.model.ProtocolStatus.PENDING_APPROVAL
            "IN_PROGRESS" -> com.carslab.crm.domain.model.ProtocolStatus.IN_PROGRESS
            "READY_FOR_PICKUP" -> com.carslab.crm.domain.model.ProtocolStatus.READY_FOR_PICKUP
            "COMPLETED" -> com.carslab.crm.domain.model.ProtocolStatus.COMPLETED
            else -> throw IllegalArgumentException("Unknown status: $status")
        }
    }

    /**
     * Mapuje status API na status domeny.
     * Metoda jest dostępna publicznie dla kontrolera.
     */
    fun mapApiStatusToDomain(apiStatus: ApiProtocolStatus?): com.carslab.crm.domain.model.ProtocolStatus {
        return when (apiStatus) {
            ApiProtocolStatus.SCHEDULED -> com.carslab.crm.domain.model.ProtocolStatus.SCHEDULED
            ApiProtocolStatus.PENDING_APPROVAL -> com.carslab.crm.domain.model.ProtocolStatus.PENDING_APPROVAL
            ApiProtocolStatus.IN_PROGRESS -> com.carslab.crm.domain.model.ProtocolStatus.IN_PROGRESS
            ApiProtocolStatus.READY_FOR_PICKUP -> com.carslab.crm.domain.model.ProtocolStatus.READY_FOR_PICKUP
            ApiProtocolStatus.COMPLETED -> com.carslab.crm.domain.model.ProtocolStatus.COMPLETED
            null -> com.carslab.crm.domain.model.ProtocolStatus.SCHEDULED // Domyślna wartość
        }
    }

    /**
     * Konwertuje komendę utworzenia protokołu na model domenowy.
     */
    fun fromCreateCommand(command: CreateCarReceptionCommand): CreateProtocolRootModel {
        val protocolId = ProtocolId.generate()

        val startDate = LocalDate.parse(command.startDate)
        val endDate = command.endDate?.let { LocalDate.parse(it) } ?: startDate

        val now = LocalDateTime.now()

        return CreateProtocolRootModel(
            id = protocolId,
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
                startDate = startDate,
                endDate = endDate
            ),
            status = mapApiStatusToDomain(command.status),
            services = command.selectedServices?.map { mapCreateServiceCommandToService(it) } ?: emptyList(),
            notes = command.notes,
            referralSource = mapApiReferralSourceToDomain(command.referralSource),
            otherSourceDetails = command.otherSourceDetails,
            documents = Documents(
                keysProvided = command.keysProvided ?: false,
                documentsProvided = command.documentsProvided ?: false
            ),
            mediaItems = emptyList(),
            audit = AuditInfo(
                createdAt = now,
                updatedAt = now,
                statusUpdatedAt = now,
                appointmentId = command.appointmentId
            )
        )
    }

    /**
     * Konwertuje komendę aktualizacji protokołu na model domenowy.
     */
    fun fromUpdateCommand(command: UpdateCarReceptionCommand, existingProtocol: CarReceptionProtocol? = null): CarReceptionProtocol {
        val protocolId = ProtocolId(command.id)

        // Konwertujemy daty
        val startDate = LocalDate.parse(command.startDate)
        val endDate = command.endDate?.let { LocalDate.parse(it) } ?: startDate

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
            services = command.selectedServices?.map { mapUpdateServiceCommandToService(it) } ?: emptyList(),
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
            )
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
            totalServiceCount = protocol.services.size,
            totalAmount = protocol.services.sumOf { it.finalPrice.amount }
        )
    }

    /**
     * Konwertuje model domenowy na DTO szczegółów.
     */
    fun toDetailDto(protocol: CarReceptionProtocol): CarReceptionDetailDto {
        return CarReceptionDetailDto(
            id = protocol.id.value,
            startDate = protocol.period.startDate.format(DATE_FORMATTER),
            endDate = protocol.period.endDate.format(DATE_FORMATTER),
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
            selectedServices = protocol.services.map { service ->
                ServiceDto(
                    id = service.id,
                    name = service.name,
                    price = service.basePrice.amount,
                    discountType = mapDomainDiscountTypeToApi(service.discount?.type),
                    discountValue = service.discount?.value ?: 0.0,
                    finalPrice = service.finalPrice.amount,
                    approvalStatus = mapDomainApprovalStatusToApi(service.approvalStatus)
                )
            },
            status = mapDomainStatusToApi(protocol.status),
            referralSource = mapDomainReferralSourceToApi(protocol.referralSource),
            otherSourceDetails = protocol.otherSourceDetails,
            vehicleImages = emptyList(), // Ten fragment powinien być zaktualizowany, gdy zaimplementujesz obsługę obrazów
            createdAt = protocol.audit.createdAt.format(DATETIME_FORMATTER),
            updatedAt = protocol.audit.updatedAt.format(DATETIME_FORMATTER),
            statusUpdatedAt = protocol.audit.statusUpdatedAt.format(DATETIME_FORMATTER),
            appointmentId = protocol.audit.appointmentId
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
            totalAmount = protocol.services.sumOf { it.finalPrice.amount }
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
            approvalStatus = command.approvalStatus?.let { mapApiApprovalStatusToDomain(it) } ?: ApprovalStatus.PENDING
        )
    }

    private fun mapUpdateServiceCommandToService(command: UpdateServiceCommand): Service {
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

        return Service(
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
            approvalStatus = command.approvalStatus?.let { mapApiApprovalStatusToDomain(it) } ?: ApprovalStatus.PENDING
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