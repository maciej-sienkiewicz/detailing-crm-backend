package com.carslab.crm.production.modules.media.domain.model

enum class MediaContext {
    /**
     * Zdjęcie przypisane do wizyty
     */
    VISIT,

    /**
     * Zdjęcie przypisane bezpośrednio do pojazdu
     */
    VEHICLE,

    /**
     * Zdjęcie niezależne (dla przyszłego użytku)
     */
    STANDALONE;

    fun isVisit(): Boolean = this == VISIT
    fun isVehicle(): Boolean = this == VEHICLE
    fun isStandalone(): Boolean = this == STANDALONE
}