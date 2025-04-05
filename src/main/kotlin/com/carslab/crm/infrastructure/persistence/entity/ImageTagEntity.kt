package com.carslab.crm.infrastructure.persistence.entity

import jakarta.persistence.*

@Entity
@Table(name = "image_tags")
@IdClass(ImageTagId::class)
class ImageTagEntity(
    @Id
    @Column(name = "image_id", nullable = false)
    var imageId: String? = null,

    @Id
    @Column(nullable = false)
    var tag: String
) {
    // Domyślny konstruktor wymagany przez JPA
    constructor() : this("", "")
}

// Klasa klucza złożonego
class ImageTagId(
    var imageId: String = "",
    var tag: String = ""
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as ImageTagId

        if (imageId != other.imageId) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageId.hashCode()
        result = 31 * result + tag.hashCode()
        return result
    }
}