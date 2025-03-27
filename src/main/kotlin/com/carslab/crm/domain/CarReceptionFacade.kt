package com.carslab.crm.domain

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.model.stats.ClientStats
import com.carslab.crm.domain.port.*
import com.carslab.crm.domain.protocol.NewClientCreator
import com.carslab.crm.domain.protocol.NewVehicleCreator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Implementacja serwisu dla protokołów przyjęcia pojazdów.
 */
@Service
class CarReceptionService(
    private val carReceptionRepository: CarReceptionRepository,
    private val newClientCreator: NewClientCreator,
    private val newVehicleCreator: NewVehicleCreator,
    private val clientStatisticsRepository: ClientStatisticsRepository,
    private val vehicleStatisticsRepository: VehicleStatisticsRepository,
    private val vehicleRepository: VehicleRepository,
) {

    private val logger = LoggerFactory.getLogger(CarReceptionService::class.java)

    fun createProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        logger.info("Creating new protocol for vehicle: ${protocol.vehicle.make} ${protocol.vehicle.model} (${protocol.vehicle.licensePlate})")

        validateProtocol(protocol)

        val client = newClientCreator.getClient(protocol)
        val savedProtocol: CarReceptionProtocol = carReceptionRepository.save(protocol.copy(client = protocol.client.copy(id = client.id.value)))

        newVehicleCreator.getVehicle(savedProtocol)

        logger.info("Created protocol with ID: ${savedProtocol.id.value}")
        return savedProtocol
    }

    fun updateProtocol(protocol: CarReceptionProtocol): CarReceptionProtocol {
        logger.info("Updating protocol with ID: ${protocol.id.value}")

        // Sprawdzamy czy protokół istnieje
        val existingProtocol = carReceptionRepository.findById(protocol.id)
            ?: throw IllegalArgumentException("Protocol with ID ${protocol.id.value} not found")

        // Tutaj możemy dodać logikę biznesową, walidację, itp.
        validateProtocol(protocol)

        // Aktualizujemy informacje audytowe
        val updatedProtocol = protocol.copy(
            audit = protocol.audit.copy(
                updatedAt = LocalDateTime.now(),
                statusUpdatedAt = if (protocol.status != existingProtocol.status) {
                    LocalDateTime.now()
                } else {
                    existingProtocol.audit.statusUpdatedAt
                }
            )
        )

        // Zapisujemy zaktualizowany protokół
        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Updated protocol with ID: ${savedProtocol.id.value}")
        return savedProtocol
    }

    fun changeStatus(protocolId: ProtocolId, newStatus: ProtocolStatus): CarReceptionProtocol {
        logger.info("Changing status of protocol ${protocolId.value} to $newStatus")

        // Sprawdzamy czy protokół istnieje
        val existingProtocol = carReceptionRepository.findById(protocolId)
            ?: throw IllegalArgumentException("Protocol with ID ${protocolId.value} not found")



        // Aktualizujemy status i informacje audytowe
        val now = LocalDateTime.now()
        val updatedProtocol = existingProtocol.copy(
            status = newStatus,
            audit = existingProtocol.audit.copy(
                updatedAt = now,
                statusUpdatedAt = now
            )
        )

        if(updatedProtocol.status == ProtocolStatus.COMPLETED) {
            val clientStats = clientStatisticsRepository.findById(ClientId(updatedProtocol.client.id!!))
                ?: ClientStats(updatedProtocol.client.id, 0, "0".toBigDecimal(), 0)
            val incr = clientStats.copy(
                visitNo = clientStats.visitNo + 1,
                gmv = clientStats.gmv + updatedProtocol.services.sumOf { it.finalPrice.amount }.toBigDecimal()
            )
            clientStatisticsRepository.save(incr)


            val vehicle = vehicleRepository.findByVinOrLicensePlate(updatedProtocol.vehicle.vin, updatedProtocol.vehicle.licensePlate)
            val stats = vehicleStatisticsRepository.findById(vehicle!!.id)
            val updatedStats = stats.copy(
                visitNo = stats.visitNo + 1,
                gmv = stats.gmv + updatedProtocol.services.sumOf { it.finalPrice.amount }.toBigDecimal()
            )
            vehicleStatisticsRepository.save(updatedStats)
        }

        // Zapisujemy zaktualizowany protokół
        val savedProtocol = carReceptionRepository.save(updatedProtocol)
        logger.info("Status of protocol ${savedProtocol.id.value} changed to ${savedProtocol.status}")
        return savedProtocol
    }

    fun getProtocolById(protocolId: ProtocolId): CarReceptionProtocol? {
        logger.debug("Getting protocol by ID: ${protocolId.value}")
        return carReceptionRepository.findById(protocolId)
    }

    fun getAllProtocols(): List<CarReceptionProtocol> {
        logger.debug("Getting all protocols")
        return carReceptionRepository.findAll()
    }

    fun searchProtocols(
        clientName: String?,
        licensePlate: String?,
        status: ProtocolStatus?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<CarReceptionProtocol> {
        logger.debug("Searching protocols with filters: clientName=$clientName, licensePlate=$licensePlate, status=$status, startDate=$startDate, endDate=$endDate")

        // Pobieramy wszystkie protokoły, a następnie filtrujemy je według podanych kryteriów
        var result = carReceptionRepository.findAll()

        // Filtrowanie według nazwy klienta
        if (!clientName.isNullOrBlank()) {
            result = result.filter {
                it.client.name.contains(clientName, ignoreCase = true) ||
                        it.client.companyName?.contains(clientName, ignoreCase = true) == true
            }
        }

        // Filtrowanie według numeru rejestracyjnego
        if (!licensePlate.isNullOrBlank()) {
            result = result.filter {
                it.vehicle.licensePlate.contains(licensePlate, ignoreCase = true)
            }
        }

        // Filtrowanie według statusu
        if (status != null) {
            result = result.filter { it.status == status }
        }

        // Filtrowanie według daty początkowej
        if (startDate != null) {
            result = result.filter {
                it.period.startDate >= startDate || it.period.endDate >= startDate
            }
        }

        // Filtrowanie według daty końcowej
        if (endDate != null) {
            result = result.filter {
                it.period.startDate <= endDate || it.period.endDate <= endDate
            }
        }

        logger.debug("Found ${result.size} protocols matching filters")
        return result
    }

    fun deleteProtocol(protocolId: ProtocolId): Boolean {
        logger.info("Deleting protocol with ID: ${protocolId.value}")
        return carReceptionRepository.deleteById(protocolId)
    }

    // Prywatna metoda pomocnicza do walidacji protokołu
    private fun validateProtocol(protocol: CarReceptionProtocol) {
        // Sprawdzamy czy daty są prawidłowe
        if (protocol.period.endDate < protocol.period.startDate) {
            throw IllegalArgumentException("End date cannot be before start date")
        }

        // Sprawdzamy czy klient ma prawidłowe dane kontaktowe
        if (!protocol.client.hasValidContactInfo()) {
            throw IllegalArgumentException("Client must have at least one contact method (email or phone)")
        }

        // Sprawdzamy czy pojazd ma wszystkie wymagane dane
        if (protocol.vehicle.make.isBlank() || protocol.vehicle.model.isBlank() || protocol.vehicle.licensePlate.isBlank()) {
            throw IllegalArgumentException("Vehicle make, model and license plate are required")
        }
    }
}