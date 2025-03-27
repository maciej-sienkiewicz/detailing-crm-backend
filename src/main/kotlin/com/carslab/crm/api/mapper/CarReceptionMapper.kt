package com.carslab.crm.api.mapper

import com.carslab.crm.api.model.request.CarReceptionProtocolRequest
import com.carslab.crm.api.model.request.SelectedServiceRequest
import com.carslab.crm.api.model.request.ServiceApprovalStatus
import com.carslab.crm.api.model.response.*
import com.carslab.crm.api.model.request.DiscountType as ApiDiscountType
import com.carslab.crm.api.model.request.ProtocolStatus as ApiProtocolStatus
import com.carslab.crm.api.model.request.ReferralSource as ApiReferralSource
import com.carslab.crm.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Klasa mapująca DTO na obiekty domenowe i odwrotnie.
 */
class CarReceptionMapper {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE
        private val DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME

        /**
         * Mapuje żądanie utworzenia protokołu na model domenowy.
         */
        fun toDomain(request: CarReceptionProtocolRequest): CarReceptionProtocol {
            // Generujemy nowe ID dla protokołu lub używamy istniejącego
            val protocolId = if (request.id != null) ProtocolId(request.id!!) else ProtocolId.generate()

            // Konwertujemy daty
            val startDate = LocalDate.parse(request.startDate)
            val endDate = LocalDate.parse(request.endDate ?: request.startDate)

            // Tworzymy informacje audytowe
            val now = LocalDateTime.now()
            val statusUpdatedAt = request.statusUpdatedAt?.let { LocalDateTime.parse(it, DATETIME_FORMATTER) } ?: now

            return CarReceptionProtocol(
                id = protocolId,
                vehicle = VehicleDetails(
                    make = request.make ?: "",
                    model = request.model ?: "",
                    licensePlate = request.licensePlate ?: "",
                    productionYear = request.productionYear ?: 0,
                    vin = request.vin,
                    mileage = request.mileage
                ),
                client = Client(
                    id = request.ownerId,
                    name = request.ownerName ?: "",
                    email = request.email,
                    phone = request.phone,
                    companyName = request.companyName,
                    taxId = request.taxId
                ),
                period = ServicePeriod(
                    startDate = startDate,
                    endDate = endDate
                ),
                status = mapStatus(request.status),
                services = mapServices(request.selectedServices),
                notes = request.notes,
                referralSource = mapReferralSource(request.referralSource),
                otherSourceDetails = request.otherSourceDetails,
                documents = Documents(
                    keysProvided = request.keysProvided ?: false,
                    documentsProvided = request.documentsProvided ?: false
                ),
                mediaItems = emptyList(), // Zdjęcia i inne media są obsługiwane osobno
                audit = AuditInfo(
                    createdAt = now,
                    updatedAt = now,
                    statusUpdatedAt = statusUpdatedAt,
                    appointmentId = request.appointmentId
                )
            )
        }

        /**
         * Mapuje model domenowy na odpowiedź API.
         */
        fun toResponse(protocol: CarReceptionProtocol): CarReceptionProtocolResponse {
            return CarReceptionProtocolResponse(
                id = protocol.id.value,
                createdAt = protocol.audit.createdAt.format(DATETIME_FORMATTER),
                updatedAt = protocol.audit.updatedAt.format(DATETIME_FORMATTER),
                statusUpdatedAt = protocol.audit.statusUpdatedAt.format(DATETIME_FORMATTER),
                status = mapStatus(protocol.status)
            )
        }

