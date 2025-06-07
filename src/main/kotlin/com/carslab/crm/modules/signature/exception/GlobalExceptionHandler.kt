package com.carslab.crm.signature.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn("Business exception: ${e.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "Business logic error",
                code = e.errorCode,
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(e: SecurityException): ResponseEntity<ErrorResponse> {
        logger.warn("Security exception: ${e.message}")

        val status = when (e.errorCode) {
            "UNAUTHORIZED_TABLET", "UNAUTHORIZED_DEVICE" -> HttpStatus.UNAUTHORIZED
            else -> HttpStatus.FORBIDDEN
        }

        return ResponseEntity.status(status).body(
            ErrorResponse(
                success = false,
                message = "Access denied",
                code = e.errorCode,
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception: ${e.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "Validation error",
                code = e.errorCode,
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors.joinToString(", ") { error: FieldError ->
            "${error.field}: ${error.defaultMessage}"
        }

        logger.warn("Validation errors: $errors")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                success = false,
                message = "Validation failed: $errors",
                code = "VALIDATION_FAILED",
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: ${e.message}")

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                success = false,
                message = "Access denied",
                code = "ACCESS_DENIED",
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", e)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                success = false,
                message = "Internal server error",
                code = "INTERNAL_ERROR",
                timestamp = LocalDateTime.now()
            )
        )
    }
}

data class ErrorResponse(
    val success: Boolean,
    val message: String,
    val code: String,
    val timestamp: LocalDateTime
)