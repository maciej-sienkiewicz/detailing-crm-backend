package com.carslab.crm.infrastructure.security

import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import com.carslab.crm.modules.visits.infrastructure.events.CurrentUser
import com.carslab.crm.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityContext {

    fun getCurrentCompanyId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is UserEntity -> principal.companyId
            else -> throw IllegalStateException("No authenticated user found")
        }
    }

    fun getCurrentUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is UserPrincipal -> principal.id.toString()
            is UserEntity -> principal.id.toString()
            else -> null
        }
    }

    fun getCurrentUserName(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is UserEntity -> principal.username
            else -> null
        }
    }


    fun getCurrentUser(): CurrentUser {
        val authentication = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is UserPrincipal -> CurrentUser(
                id = principal.id.toString(),
                name = principal.username,
                companyId = principal.companyId
            )
            is UserEntity -> CurrentUser(
                id = principal.id.toString(),
                name = principal.username,
                companyId = principal.companyId
            )
            else -> throw IllegalStateException("No authenticated user found")
        }
    }
}