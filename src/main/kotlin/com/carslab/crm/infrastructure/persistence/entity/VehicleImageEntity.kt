package com.carslab.crm.infrastructure.persistence.entity

import com.carslab.crm.domain.model.MediaType
import com.carslab.crm.domain.model.view.protocol.MediaTypeView
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vehicle_images")
class VehicleImageEntity(
    @Id
    @Column(nullable = false)
    val id: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    var protocol: ProtocolEntity,

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

    @OneToMany(mappedBy = "image", cascade = [CascadeType.ALL], orphanRemoval = true)
    var tags: MutableSet<ImageTagEntity> = mutableSetOf()
) {
    fun toDomain(): MediaTypeView {
        return MediaTypeView(
            id = id,
            name = name,
            size = size,
            tags = tags.map { it.tag }.toList()
        )
    }
}

@Entity
@Table(name = "image_tags")
class ImageTagEntity(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    var image: VehicleImageEntity,

    @Id
    @Column(nullable = false)
    var tag: String
)