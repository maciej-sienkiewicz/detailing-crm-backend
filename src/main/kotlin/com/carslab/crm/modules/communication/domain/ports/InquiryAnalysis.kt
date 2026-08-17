package com.carslab.crm.modules.communication.domain.ports

import com.carslab.crm.modules.communication.domain.model.ThreadClassification

data class InquiryClassificationInput(
    val fromEmail: String,
    val subject: String?,
    /** Cleaned body, already capped — never raw HTML. */
    val bodySnippet: String
)

data class ExtractedInquiry(
    val vehicleBrand: String? = null,
    val vehicleModel: String? = null,
    val vehicleYear: Int? = null,
    val services: List<String> = emptyList(),
    val phone: String? = null,
    val preferredDate: String? = null,
    val customerName: String? = null
)

/**
 * F2 of the classification funnel: cheap yes/no/unsure verdict on whether a
 * thread is a sales inquiry. Implementations may call an LLM; the pipeline
 * guarantees the input is denoised and capped.
 */
interface InquiryClassifier {
    fun classify(input: InquiryClassificationInput): ThreadClassification
}

/**
 * F3 of the funnel: structured extraction, invoked only for INQUIRY/UNSURE
 * threads. Low-confidence fields must be returned as null rather than guessed —
 * the UI renders no chip instead of a doubtful one.
 */
interface InquiryExtractor {
    fun extract(input: InquiryClassificationInput): ExtractedInquiry
}
