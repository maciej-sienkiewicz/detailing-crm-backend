package com.carslab.crm.production.shared.presentation

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

abstract class BaseController {
    protected val logger = LoggerFactory.getLogger(this::class.java)

    protected fun <T> created(body: T): ResponseEntity<T> {
        return ResponseEntity.status(HttpStatus.CREATED).body(body)
    }

    protected fun <T> ok(body: T): ResponseEntity<T> {
        return ResponseEntity.ok(body)
    }
}