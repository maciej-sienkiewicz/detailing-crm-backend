package com.carslab.crm.production.modules.visits.infrastructure.request

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartHttpServletRequest

@Component
class RequestFormatDetector {

    fun detectFormat(request: MultipartHttpServletRequest): RequestFormat {
        return when {
            hasImageArrayFormat(request) -> RequestFormat.IMAGE_ARRAY
            hasStandardFormat(request) -> RequestFormat.STANDARD
            else -> RequestFormat.UNKNOWN
        }
    }

    private fun hasImageArrayFormat(request: MultipartHttpServletRequest): Boolean {
        return request.fileMap.keys.any { it.matches(Regex("images\\[\\d+\\]")) } &&
                request.getParameter("image") != null
    }

    private fun hasStandardFormat(request: MultipartHttpServletRequest): Boolean {
        return request.getFile("file") != null &&
                request.getParameter("mediaDetails") != null
    }
}

enum class RequestFormat {
    IMAGE_ARRAY,
    STANDARD,
    UNKNOWN
}