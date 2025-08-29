package com.carslab.crm.production.modules.templates.infrastructure.exception

import com.carslab.crm.production.shared.exception.BusinessException
import com.carslab.crm.production.shared.exception.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.carslab.crm.production.modules.templates"])
class TemplateExceptionHandler {

    private val logger = LoggerFactory.getLogger(TemplateExceptionHandler::class.java)

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Template not found: {}", ex.message)

        val errorResponse = ErrorResponse(
            error = "TEMPLATE_NOT_FOUND",
            message = ex.message ?: "Template not found",
            status = HttpStatus.NOT_FOUND.value()
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn("Template business rule violation: {}", ex.message)

        val errorResponse = ErrorResponse(
            error = "TEMPLATE_BUSINESS_ERROR",
            message = ex.message ?: "Business rule violation",
            status = HttpStatus.BAD_REQUEST.value()
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    data class ErrorResponse(
        val error: String,
        val message: String,
        val status: Int,
        val timestamp: Long = System.currentTimeMillis()
    )
}