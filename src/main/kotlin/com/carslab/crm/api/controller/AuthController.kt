package com.carslab.crm.api.controller

import com.carslab.crm.api.model.request.LoginRequest
import com.carslab.crm.api.model.response.LoginResponse
import com.carslab.crm.domain.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/auth")
class AuthController(private val userService: UserService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        val response = userService.authenticate(loginRequest.username, loginRequest.password)
        return ResponseEntity.ok(response)
    }
}