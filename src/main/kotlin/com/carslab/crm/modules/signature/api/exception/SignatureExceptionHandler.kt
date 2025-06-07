package com.carslab.crm.signature.api.exception

import com.carslab.crm.signature.exception.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class SignatureExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(InvalidPairingCodeException::class)
    fun handleInvalidPairingCode(e: InvalidPairingCodeException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid pairing code: ${e.message}")

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "Invalid pairing code",
                code = "INVALID_PAIRING_CODE",
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(TabletNotFoundException::class)
    fun handleTabletNotFound(e: TabletNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Tablet not found: ${e.message}")

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "Tablet not found",
                code = "TABLET_NOT_FOUND",
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(TabletNotAvailableException::class)
    fun handleTabletNotAvailable(e: TabletNotAvailableException): ResponseEntity<ErrorResponse> {
        logger.warn("Tablet not available: ${e.message}")

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "No tablet available",
                code = "TABLET_NOT_AVAILABLE",
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(SignatureSessionNotFoundException::class)
    fun handleSignatureSessionNotFound(e: SignatureSessionNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Signature session not found: ${e.message}")

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "Signature session not found",
                code = "SESSION_NOT_FOUND",
                timestamp = LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(UnauthorizedTabletException::class)
    fun handleUnauthorizedTablet(e: UnauthorizedTabletException): ResponseEntity<ErrorResponse> {
        logger.warn("Unauthorized tablet access: ${e.message}")

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                success = false,
                message = e.message ?: "Unauthorized access",
                code = "UNAUTHORIZED_TABLET",
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