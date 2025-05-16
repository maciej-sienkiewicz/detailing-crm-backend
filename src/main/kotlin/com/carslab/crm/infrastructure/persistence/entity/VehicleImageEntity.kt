package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.view.protocol.MediaTypeView
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vehicle_images")
class VehicleImageEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @Column(nullable = false)
    var companyId: Long,

    // Zmiana z obiektu na ID
    @Column(name = "protocol_id", nullable = false)
    var protocolId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(name = "content_type", nullable = false)
    var contentType: String,

    @Column(nullable = false)
    var size: Long,

    @Column(nullable = true)
    var description: String? = null,

    @Column(nullable = true)
    var location: String? = null,

    @Column(name = "storage_path", nullable = false)
    var storagePath: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    // Zmiana mapowania - zamiast używać JPA relation użyjemy query
    @Transient
    var tagsList: List<String> = emptyList()
) {
    fun toDomain(): MediaTypeView {
        return MediaTypeView(
            id = id,
            name = name,
            size = size,
            tags = tagsList
        )
    }

    // Metoda pomocnicza do ustawiania tagów
    fun setTags(tags: List<String>) {
        this.tagsList = tags
    }
}