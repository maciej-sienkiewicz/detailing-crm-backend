package com.carslab.crm.production.shared.presentation.mapper

import com.carslab.crm.production.shared.domain.value_objects.DiscountType
import com.carslab.crm.production.shared.domain.value_objects.DiscountValueObject
import com.carslab.crm.production.shared.domain.value_objects.PriceValueObject
import com.carslab.crm.production.shared.presentation.dto.DiscountDto
import com.carslab.crm.production.shared.presentation.dto.DiscountResponseDto
import com.carslab.crm.production.shared.presentation.dto.DiscountTypeDto
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto

/**
 * Mapper między DTOs a obiektami domenowymi dla rabatów.
 *
 * Odpowiedzialny za transformację danych między warstwą prezentacji a domeną.
 */
object DiscountMapper {

    /**
     * Mapuje DTO typu rabatu na typ domenowy.
     */
    fun toDomain(dto: DiscountTypeDto): DiscountType {
        return when (dto) {
            DiscountTypeDto.PERCENT -> DiscountType.PERCENT
            DiscountTypeDto.FIXED_AMOUNT_OFF_BRUTTO -> DiscountType.FIXED_AMOUNT_OFF_BRUTTO
            DiscountTypeDto.FIXED_AMOUNT_OFF_NETTO -> DiscountType.FIXED_AMOUNT_OFF_NETTO
            DiscountTypeDto.FIXED_FINAL_BRUTTO -> DiscountType.FIXED_FINAL_BRUTTO
            DiscountTypeDto.FIXED_FINAL_NETTO -> DiscountType.FIXED_FINAL_NETTO
        }
    }

    /**
     * Mapuje typ domenowy na DTO.
     */
    fun toDto(domain: DiscountType): DiscountTypeDto {
        return when (domain) {
            DiscountType.PERCENT -> DiscountTypeDto.PERCENT
            DiscountType.FIXED_AMOUNT_OFF_BRUTTO -> DiscountTypeDto.FIXED_AMOUNT_OFF_BRUTTO
            DiscountType.FIXED_AMOUNT_OFF_NETTO -> DiscountTypeDto.FIXED_AMOUNT_OFF_NETTO
            DiscountType.FIXED_FINAL_BRUTTO -> DiscountTypeDto.FIXED_FINAL_BRUTTO
            DiscountType.FIXED_FINAL_NETTO -> DiscountTypeDto.FIXED_FINAL_NETTO
        }
    }

    /**
     * Mapuje DTO rabatu na obiekt domenowy.
     *
     * Waliduje dane i tworzy odpowiedni Value Object.
     */
    fun toDomain(dto: DiscountDto): DiscountValueObject {
        val type = toDomain(dto.discountType)
        return DiscountValueObject.fromRaw(
            type = type,
            value = dto.discountValue,
        )
    }

    /**
     * Mapuje obiekt domenowy na DTO odpowiedzi.
     *
     * @param discount Obiekt rabatu domenowego
     * @param basePrice Cena bazowa (do obliczenia oszczędności)
     * @param vatRate Stawka VAT
     */
    fun toResponseDto(
        discount: DiscountValueObject,
        basePrice: PriceValueObject,
        vatRate: Int
    ): DiscountResponseDto {
        val savings = discount.calculateSavings(basePrice, vatRate)

        return DiscountResponseDto(
            discountType = toDto(discount.type),
            discountValue = discount.value,
            savings = PriceResponseDto.from(savings)
        )
    }

    /**
     * Mapuje opcjonalny DTO na opcjonalny obiekt domenowy.
     */
    fun toDomainOrNull(dto: DiscountDto?): DiscountValueObject? {
        return dto?.let { toDomain(it) }
    }

    /**
     * Mapuje opcjonalny obiekt domenowy na opcjonalny DTO.
     */
    fun toResponseDtoOrNull(
        discount: DiscountValueObject?,
        basePrice: PriceValueObject,
        vatRate: Int
    ): DiscountResponseDto? {
        return discount?.let { toResponseDto(it, basePrice, vatRate) }
    }
}