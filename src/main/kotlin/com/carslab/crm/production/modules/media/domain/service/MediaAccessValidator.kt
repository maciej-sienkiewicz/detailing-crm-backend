package com.carslab.crm.production.modules.media.domain.service

import com.carslab.crm.production.modules.media.domain.model.Media
import com.carslab.crm.production.modules.media.domain.model.MediaId
import com.carslab.crm.production.modules.media.domain.repository.MediaRepository
import com.carslab.crm.production.modules.vehicles.domain.service.VehicleAccessValidator
import com.carslab.crm.production.modules.visits.domain.repositories.VisitRepository
import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Walidator dostępu do mediów
 */
@Component
class MediaAccessValidator(
    private val mediaRepository: MediaRepository,
    private val vehicleAccessValidator: VehicleAccessValidator,
    private val visitRepository: VisitRepository
) {
    private val logger = LoggerFactory.getLogger(MediaAccessValidator::class.java)

    /**
     * Waliduje dostęp do media i zwraca je jeśli dostęp jest dozwolony
     */
    fun validateMediaAccess(mediaId: MediaId, companyId: Long): Media {
        val media = mediaRepository.findById(mediaId)
            ?: throw EntityNotFoundException("Media not found: ${mediaId.value}")

        if (!media.canBeAccessedBy(companyId)) {
            throw BusinessException("Access denied to media: ${mediaId.value}")
        }

        // Dodatkowa walidacja w zależności od kontekstu
        when (media.context) {
            com.carslab.crm.production.modules.media.domain.model.MediaContext.VEHICLE -> {
                // Sprawdź dostęp do pojazdu
                media.vehicleId?.let { vehicleId ->
                    vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)
                }
            }

            com.carslab.crm.production.modules.media.domain.model.MediaContext.VISIT -> {
                // Sprawdź dostęp do wizyty
                media.visitId?.let { visitId ->
                    val visit = visitRepository.findById(visitId, companyId)
                        ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")

                    if (visit.companyId != companyId) {
                        throw BusinessException("Access denied to visit: ${visitId.value}")
                    }
                }
            }

            com.carslab.crm.production.modules.media.domain.model.MediaContext.STANDALONE -> {
                // Dla standalone wystarczy sprawdzenie company_id (już wykonane wyżej)
            }
        }

        logger.debug("Media access validated for: {} by company: {}", mediaId.value, companyId)
        return media
    }

    /**
     * Waliduje dostęp do pojazdu dla operacji na mediach
     */
    fun validateVehicleMediaAccess(
        vehicleId: com.carslab.crm.production.modules.vehicles.domain.model.VehicleId,
        companyId: Long
    ) {
        vehicleAccessValidator.getVehicleForCompany(vehicleId, companyId)
        logger.debug("Vehicle media access validated for vehicle: {} by company: {}", vehicleId.value, companyId)
    }

    /**
     * Waliduje dostęp do wizyty dla operacji na mediach
     */
    fun validateVisitMediaAccess(
        visitId: com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId,
        companyId: Long
    ) {
        val visit = visitRepository.findById(visitId, companyId)
            ?: throw EntityNotFoundException("Visit not found: ${visitId.value}")

        if (visit.companyId != companyId) {
            throw BusinessException("Access denied to visit: ${visitId.value}")
        }

        logger.debug("Visit media access validated for visit: {} by company: {}", visitId.value, companyId)
    }

    /**
     * Sprawdza czy media należy do określonej firmy (bez rzucania wyjątków)
     */
    fun hasMediaAccess(mediaId: MediaId, companyId: Long): Boolean {
        return try {
            validateMediaAccess(mediaId, companyId)
            true
        } catch (e: Exception) {
            logger.debug("Media access denied for: {} by company: {}", mediaId.value, companyId)
            false
        }
    }

    /**
     * Sprawdza czy firma ma dostęp do pojazdu (bez rzucania wyjątków)
     */
    fun hasVehicleAccess(
        vehicleId: com.carslab.crm.production.modules.vehicles.domain.model.VehicleId,
        companyId: Long
    ): Boolean {
        return try {
            validateVehicleMediaAccess(vehicleId, companyId)
            true
        } catch (e: Exception) {
            logger.debug("Vehicle access denied for: {} by company: {}", vehicleId.value, companyId)
            false
        }
    }

    /**
     * Sprawdza czy firma ma dostęp do wizyty (bez rzucania wyjątków)
     */
    fun hasVisitAccess(
        visitId: com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId,
        companyId: Long
    ): Boolean {
        return try {
            validateVisitMediaAccess(visitId, companyId)
            true
        } catch (e: Exception) {
            logger.debug("Visit access denied for: {} by company: {}", visitId.value, companyId)
            false
        }
    }
}