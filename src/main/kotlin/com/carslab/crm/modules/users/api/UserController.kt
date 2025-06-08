package com.carslab.crm.modules.users.api

import com.carslab.crm.domain.model.CreateUserCommand
import com.carslab.crm.domain.model.UpdateProfileCommand
import com.carslab.crm.domain.model.UserResponse
import com.carslab.crm.infrastructure.auth.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping
    fun createUser(@Valid @RequestBody createUserCommand: CreateUserCommand): ResponseEntity<UserResponse> {
        val user = userService.createUser(createUserCommand)
        return ResponseEntity(user, HttpStatus.CREATED)
    }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }

    @GetMapping
    fun getAllUsersByCompany(@RequestParam companyId: Long): ResponseEntity<List<UserResponse>> {
        val users = userService.getUsersByCompany(companyId)
        return ResponseEntity.ok(users)
    }

    @PutMapping("/{id}")
    fun updateUserProfile(
        @PathVariable id: Long,
        @Valid @RequestBody updateProfileCommand: UpdateProfileCommand
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUserProfile(id, updateProfileCommand)
        return ResponseEntity.ok(updatedUser)
    }

    @PostMapping("/{userId}/roles/{roleId}")
    fun assignRoleToUser(
        @PathVariable userId: Long,
        @PathVariable roleId: Long,
        @RequestParam companyId: Long
    ): ResponseEntity<Void> {
        userService.assignRole(userId, roleId, companyId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    fun removeRoleFromUser(
        @PathVariable userId: Long,
        @PathVariable roleId: Long,
        @RequestParam companyId: Long
    ): ResponseEntity<Void> {
        userService.removeRole(userId, roleId, companyId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{userId}/roles")
    fun getUserRoles(
        @PathVariable userId: Long,
        @RequestParam companyId: Long
    ): ResponseEntity<List<Any>> {
        val roles = userService.getUserRoles(userId, companyId)
        return ResponseEntity.ok(roles)
    }
}