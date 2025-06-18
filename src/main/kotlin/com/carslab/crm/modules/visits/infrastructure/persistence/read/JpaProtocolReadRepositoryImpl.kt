// src/main/kotlin/com/carslab/crm/modules/visits/infrastructure/persistence/read/JpaProtocolReadRepositoryImpl.kt
package com.carslab.crm.modules.visits.infrastructure.persistence.read

import com.carslab.crm.modules.visits.application.queries.models.*
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.api.model.response.PaginatedResponse
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.modules.clients.infrastructure.persistence.repository.VehicleJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolServiceJpaRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.modules.visits.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.modules.clients.infrastructure.persistence.entity.VehicleEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Repository
import jakarta.persistence.criteria.Predicate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class JpaProtocolReadRepositoryImpl(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val clientJpaRepository: ClientJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val protocolServiceJpaRepository: ProtocolServiceJpaRepository
) : ProtocolReadRepository {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun findDetailById(protocolId: String): ProtocolDetailReadModel? {
        val companyId = getCurrentCompanyId()
        val protocolEntity = protocolJpaRepository.findByCompanyIdAndId(companyId, protocolId.toLong())
            .orElse(null) ?: return null

        val client = clientJpaRepository.findByIdAndCompanyId(companyId, protocolEntity.clientId)
            .orElse(null) ?: return null

        val vehicle = vehicleJpaRepository.findByIdAndCompanyId(companyId, protocolEntity.vehicleId)
            .orElse(null) ?: return null

        val services = protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolEntity.id!!, companyId)

        return ProtocolDetailReadModel(
            id = protocolEntity.id.toString(),
            title = protocolEntity.title,
            calendarColorId = protocolEntity.calendarColorId,
            vehicle = toVehicleReadModel(vehicle),
            client = toClientReadModel(client),
            period = PeriodReadModel(
                startDate = protocolEntity.startDate.format(DateTimeFormatter.ISO_DATE_TIME),
                endDate = protocolEntity.endDate.format(DateTimeFormatter.ISO_DATE_TIME)
            ),
            status = protocolEntity.status.name,
            services = services.map { toServiceReadModel(it.toDomain()) },
            notes = protocolEntity.notes,
            referralSource = protocolEntity.referralSource?.name,
            otherSourceDetails = protocolEntity.otherSourceDetails,
            documents = DocumentsReadModel(
                keysProvided = protocolEntity.keysProvided,
                documentsProvided = protocolEntity.documentsProvided
            ),
            mediaItems = emptyList(), // TODO: implement when needed
            audit = AuditReadModel(
                createdAt = protocolEntity.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
                updatedAt = protocolEntity.updatedAt.format(DateTimeFormatter.ISO_DATE_TIME),
                statusUpdatedAt = protocolEntity.statusUpdatedAt.format(DateTimeFormatter.ISO_DATE_TIME)
            ),
            appointmentId = protocolEntity.appointmentId
        )
    }

    override fun searchProtocols(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        make: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        page: Int,
        size: Int
    ): PaginatedResponse<ProtocolListReadModel> {
        val companyId = getCurrentCompanyId()
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val specification = Specification<ProtocolEntity> { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            // Company filter
            predicates.add(cb.equal(root.get<Long>("companyId"), companyId))

            // Client name filter
            if (clientName != null) {
                val clientSubquery = query!!.subquery(ClientEntity::class.java)
                val clientRoot = clientSubquery.from(ClientEntity::class.java)
                val clientPredicates = mutableListOf<Predicate>()
                clientPredicates.add(cb.equal(clientRoot.get<Long>("id"), root.get<Long>("clientId")))
                val firstNamePredicate = cb.like(cb.lower(clientRoot.get("firstName")), "%" + clientName.lowercase() + "%")
                val lastNamePredicate = cb.like(cb.lower(clientRoot.get("lastName")), "%" + clientName.lowercase() + "%")
                clientPredicates.add(cb.or(firstNamePredicate, lastNamePredicate))
                clientSubquery.where(*clientPredicates.toTypedArray())
                predicates.add(cb.exists(clientSubquery))
            }

            // Client ID filter
            clientId?.let {
                predicates.add(cb.equal(root.get<Long>("clientId"), it))
            }

            // Vehicle filters
            if (licensePlate != null || make != null) {
                val vehicleSubquery = query!!.subquery(VehicleEntity::class.java)
                val vehicleRoot = vehicleSubquery.from(VehicleEntity::class.java)
                val vehiclePredicates = mutableListOf<Predicate>()
                vehiclePredicates.add(cb.equal(vehicleRoot.get<Long>("id"), root.get<Long>("vehicleId")))

                licensePlate?.let {
                    vehiclePredicates.add(cb.like(cb.lower(vehicleRoot.get("licensePlate")), "%" + it.lowercase() + "%"))
                }

                make?.let {
                    vehiclePredicates.add(cb.like(cb.lower(vehicleRoot.get("make")), "%" + it.lowercase() + "%"))
                }

                vehicleSubquery.where(*vehiclePredicates.toTypedArray())
                predicates.add(cb.exists(vehicleSubquery))
            }

            // Status filter
            status?.let {
                predicates.add(cb.equal(root.get<ProtocolStatus>("status"), it))
            }

            // Date filters
            startDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), it))
            }

            endDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), it))
            }

            cb.and(*predicates.toTypedArray())
        }

        val protocolPage = protocolJpaRepository.findAll(specification, pageable)

        val protocols = protocolPage.content.map { protocolEntity ->
            val client = clientJpaRepository.findByIdAndCompanyId(companyId, protocolEntity.clientId).orElse(null)
            val vehicle = vehicleJpaRepository.findByIdAndCompanyId(companyId, protocolEntity.vehicleId).orElse(null)
            val services = protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolEntity.id!!, companyId)

            ProtocolListReadModel(
                id = protocolEntity.id.toString(),
                title = protocolEntity.title,
                vehicle = VehicleBasicReadModel(
                    make = vehicle?.make ?: "",
                    model = vehicle?.model ?: "",
                    licensePlate = vehicle?.licensePlate ?: "",
                    productionYear = vehicle?.year ?: 0,
                    color = vehicle?.color
                ),
                client = ClientBasicReadModel(
                    name = "${client?.firstName ?: ""} ${client?.lastName ?: ""}".trim(),
                    companyName = client?.company
                ),
                period = PeriodReadModel(
                    startDate = protocolEntity.startDate.format(DateTimeFormatter.ISO_DATE),
                    endDate = protocolEntity.endDate.format(DateTimeFormatter.ISO_DATE)
                ),
                status = protocolEntity.status.name,
                calendarColorId = protocolEntity.calendarColorId,
                totalServiceCount = services.size,
                totalAmount = services.sumOf { it.finalPrice.toDouble() },
                lastUpdate = protocolEntity.updatedAt.format(dateTimeFormatter)
            )
        }

        return PaginatedResponse(
            data = protocols,
            page = page,
            size = size,
            totalItems = protocolPage.totalElements,
            totalPages = protocolPage.totalPages.toLong()
        )
    }

    override fun getCounters(): ProtocolCountersReadModel {
        val companyId = getCurrentCompanyId()

        return ProtocolCountersReadModel(
            scheduled = protocolJpaRepository.countByStatusAndCompanyId(ProtocolStatus.SCHEDULED, companyId),
            inProgress = protocolJpaRepository.countByStatusAndCompanyId(ProtocolStatus.IN_PROGRESS, companyId),
            readyForPickup = protocolJpaRepository.countByStatusAndCompanyId(ProtocolStatus.READY_FOR_PICKUP, companyId),
            completed = protocolJpaRepository.countByStatusAndCompanyId(ProtocolStatus.COMPLETED, companyId),
            cancelled = protocolJpaRepository.countByStatusAndCompanyId(ProtocolStatus.CANCELLED, companyId),
            all = protocolJpaRepository.countByCompanyId(companyId)
        )
    }

    override fun findByClientId(clientId: Long): List<ProtocolListReadModel> {
        val companyId = getCurrentCompanyId()
        val protocols = protocolJpaRepository.findByClientIdAndCompanyId(clientId, companyId)

        return protocols.map { protocolEntity ->
            val client = clientJpaRepository.findByIdAndCompanyId(companyId, protocolEntity.clientId).orElse(null)
            val vehicle = vehicleJpaRepository.findByIdAndCompanyId(companyId, protocolEntity.vehicleId).orElse(null)
            val services = protocolServiceJpaRepository.findByProtocolIdAndCompanyId(protocolEntity.id!!, companyId)

            ProtocolListReadModel(
                id = protocolEntity.id.toString(),
                title = protocolEntity.title,
                vehicle = VehicleBasicReadModel(
                    make = vehicle?.make ?: "",
                    model = vehicle?.model ?: "",
                    licensePlate = vehicle?.licensePlate ?: "",
                    productionYear = vehicle?.year ?: 0,
                    color = vehicle?.color
                ),
                client = ClientBasicReadModel(
                    name = "${client?.firstName ?: ""} ${client?.lastName ?: ""}".trim(),
                    companyName = client?.company
                ),
                period = PeriodReadModel(
                    startDate = protocolEntity.startDate.format(DateTimeFormatter.ISO_DATE),
                    endDate = protocolEntity.endDate.format(DateTimeFormatter.ISO_DATE)
                ),
                status = protocolEntity.status.name,
                calendarColorId = protocolEntity.calendarColorId,
                totalServiceCount = services.size,
                totalAmount = services.sumOf { it.finalPrice.toDouble() },
                lastUpdate = protocolEntity.updatedAt.format(dateTimeFormatter)
            )
        }
    }

    private fun getCurrentCompanyId(): Long {
        return (SecurityContextHolder.getContext().authentication.principal as UserEntity).companyId
    }

    private fun toVehicleReadModel(vehicle: VehicleEntity): VehicleReadModel {
        return VehicleReadModel(
            id = vehicle.id.toString(),
            make = vehicle.make,
            model = vehicle.model,
            licensePlate = vehicle.licensePlate,
            productionYear = vehicle.year ?: 0,
            vin = vehicle.vin,
            color = vehicle.color,
            mileage = vehicle.mileage
        )
    }

    private fun toClientReadModel(client: ClientEntity): ClientReadModel {
        return ClientReadModel(
            id = client.id.toString(),
            name = "${client.firstName} ${client.lastName}".trim(),
            email = client.email,
            phone = client.phone,
            companyName = client.company,
            taxId = client.taxId
        )
    }

    private fun toServiceReadModel(service: com.carslab.crm.domain.model.view.protocol.ProtocolServiceView): ServiceReadModel {
        return ServiceReadModel(
            id = service.id.toString(),
            name = service.name,
            basePrice = service.basePrice.amount,
            quantity = service.quantity,
            discountType = service.discount?.type?.name,
            discountValue = service.discount?.value ?: 0.0,
            finalPrice = service.finalPrice.amount,
            approvalStatus = service.approvalStatus.name,
            note = service.note
        )
    }
}