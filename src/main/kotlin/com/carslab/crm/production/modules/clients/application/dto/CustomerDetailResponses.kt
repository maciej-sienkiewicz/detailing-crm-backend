package com.carslab.crm.production.modules.clients.application.dto

import com.carslab.crm.production.modules.clients.domain.model.Client
import com.carslab.crm.production.modules.clients.domain.model.ClientStatistics
import com.carslab.crm.production.shared.presentation.dto.PriceResponseDto
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main response for customer detail endpoint
 */
data class CustomerDetailResponse(
    val customer: CustomerDetailDto,
    @JsonProperty("marketingConsents")
    val marketingConsents: List<MarketingConsentDto>,
    @JsonProperty("loyaltyTier")
    val loyaltyTier: String,
    @JsonProperty("lifetimeValue")
    val lifetimeValue: CustomerRevenueDto
)

/**
 * Detailed customer information
 */
data class CustomerDetailDto(
    val id: String,
    @JsonProperty("firstName")
    val firstName: String,
    @JsonProperty("lastName")
    val lastName: String,
    val contact: CustomerContactDto,
    @JsonProperty("homeAddress")
    val homeAddress: HomeAddressDto?,
    val company: CompanyDetailsDto?,
    val notes: String,
    @JsonProperty("lastVisitDate")
    val lastVisitDate: String?,
    @JsonProperty("totalVisits")
    val totalVisits: Int,
    @JsonProperty("vehicleCount")
    val vehicleCount: Int,
    @JsonProperty("totalRevenue")
    val totalRevenue: CustomerRevenueDto,
    @JsonProperty("createdAt")
    val createdAt: String,
    @JsonProperty("updatedAt")
    val updatedAt: String
) {
    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(client: Client, statistics: ClientStatistics?): CustomerDetailDto {
            return CustomerDetailDto(
                id = client.id.value.toString(),
                firstName = client.firstName,
                lastName = client.lastName,
                contact = CustomerContactDto(
                    email = client.email,
                    phone = client.phone
                ),
                homeAddress = client.address?.let {
                    HomeAddressDto.fromAddress(it)
                },
                company = client.company?.let { companyName ->
                    CompanyDetailsDto.fromClient(client, companyName)
                },
                notes = client.notes ?: "",
                lastVisitDate = statistics?.lastVisitDate?.format(DATE_TIME_FORMATTER),
                totalVisits = statistics?.visitCount?.toInt() ?: 0,
                vehicleCount = statistics?.vehicleCount?.toInt() ?: 0,
                totalRevenue = CustomerRevenueDto.from(statistics?.totalRevenue),
                createdAt = client.createdAt.format(DATE_TIME_FORMATTER),
                updatedAt = client.updatedAt.format(DATE_TIME_FORMATTER)
            )
        }
    }
}

/**
 * Customer contact information
 */
data class CustomerContactDto(
    val email: String,
    val phone: String
)

/**
 * Home address information
 */
data class HomeAddressDto(
    val street: String,
    val city: String,
    @JsonProperty("postalCode")
    val postalCode: String,
    val country: String
) {
    companion object {
        fun fromAddress(address: String): HomeAddressDto {
            // Parse address string - for now return a simple structure
            // In production, you would parse this properly or store structured data
            return HomeAddressDto(
                street = address,
                city = "",
                postalCode = "",
                country = "Polska"
            )
        }
    }
}

/**
 * Company details
 */
data class CompanyDetailsDto(
    val id: String,
    val name: String,
    val nip: String,
    val regon: String,
    val address: CompanyAddressDto
) {
    companion object {
        fun fromClient(client: Client, companyName: String): CompanyDetailsDto {
            return CompanyDetailsDto(
                id = client.id.value.toString(),
                name = companyName,
                nip = client.taxId ?: "",
                regon = "",
                address = CompanyAddressDto(
                    street = client.address ?: "",
                    city = "",
                    postalCode = "",
                    country = "Polska"
                )
            )
        }
    }
}

/**
 * Company address
 */
data class CompanyAddressDto(
    val street: String,
    val city: String,
    @JsonProperty("postalCode")
    val postalCode: String,
    val country: String
)

/**
 * Customer revenue information
 */
data class CustomerRevenueDto(
    @JsonProperty("netAmount")
    val netAmount: Double,
    @JsonProperty("grossAmount")
    val grossAmount: Double,
    val currency: String
) {
    companion object {
        fun from(priceValueObject: com.carslab.crm.production.shared.domain.value_objects.PriceValueObject?): CustomerRevenueDto {
            return CustomerRevenueDto(
                netAmount = priceValueObject?.priceNetto?.toDouble() ?: 0.0,
                grossAmount = priceValueObject?.priceBrutto?.toDouble() ?: 0.0,
                currency = "PLN"
            )
        }
    }
}

/**
 * Marketing consent information
 */
data class MarketingConsentDto(
    val id: String,
    val type: String, // 'email' | 'sms' | 'phone' | 'postal'
    val granted: Boolean,
    @JsonProperty("grantedAt")
    val grantedAt: String?,
    @JsonProperty("revokedAt")
    val revokedAt: String?,
    @JsonProperty("lastModifiedBy")
    val lastModifiedBy: String
)
