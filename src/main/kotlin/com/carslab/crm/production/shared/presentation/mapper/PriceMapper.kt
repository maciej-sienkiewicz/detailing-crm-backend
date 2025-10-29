package com.carslab.crm.production.shared.presentation.mapper

import com.carslab.crm.production.shared.domain.value_objects.PriceType
import com.carslab.crm.production.shared.presentation.dto.PriceTypeDto

object PriceMapper {

    fun toDomain(dto: PriceTypeDto): PriceType {
        return when (dto) {
            PriceTypeDto.NETTO -> PriceType.NETTO
            PriceTypeDto.BRUTTO -> PriceType.BRUTTO
        }
    }

    fun toDto(domain: PriceType): PriceTypeDto {
        return when (domain) {
            PriceType.NETTO -> PriceTypeDto.NETTO
            PriceType.BRUTTO -> PriceTypeDto.BRUTTO
        }
    }
}