        /**
         * Mapuje model domenowy na odpowiedź szczegółową API.
         */
        fun toDetailResponse(protocol: CarReceptionProtocol): CarReceptionProtocolDetailResponse {
            return CarReceptionProtocolDetailResponse(
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
                    SelectedServiceResponse(
                        id = service.id,
                        name = service.name,
                        price = service.basePrice.amount,
                        discountType =  service.discount?.type ?: DiscountType.AMOUNT,
                        discountValue = service.discount?.value ?: 0.0,
                        finalPrice = service.finalPrice.amount,
                        approvalStatus = when (service.approvalStatus) {
                            ApprovalStatus.PENDING -> ServiceApprovalStatus.PENDING
                            ApprovalStatus.APPROVED -> ServiceApprovalStatus.APPROVED
                            ApprovalStatus.REJECTED -> ServiceApprovalStatus.REJECTED
                        }
                    )
                },
                status = mapStatus(protocol.status),
                referralSource = mapReferralSource(protocol.referralSource),
                otherSourceDetails = protocol.otherSourceDetails,
                vehicleImages = emptyList(),
                createdAt = protocol.audit.createdAt.format(DATETIME_FORMATTER),
                updatedAt = protocol.audit.updatedAt.format(DATETIME_FORMATTER),
                statusUpdatedAt = protocol.audit.statusUpdatedAt.format(DATETIME_FORMATTER),
                appointmentId = protocol.audit.appointmentId
            )
        }

        /**
         * Mapuje typ rabatu z domeny na API.
         */
        private fun mapDiscountType(discountType: DiscountType?): ApiDiscountType? {
            return when (discountType) {
                DiscountType.PERCENTAGE -> ApiDiscountType.PERCENTAGE
                DiscountType.AMOUNT -> ApiDiscountType.AMOUNT
                DiscountType.FIXED_PRICE -> ApiDiscountType.FIXED_PRICE
                null -> null
            }
        }

        /**
         * Mapuje źródło polecenia z domeny na API.
         */
        private fun mapReferralSource(referralSource: ReferralSource?): ApiReferralSource? {
            return when (referralSource) {
                ReferralSource.REGULAR_CUSTOMER -> ApiReferralSource.REGULAR_CUSTOMER
                ReferralSource.RECOMMENDATION -> ApiReferralSource.RECOMMENDATION
                ReferralSource.SEARCH_ENGINE -> ApiReferralSource.SEARCH_ENGINE
                ReferralSource.SOCIAL_MEDIA -> ApiReferralSource.SOCIAL_MEDIA
                ReferralSource.LOCAL_AD -> ApiReferralSource.LOCAL_AD
                ReferralSource.OTHER -> ApiReferralSource.OTHER
                null -> null
            }
        }

        fun toListResponse(protocol: CarReceptionProtocol): CarReceptionProtocolListResponse {
            return CarReceptionProtocolListResponse(
                id = protocol.id.value,
                vehicle = VehicleBasicInfo(
                    make = protocol.vehicle.make,
                    model = protocol.vehicle.model,
                    licensePlate = protocol.vehicle.licensePlate,
                    productionYear = protocol.vehicle.productionYear,
                    color = protocol.vehicle.color
                ),
                period = PeriodInfo(
                    startDate = protocol.period.startDate.format(DATE_FORMATTER),
                    endDate = protocol.period.endDate.format(DATE_FORMATTER)
                ),
                owner = OwnerBasicInfo(
                    name = protocol.client.name,
                    companyName = protocol.client.companyName
                ),
                status = mapStatuss(protocol.status),
                totalServiceCount = protocol.services.size,
                totalAmount = protocol.services.sumOf { it.finalPrice.amount }
            )
        }

        // Metody pomocnicze do mapowania poszczególnych elementów

        private fun mapServices(apiServices: List<SelectedServiceRequest>?): List<Service> {
            return apiServices?.map { apiService ->
                Service(
                    id = apiService.id ?: "",
                    name = apiService.name ?: "",
                    basePrice = Money(apiService.price ?: 0.0),
                    discount = if (apiService.discountValue != null && apiService.discountValue!! > 0) {
                        Discount(
                            type = mapDiscountType(apiService.discountType),
                            value = apiService.discountValue!!,
                            calculatedAmount = Money(
                                amount = calculateDiscountAmount(
                                    apiService.price ?: 0.0,
                                    apiService.discountValue!!,
                                    mapDiscountType(apiService.discountType)
                                )
                            )
                        )
                    } else null,
                    finalPrice = Money(apiService.finalPrice ?: apiService.price ?: 0.0),
                    approvalStatus = when (apiService.approvalStatus) {
                        ServiceApprovalStatus.PENDING -> ApprovalStatus.PENDING
                        ServiceApprovalStatus.APPROVED -> ApprovalStatus.APPROVED
                        ServiceApprovalStatus.REJECTED -> ApprovalStatus.REJECTED
                        null -> ApprovalStatus.PENDING
                    }
                )
            } ?: emptyList()
        }

        private fun calculateDiscountAmount(basePrice: Double, discountValue: Double, discountType: DiscountType): Double {
            return when (discountType) {
                DiscountType.PERCENTAGE -> basePrice * (discountValue / 100)
                DiscountType.AMOUNT -> discountValue
                DiscountType.FIXED_PRICE -> basePrice - discountValue
            }
        }

        private fun mapStatus(apiStatus: ApiProtocolStatus?): ProtocolStatus {
            return when (apiStatus) {
                ApiProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
                ApiProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
                ApiProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
                ApiProtocolStatus.READY_FOR_PICKUP -> ProtocolStatus.READY_FOR_PICKUP
                ApiProtocolStatus.COMPLETED -> ProtocolStatus.COMPLETED
                null -> ProtocolStatus.SCHEDULED
            }
        }

        private fun mapStatuss(domainStatus:  com.carslab.crm.domain.model.ProtocolStatus): com.carslab.crm.api.model.request.ProtocolStatus {
            return when (domainStatus) {
                com.carslab.crm.domain.model.ProtocolStatus.SCHEDULED -> com.carslab.crm.api.model.request.ProtocolStatus.SCHEDULED
                com.carslab.crm.domain.model.ProtocolStatus.PENDING_APPROVAL -> com.carslab.crm.api.model.request.ProtocolStatus.PENDING_APPROVAL
                com.carslab.crm.domain.model.ProtocolStatus.IN_PROGRESS -> com.carslab.crm.api.model.request.ProtocolStatus.IN_PROGRESS
                com.carslab.crm.domain.model.ProtocolStatus.READY_FOR_PICKUP -> com.carslab.crm.api.model.request.ProtocolStatus.READY_FOR_PICKUP
                com.carslab.crm.domain.model.ProtocolStatus.COMPLETED -> com.carslab.crm.api.model.request.ProtocolStatus.COMPLETED
                null -> com.carslab.crm.api.model.request.ProtocolStatus.SCHEDULED
            }
        }

        private fun mapStatus(domainStatus: ProtocolStatus): ProtocolStatus {
            return when (domainStatus) {
                ProtocolStatus.SCHEDULED -> ProtocolStatus.SCHEDULED
                ProtocolStatus.PENDING_APPROVAL -> ProtocolStatus.PENDING_APPROVAL
                ProtocolStatus.IN_PROGRESS -> ProtocolStatus.IN_PROGRESS
                ProtocolStatus.READY_FOR_PICKUP -> ProtocolStatus.READY_FOR_PICKUP
                ProtocolStatus.COMPLETED -> ProtocolStatus.COMPLETED
            }
        }

        private fun mapDiscountType(apiDiscountType: ApiDiscountType?): DiscountType {
            return when (apiDiscountType) {
                ApiDiscountType.PERCENTAGE -> DiscountType.PERCENTAGE
                ApiDiscountType.AMOUNT -> DiscountType.AMOUNT
                ApiDiscountType.FIXED_PRICE -> DiscountType.FIXED_PRICE
                null -> DiscountType.PERCENTAGE
            }
        }

        private fun mapReferralSource(apiReferralSource: ApiReferralSource?): ReferralSource? {
            return when (apiReferralSource) {
                ApiReferralSource.REGULAR_CUSTOMER -> ReferralSource.REGULAR_CUSTOMER
                ApiReferralSource.RECOMMENDATION -> ReferralSource.RECOMMENDATION
                ApiReferralSource.SEARCH_ENGINE -> ReferralSource.SEARCH_ENGINE
                ApiReferralSource.SOCIAL_MEDIA -> ReferralSource.SOCIAL_MEDIA
                ApiReferralSource.LOCAL_AD -> ReferralSource.LOCAL_AD
                ApiReferralSource.OTHER -> ReferralSource.OTHER
                null -> null
            }
        }
    }
}