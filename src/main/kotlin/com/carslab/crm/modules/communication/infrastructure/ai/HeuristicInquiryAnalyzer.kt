package com.carslab.crm.modules.communication.infrastructure.ai

import com.carslab.crm.modules.communication.domain.model.ThreadClassification
import com.carslab.crm.modules.communication.domain.ports.ExtractedInquiry
import com.carslab.crm.modules.communication.domain.ports.InquiryClassificationInput
import com.carslab.crm.modules.communication.domain.ports.InquiryClassifier
import com.carslab.crm.modules.communication.domain.ports.InquiryExtractor
import org.springframework.stereotype.Service

/**
 * Rule-based fallback implementation of the F2/F3 analysis ports. An
 * LLM-backed implementation should be registered as @Primary to take over;
 * this bean keeps the pipeline fully functional (and testable) without any
 * external model dependency.
 */
@Service
class HeuristicInquiryAnalyzer : InquiryClassifier, InquiryExtractor {

    private val inquiryKeywords = listOf(
        "wycen", "cena", "cenę", "koszt", "ile by", "ile kosztuje", "oferta", "termin",
        "powłok", "ceramicz", "ceramik", "folia", "folię", "ppf", "detailing",
        "polerow", "korekta", "korekt", "czyszczenie", "pranie", "wnętrza", "wnętrze",
        "auto detail", "przyciemni", "dechrom", "oklejenie", "okleić", "zabezpiecz",
        "renowacj", "felg", "tapicerk", "lakier", "quote", "price"
    )

    private val brandModels = mapOf(
        "audi" to "Audi", "bmw" to "BMW", "mercedes" to "Mercedes", "vw" to "Volkswagen",
        "volkswagen" to "Volkswagen", "toyota" to "Toyota", "skoda" to "Skoda",
        "škoda" to "Skoda", "ford" to "Ford", "opel" to "Opel", "kia" to "Kia",
        "hyundai" to "Hyundai", "mazda" to "Mazda", "nissan" to "Nissan",
        "porsche" to "Porsche", "tesla" to "Tesla", "volvo" to "Volvo",
        "lexus" to "Lexus", "honda" to "Honda", "peugeot" to "Peugeot",
        "renault" to "Renault", "seat" to "Seat", "cupra" to "Cupra",
        "dacia" to "Dacia", "fiat" to "Fiat", "jeep" to "Jeep", "mini" to "Mini",
        "land rover" to "Land Rover", "jaguar" to "Jaguar", "subaru" to "Subaru",
        "suzuki" to "Suzuki", "citroen" to "Citroen", "citroën" to "Citroen"
    )

    private val serviceKeywords = mapOf(
        "powłoka ceramiczna" to listOf("powłok", "ceramicz", "ceramik"),
        "folia PPF" to listOf("ppf", "folia ochronn", "folią ochronn", "oklejenie", "okleić", "folia", "folię"),
        "korekta lakieru" to listOf("korekta", "korekt", "polerow"),
        "czyszczenie wnętrza" to listOf("wnętrz", "tapicerk", "pranie", "odświeżeni"),
        "przyciemnianie szyb" to listOf("przyciemni", "szyb"),
        "renowacja felg" to listOf("felg"),
        "detailing kompletny" to listOf("full body", "kompleksow", "detailing")
    )

    private val phoneRegex = Regex("(?<!\\d)(\\+?48[\\s-]?)?(\\d{3}[\\s-]?\\d{3}[\\s-]?\\d{3})(?!\\d)")
    private val yearRegex = Regex("\\b(19[89]\\d|20[0-4]\\d)\\b")
    private val modelAfterBrandRegex = "\\s+([a-z0-9][a-z0-9-]{0,14})"

    override fun classify(input: InquiryClassificationInput): ThreadClassification {
        val haystack = (input.subject.orEmpty() + "\n" + input.bodySnippet).lowercase()
        val hits = inquiryKeywords.count { haystack.contains(it) }
        return when {
            hits >= 2 -> ThreadClassification.INQUIRY
            hits == 1 || haystack.contains("?") -> ThreadClassification.UNSURE
            else -> ThreadClassification.OTHER
        }
    }

    override fun extract(input: InquiryClassificationInput): ExtractedInquiry {
        val text = input.subject.orEmpty() + "\n" + input.bodySnippet
        val lower = text.lowercase()

        var brand: String? = null
        var model: String? = null
        for ((needle, canonical) in brandModels) {
            val idx = lower.indexOf(needle)
            if (idx >= 0) {
                brand = canonical
                Regex(Regex.escape(needle) + modelAfterBrandRegex)
                    .find(lower)
                    ?.groupValues?.get(1)
                    ?.takeIf { it !in setOf("i", "w", "z", "na", "do", "oraz", "czy") }
                    ?.let { model = it.uppercase() }
                break
            }
        }

        val services = serviceKeywords
            .filter { (_, needles) -> needles.any { lower.contains(it) } }
            .keys
            .toList()

        return ExtractedInquiry(
            vehicleBrand = brand,
            vehicleModel = model,
            vehicleYear = yearRegex.find(text)?.value?.toIntOrNull(),
            services = services,
            phone = phoneRegex.find(text)?.value?.replace(Regex("[\\s-]"), "")
        )
    }
}
