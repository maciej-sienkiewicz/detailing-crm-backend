package com.carslab.crm.api.controller

import com.carslab.crm.domain.model.*
import com.carslab.crm.domain.permissions.RoleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/roles")
class RoleController(private val roleService: RoleService) {

    @PostMapping
    @PreAuthorize("hasPermission('ROLE', 'CREATE')")
    fun createRole(
        @Valid @RequestBody createRoleCommand: CreateRoleCommand
    ): ResponseEntity<RoleResponse> {
        val role = roleService.createRole(createRoleCommand)
        return ResponseEntity(role, HttpStatus.CREATED)
    }

    @GetMapping("/{id}")
    fun getRoleById(
        @PathVariable id: Long,
        @RequestParam companyId: Long
    ): ResponseEntity<RoleDetailResponse> {
        val role = roleService.getRoleById(id, companyId)
        return ResponseEntity.ok(role)
    }

    @PutMapping("/{id}")
    fun updateRole(
        @PathVariable id: Long,
        @Valid @RequestBody updateRoleCommand: UpdateRoleCommand
    ): ResponseEntity<RoleResponse> {
        // Ensure the ID in the path matches the ID in the command
        val commandWithId = updateRoleCommand.copy(id = id)
        val role = roleService.updateRole(commandWithId)
        return ResponseEntity.ok(role)
    }

    @DeleteMapping("/{id}")
    fun deleteRole(
        @PathVariable id: Long,
        @RequestParam companyId: Long
    ): ResponseEntity<Void> {
        roleService.deleteRole(id, companyId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/permissions")
    fun getRolePermissions(@PathVariable id: Long): ResponseEntity<List<Any>> {
        val permissions = roleService.getRolePermissions(id)
        return ResponseEntity.ok(permissions)
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    fun configureRolePermission(
        @PathVariable roleId: Long,
        @PathVariable permissionId: Long,
        @RequestParam companyId: Long,
        @RequestParam enabled: Boolean,
        @RequestParam(required = false) constraints: String?,
    ): ResponseEntity<Void> {
        val command = ConfigureRolePermissionCommand(
            roleId = roleId,
            permissionId = permissionId,
            companyId = companyId,
            enabled = enabled,
            constraints = constraints,
            userId = 0
        )

        roleService.configureRolePermission(command)
        return ResponseEntity.ok().build()
    }
}