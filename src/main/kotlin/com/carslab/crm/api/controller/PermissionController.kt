package com.carslab.crm.api.controller

import com.carslab.crm.infrastructure.persistence.entity.DataField
import com.carslab.crm.infrastructure.persistence.entity.Permission
import com.carslab.crm.infrastructure.persistence.entity.PermissionAction
import com.carslab.crm.infrastructure.persistence.entity.PermissionEntity
import com.carslab.crm.infrastructure.persistence.entity.ResourceType
import com.carslab.crm.infrastructure.persistence.repository.PermissionRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/permissions")
class PermissionController(private val permissionRepository: PermissionRepository) {

    @PostMapping
    fun createPermission(@Valid @RequestBody permissionDto: PermissionDto): ResponseEntity<PermissionEntity> {
        val permissionEntity = PermissionEntity(
            name = permissionDto.name,
            description = permissionDto.description,
            resourceType = permissionDto.resourceType,
            action = permissionDto.action
        )

        // Add data fields if provided
        if (permissionDto.dataFields.isNotEmpty()) {
            permissionEntity.dataFields.addAll(permissionDto.dataFields)
        }

        val savedPermission = permissionRepository.save(permissionEntity)
        return ResponseEntity(savedPermission, HttpStatus.CREATED)
    }

    @GetMapping
    fun getAllPermissions(): ResponseEntity<List<PermissionEntity>> {
        val permissions = permissionRepository.findAllOrdered()
        return ResponseEntity.ok(permissions)
    }

    @GetMapping("/{id}")
    fun getPermissionById(@PathVariable id: Long): ResponseEntity<PermissionEntity> {
        val permission = permissionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Permission not found with id: $id") }
        return ResponseEntity.ok(permission)
    }

    @PutMapping("/{id}")
    fun updatePermission(
        @PathVariable id: Long,
        @Valid @RequestBody permissionDto: PermissionDto
    ): ResponseEntity<PermissionEntity> {
        val existingPermission = permissionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Permission not found with id: $id") }

        existingPermission.name = permissionDto.name
        existingPermission.description = permissionDto.description
        existingPermission.resourceType = permissionDto.resourceType
        existingPermission.action = permissionDto.action

        // Update data fields
        existingPermission.dataFields.clear()
        if (permissionDto.dataFields.isNotEmpty()) {
            existingPermission.dataFields.addAll(permissionDto.dataFields)
        }

        val updatedPermission = permissionRepository.save(existingPermission)
        return ResponseEntity.ok(updatedPermission)
    }

    @DeleteMapping("/{id}")
    fun deletePermission(@PathVariable id: Long): ResponseEntity<Void> {
        if (!permissionRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }

        permissionRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}

data class PermissionDto(
    val name: String = "",
    val description: String = "",
    val resourceType: ResourceType = ResourceType.CLIENT,
    val action: PermissionAction = PermissionAction.VIEW,
    val dataFields: Set<DataField> = emptySet()
)