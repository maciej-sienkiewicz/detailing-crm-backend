package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.media.application.mapper.MediaResponseMapper
import com.carslab.crm.production.modules.media.application.service.MediaQueryService
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleAccessValidator
import com.carslab.crm.production.modules.visits.application.dto.VisitMediaResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Serwis do pobierania zdjęć pojazdu
 */
@Service
@Transactional(readOnly = true)
class VehicleMediaQueryService(
    private val mediaQueryService: MediaQueryService,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VehicleMediaQueryService::class.java)

    /**
     * Pobiera wszystkie zdjęcia pojazdu (bezpośrednio przypisane + z wizyt)
     */
    fun getAllVehicleImages(vehicleId: String): List<VisitMediaResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting all images for vehicle: {} and company: {}", vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        val mediaResponses = mediaQueryService.getAllVehicleMedia(vehicleId)

        // Konwersja do VisitMediaResponse dla kompatybilności z istniejącym API
        val visitMediaResponses = mediaResponses.map { media ->
            MediaResponseMapper.toVisitMediaResponse(media)
        }

        logger.debug("Retrieved {} images for vehicle: {}", visitMediaResponses.size, vehicleId)
        return visitMediaResponses
    }

    /**
     * Pobiera tylko zdjęcia bezpośrednio przypisane do pojazdu (bez zdjęć z wizyt)
     */
    fun getDirectVehicleImages(vehicleId: String): List<VisitMediaResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting direct images for vehicle: {} and company: {}", vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        val allMedia = mediaQueryService.getAllVehicleMedia(vehicleId)

        // Filtrowanie tylko zdjęć bezpośrednio przypisanych (context = VEHICLE)
        val directMediaResponses = allMedia
            .filter { it.context == "VEHICLE" }
            .map { media ->
                MediaResponseMapper.toVisitMediaResponse(media)
            }

        logger.debug("Retrieved {} direct images for vehicle: {}", directMediaResponses.size, vehicleId)
        return directMediaResponses
    }

    /**
     * Pobiera tylko zdjęcia z wizyt dla pojazdu
     */
    fun getVehicleVisitImages(vehicleId: String): List<VisitMediaResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Getting visit images for vehicle: {} and company: {}", vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        val allMedia = mediaQueryService.getAllVehicleMedia(vehicleId)

        // Filtrowanie tylko zdjęć z wizyt (context = VISIT)
        val visitMediaResponses = allMedia
            .filter { it.context == "VISIT" }
            .map { media ->
                MediaResponseMapper.toVisitMediaResponse(media)
            }

        logger.debug("Retrieved {} visit images for vehicle: {}", visitMediaResponses.size, vehicleId)
        return visitMediaResponses
    }

    /**
     * Liczy wszystkie zdjęcia pojazdu
     */
    fun countAllVehicleImages(vehicleId: String): Long {
        val companyId = securityContext.getCurrentCompanyId()
        logger.debug("Counting all images for vehicle: {} and company: {}", vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        val count = mediaQueryService.getAllVehicleMedia(vehicleId).size.toLong()

        logger.debug("Vehicle: {} has {} images total", vehicleId, count)
        return count
    }

    /**
     * Sprawdza czy pojazd ma jakieś zdjęcia
     */
    fun hasVehicleImages(vehicleId: String): Boolean {
        return countAllVehicleImages(vehicleId) > 0
    }
}