package com.carslab.crm.production.modules.reservations.domain.models.enums

enum class ReservationStatus {
    PENDING,      // Oczekuje na potwierdzenie
    CONFIRMED,    // Potwierdzona - gotowa do konwersji
    CONVERTED,    // Przekonwertowana na wizytę
    CANCELLED;    // Anulowana

    fun canTransitionTo(newStatus: ReservationStatus): Boolean {
        return when (this) {
            PENDING -> newStatus in listOf(CONFIRMED, CANCELLED)
            CONFIRMED -> newStatus in listOf(CONVERTED, CANCELLED)
            CONVERTED -> false // Nie można zmienić po konwersji
            CANCELLED -> false // Nie można zmienić po anulowaniu
        }
    }
}