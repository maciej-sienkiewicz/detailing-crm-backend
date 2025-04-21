package com.carslab.crm.infrastructure.persistence.repository

import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {

    fun findByUsername(username: String): Optional<UserEntity>

    fun findByEmail(email: String): Optional<UserEntity>

    @Query("SELECT u FROM UserEntity u WHERE u.companyId = :companyId")
    fun findByCompanyId(@Param("companyId") companyId: Long): List<UserEntity>

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u " +
            "WHERE u.username = :username")
    fun existsByUsername(@Param("username") username: String): Boolean

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u " +
            "WHERE u.email = :email")
    fun existsByEmail(@Param("email") email: String): Boolean

    @Query("SELECT u FROM UserEntity u " +
            "JOIN u.roles r " +
            "WHERE r.id = :roleId")
    fun findByRoleId(@Param("roleId") roleId: Long): List<UserEntity>
}