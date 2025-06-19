package com.carslab.crm.modules.visits.infrastructure.processor

import com.carslab.crm.modules.visits.infrastructure.processor.exceptions.UnsupportedRequestFormatException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartHttpServletRequest

@Component
class RequestProcessorFactory(
    private val processors: List<MultipartRequestProcessor>
) {
    fun getProcessor(request: MultipartHttpServletRequest): MultipartRequestProcessor {
        return processors.firstOrNull { it.canProcess(request) }
            ?: throw UnsupportedRequestFormatException("No processor found for request format")
    }
}