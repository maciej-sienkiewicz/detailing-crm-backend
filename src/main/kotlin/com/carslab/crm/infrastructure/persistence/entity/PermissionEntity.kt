package com.carslab.crm.infrastructure.persistence.entity

import jakarta.persistence.*

enum class DataField {
    // Dla klientów
    CLIENT_PERSONAL_DATA,  // dane osobowe klienta
    CLIENT_CONTACT_DATA,   // dane kontaktowe
    CLIENT_FINANCIAL_DATA, // dane finansowe

    // Dla protokołów
    PROTOCOL_OWNER_DATA,   // dane właściciela w protokole
    PROTOCOL_VEHICLE_DATA, // dane pojazdu
    PROTOCOL_SERVICE_DATA, // dane usług
    PROTOCOL_PRICE_DATA,   // dane cenowe

    // Inne kategorie danych
    // ...
}

@Entity
@Table(name = "permissions")
class PermissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false, name = "resource_type")
    @Enumerated(EnumType.STRING)
    var resourceType: ResourceType,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var action: PermissionAction,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "permission_data_fields",
        joinColumns = [JoinColumn(name = "permission_id")]
    )
    @Column(name = "data_field")
    @Enumerated(EnumType.STRING)
    var dataFields: MutableSet<DataField> = mutableSetOf()
) {
    fun toDomain(): Permission = Permission(
        id = this.id,
        name = this.name,
        description = this.description,
        resourceType = this.resourceType,
        action = this.action,
        dataFields = this.dataFields.toSet()
    )

    fun updateFromDomain(domain: Permission) {
        this.name = domain.name
        this.description = domain.description
        this.resourceType = domain.resourceType
        this.action = domain.action
        this.dataFields.clear()
        this.dataFields.addAll(domain.dataFields)
    }

    companion object {
        fun fromDomain(domain: Permission): PermissionEntity {
            val entity = PermissionEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                resourceType = domain.resourceType,
                action = domain.action
            )
            entity.dataFields.addAll(domain.dataFields)
            return entity
        }
    }
}

// Model domeny dla uprawnienia
data class Permission(
    val id: Long? = null,
    val name: String,
    val description: String,
    val resourceType: ResourceType,
    val action: PermissionAction,
    val dataFields: Set<DataField> = emptySet()
)

enum class ResourceType {
    CLIENT, VEHICLE, PROTOCOL, INVOICE, CALENDAR, REPORT, USER, ROLE, SETTING
}

enum class PermissionAction {
    VIEW, CREATE, EDIT, DELETE, APPROVE, EXPORT, ASSIGN, CONFIGURE
}