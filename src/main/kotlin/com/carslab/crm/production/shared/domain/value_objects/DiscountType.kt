package com.carslab.crm.production.shared.domain.value_objects

/**
 * Typy rabatów obsługiwane przez system.
 *
 * System wspiera 5 głównych scenariuszy rabatowych:
 * - Rabat procentowy
 * - Stała obniżka (brutto lub netto)
 * - Sztywna cena finalna (brutto lub netto)
 */
enum class DiscountType {
    /**
     * Rabat procentowy od ceny bazowej.
     * Przykład: 10% zniżki od 100 zł brutto = 90 zł brutto
     */
    PERCENT,

    /**
     * Stała obniżka wyrażona w kwocie brutto.
     * Przykład: Obniżka o 50 zł brutto od ceny bazowej 200 zł brutto = 150 zł brutto
     */
    FIXED_AMOUNT_OFF_BRUTTO,

    /**
     * Stała obniżka wyrażona w kwocie netto.
     * Przykład: Obniżka o 40 zł netto od ceny bazowej
     */
    FIXED_AMOUNT_OFF_NETTO,

    /**
     * Ustawienie sztywnej ceny końcowej brutto.
     * Przykład: Ustaw finalną cenę na 100 zł brutto (niezależnie od ceny bazowej)
     */
    FIXED_FINAL_BRUTTO,

    /**
     * Ustawienie sztywnej ceny końcowej netto.
     * Przykład: Ustaw finalną cenę na 80 zł netto (niezależnie od ceny bazowej)
     */
    FIXED_FINAL_NETTO;

    /**
     * Sprawdza czy rabat wymaga wartości kwotowej (a nie procentowej).
     */
    fun isAmountBased(): Boolean {
        return this in listOf(
            FIXED_AMOUNT_OFF_BRUTTO,
            FIXED_AMOUNT_OFF_NETTO,
            FIXED_FINAL_BRUTTO,
            FIXED_FINAL_NETTO
        )
    }

    /**
     * Sprawdza czy rabat ustawia finalną cenę (a nie obniża istniejącą).
     */
    fun isFinalPriceSetting(): Boolean {
        return this in listOf(FIXED_FINAL_BRUTTO, FIXED_FINAL_NETTO)
    }

    /**
     * Sprawdza czy rabat operuje na wartościach brutto.
     */
    fun isBruttoBased(): Boolean {
        return this in listOf(FIXED_AMOUNT_OFF_BRUTTO, FIXED_FINAL_BRUTTO)
    }

    /**
     * Sprawdza czy rabat operuje na wartościach netto.
     */
    fun isNettoBased(): Boolean {
        return this in listOf(FIXED_AMOUNT_OFF_NETTO, FIXED_FINAL_NETTO)
    }
}