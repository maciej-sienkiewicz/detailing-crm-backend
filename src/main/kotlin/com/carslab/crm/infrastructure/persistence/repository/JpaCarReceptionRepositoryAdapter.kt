package com.carslab.crm.infrastructure.persistence.adapter

import com.carslab.crm.domain.model.CarReceptionProtocol
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.domain.model.ProtocolStatus
import com.carslab.crm.domain.model.create.protocol.CreateProtocolRootModel
import com.carslab.crm.domain.model.view.protocol.ProtocolView
import com.carslab.crm.domain.port.CarReceptionRepository
import com.carslab.crm.infrastructure.persistence.entity.ClientEntity
import com.carslab.crm.infrastructure.persistence.entity.ProtocolEntity
import com.carslab.crm.infrastructure.persistence.entity.VehicleEntity
import com.carslab.crm.infrastructure.persistence.repository.ClientJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.ProtocolJpaRepository
import com.carslab.crm.infrastructure.persistence.repository.VehicleJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import jakarta.persistence.criteria.Predicate

@Repository
class JpaCarReceptionRepositoryAdapter(
    private val protocolJpaRepository: ProtocolJpaRepository,
    private val vehicleJpaRepository: VehicleJpaRepository,
    private val clientJpaRepository: ClientJpaRepository
) : CarReceptionRepository {

    override fun save(protocol: CreateProtocolRootModel): ProtocolId {
        val vehicleId = protocol.vehicle.id?.toLong()
            ?: throw IllegalStateException("Vehicle ID is required")

        val clientId = protocol.client.id?.toLong()
            ?: throw IllegalStateException("Client ID is required")

        // Sprawdzamy tylko, czy encje istnieją
        if (!vehicleJpaRepository.existsById(vehicleId)) {
            throw IllegalStateException("Vehicle with ID $vehicleId not found")
        }

        if (!clientJpaRepository.existsById(clientId)) {
            throw IllegalStateException("Client with ID $clientId not found")
        }

        val protocolEntity = ProtocolEntity(
            title = protocol.title,
            vehicleId = vehicleId,
            clientId = clientId,
            startDate = protocol.period.startDate,
            endDate = protocol.period.endDate,
            status = protocol.status,
            notes = protocol.notes,
            referralSource = protocol.referralSource,
            otherSourceDetails = protocol.otherSourceDetails,
            keysProvided = protocol.documents.keysProvided,
            documentsProvided = protocol.documents.documentsProvided,
            appointmentId = protocol.audit.appointmentId,
            createdAt = protocol.audit.createdAt,
            updatedAt = protocol.audit.updatedAt,
            statusUpdatedAt = protocol.audit.statusUpdatedAt,
            calendarColorId = protocol.calendarColorId.value
        )

        val savedEntity = protocolJpaRepository.save(protocolEntity)
        return ProtocolId(savedEntity.id.toString())
    }

    override fun save(protocol: CarReceptionProtocol): CarReceptionProtocol {
        val vehicleId = vehicleJpaRepository.findByVinOrLicensePlate(
            protocol.vehicle.vin,
            protocol.vehicle.licensePlate
        )?.id  ?: throw IllegalStateException("Vehicle ID is required")


        val clientId = protocol.client.id
            ?: throw IllegalStateException("Client ID is required")

        if (!clientJpaRepository.existsById(clientId)) {
            throw IllegalStateException("Client with ID $clientId not found")
        }

        // Sprawdź, czy protokół istnieje
        val protocolEntity = if (protocolJpaRepository.existsById(protocol.id.value)) {
            val existingEntity = protocolJpaRepository.findById(protocol.id.value).get()

            // Aktualizuj istniejącą encję
            existingEntity.title = protocol.title
            existingEntity.startDate = protocol.period.startDate
            existingEntity.endDate = protocol.period.endDate
            existingEntity.status = protocol.status
            existingEntity.notes = protocol.notes
            existingEntity.referralSource = protocol.referralSource
            existingEntity.otherSourceDetails = protocol.otherSourceDetails
            existingEntity.keysProvided = protocol.documents.keysProvided
            existingEntity.documentsProvided = protocol.documents.documentsProvided
            existingEntity.appointmentId = protocol.audit.appointmentId
            existingEntity.updatedAt = protocol.audit.updatedAt
            existingEntity.statusUpdatedAt = protocol.audit.statusUpdatedAt
            existingEntity.calendarColorId = protocol.calendarColorId.value

            existingEntity
        } else {
            // Utwórz nową encję
            ProtocolEntity(
                id = protocol.id.value.toLong(),
                title = protocol.title,
                vehicleId = vehicleId,
                clientId = clientId,
                startDate = protocol.period.startDate,
                endDate = protocol.period.endDate,
                status = protocol.status,
                notes = protocol.notes,
                referralSource = protocol.referralSource,
                otherSourceDetails = protocol.otherSourceDetails,
                keysProvided = protocol.documents.keysProvided,
                documentsProvided = protocol.documents.documentsProvided,
                appointmentId = protocol.audit.appointmentId,
                createdAt = protocol.audit.createdAt,
                updatedAt = protocol.audit.updatedAt,
                statusUpdatedAt = protocol.audit.statusUpdatedAt,
                calendarColorId = protocol.calendarColorId.value,
            )
        }

        protocolJpaRepository.save(protocolEntity)

        // Dla uproszczenia, zwróć oryginalny protokół
        return protocol
    }

    override fun findById(id: ProtocolId): ProtocolView? {
        return protocolJpaRepository.findById(id.value)
            .map { it.toDomainView() }
            .orElse(null)
    }

    override fun findAll(): List<CarReceptionProtocol> {
        // Dla uproszczenia, zwracamy pustą listę
        return emptyList()
    }

    override fun findByStatus(status: ProtocolStatus): List<CarReceptionProtocol> {
        // Dla uproszczenia, zwracamy pustą listę
        return emptyList()
    }

    override fun findByClientName(clientName: String): List<CarReceptionProtocol> {
        // Użycie nowej metody z repozytorium z poprawnym zapytaniem JPQL
        // Dla uproszczenia, zwracamy pustą listę
        return emptyList()
    }

    override fun findByLicensePlate(licensePlate: String): List<CarReceptionProtocol> {
        // Użycie nowej metody z repozytorium z poprawnym zapytaniem JPQL
        // Dla uproszczenia, zwracamy pustą listę
        return emptyList()
    }

    override fun deleteById(id: ProtocolId): Boolean {
        return if (protocolJpaRepository.existsById(id.value)) {
            protocolJpaRepository.deleteById(id.value)
            true
        } else {
            false
        }
    }

    override fun searchProtocols(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<ProtocolView> {
        return protocolJpaRepository.findAll().map { it.toDomainView() }
    }

    override fun searchProtocolsWithPagination(
        clientName: String?,
        clientId: Long?,
        licensePlate: String?,
        status: ProtocolStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        page: Int,
        size: Int
    ): Pair<List<ProtocolView>, Long> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        // Używamy Specification API do budowania dynamicznego zapytania
        val specification = Specification<ProtocolEntity> { root, query, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            // Join do powiązanych tabel jeśli jest to konieczne
            val clientJoin = if (clientName != null) {
                root.join<ProtocolEntity, ClientEntity>("client")
            } else {
                null
            }

            val vehicleJoin = if (licensePlate != null) {
                root.join<ProtocolEntity, VehicleEntity>("vehicle")
            } else {
                null
            }

            // Dodanie warunków filtrowania
            clientName?.let {
                val firstNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(clientJoin!!.get("firstName")),
                    "%" + it.lowercase() + "%"
                )
                val lastNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(clientJoin!!.get("lastName")),
                    "%" + it.lowercase() + "%"
                )
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate))
            }

            clientId?.let {
                predicates.add(criteriaBuilder.equal(root.get<Long>("clientId"), it))
            }

            licensePlate?.let {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(vehicleJoin!!.get("licensePlate")),
                        "%" + it.lowercase() + "%"
                    )
                )
            }

            status?.let {
                predicates.add(criteriaBuilder.equal(root.get<ProtocolStatus>("status"), it))
            }

            startDate?.let {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), it))
            }

            endDate?.let {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), it))
            }

            // Łączymy wszystkie predykaty operatorem AND
            if (predicates.isEmpty()) {
                null
            } else {
                criteriaBuilder.and(*predicates.toTypedArray())
            }
        }

        // Pobieramy stronę wyników
        val protocolPage = protocolJpaRepository.findAll(specification, pageable)

        // Mapujemy wyniki na obiekty domeny
        val protocolViews = protocolPage.content.map { it.toDomainView() }

        return Pair(protocolViews, protocolPage.totalElements)
    }
}