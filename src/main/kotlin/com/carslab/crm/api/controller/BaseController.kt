package com.carslab.crm.api.controller.base

import com.carslab.crm.infrastructure.exception.ResourceNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

/**
 * Base controller to provide common functionality to all controllers.
 */
abstract class BaseController {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Creates a ResponseEntity with CREATED status and the given body.
     */
    protected fun <T> created(body: T): ResponseEntity<T> {
        return ResponseEntity.status(HttpStatus.CREATED).body(body)
    }

    /**
     * Creates a ResponseEntity with OK status and the given body.
     */
    protected fun <T> ok(body: T): ResponseEntity<T> {
        return ResponseEntity.ok(body)
    }

    /**
     * Creates a ResponseEntity with NO_CONTENT status.
     */
    protected fun noContent(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build()
    }

    /**
     * Safely finds a resource by ID, throwing a ResourceNotFoundException if not found.
     */
    protected fun <T> findResourceById(id: Any, resource: T?, resourceName: String): T {
        return resource ?: throw ResourceNotFoundException(resourceName, id)
    }

    /**
     * Creates a standard success response map with the given message and optional data.
     */
    protected fun createSuccessResponse(message: String, data: Any? = null): Map<String, Any> {
        val response = mutableMapOf<String, Any>(
            "success" to true,
            "message" to message,
            "timestamp" to LocalDateTime.now()
        )
        data?.let { response["data"] = it }
        return response
    }

    /**
     * Creates a ResponseEntity with BAD_REQUEST status and the given error message.
     */
    protected fun <T> badRequest(message: String): ResponseEntity<T> {
        val errorResponse = mapOf(
            "success" to false,
            "message" to message,
            "timestamp" to LocalDateTime.now()
        )

        @Suppress("UNCHECKED_CAST")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse as T)
    }

    /**
     * Logs an exception and re-throws it.
     */
    protected fun logAndRethrow(message: String, e: Exception): Nothing {
        logger.error("$message: ${e.message}", e)
        throw e
    }
}