package com.carslab.crm.production.modules.vehicles.application.service

import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.production.modules.media.application.service.MediaCommandService
import com.carslab.crm.production.modules.visits.application.dto.MediaUploadResponse
import com.carslab.crm.production.modules.vehicles.domain.model.VehicleId
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleAccessValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
 * Serwis do zarządzania zdjęciami pojazdu
 */
@Service
@Transactional
class VehicleMediaCommandService(
    private val mediaCommandService: MediaCommandService,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val securityContext: SecurityContext
) {
    private val logger = LoggerFactory.getLogger(VehicleMediaCommandService::class.java)

    /**
     * Upload zdjęcia bezpośrednio do pojazdu
     */
    fun uploadVehicleImage(vehicleId: String, request: MultipartHttpServletRequest): MediaUploadResponse {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Uploading image to vehicle: {} for company: {}", vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        val response = mediaCommandService.uploadMediaForVehicle(vehicleId, request)

        logger.info("Image uploaded successfully to vehicle: {} with media ID: {}", vehicleId, response.id)

        return MediaUploadResponse(
            mediaId = response.id,
            protocolId = null // Nie jest to upload do wizyty
        )
    }

    /**
     * Usuń zdjęcie pojazdu
     */
    fun deleteVehicleImage(vehicleId: String, mediaId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting image: {} from vehicle: {} for company: {}", mediaId, vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        mediaCommandService.deleteMedia(mediaId)

        logger.info("Image deleted successfully: {} from vehicle: {}", mediaId, vehicleId)
    }

    /**
     * Usuń wszystkie zdjęcia pojazdu
     */
    fun deleteAllVehicleImages(vehicleId: String) {
        val companyId = securityContext.getCurrentCompanyId()
        logger.info("Deleting all images from vehicle: {} for company: {}", vehicleId, companyId)

        // Walidacja dostępu do pojazdu
        vehicleAccessValidator.getVehicleForCompany(VehicleId.of(vehicleId.toLong()), companyId)

        mediaCommandService.deleteAllVehicleMedia(vehicleId)

        logger.info("All images deleted successfully from vehicle: {}", vehicleId)
    }
}