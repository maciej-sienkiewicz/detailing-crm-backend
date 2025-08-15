package com.carslab.crm.production.modules.activities.infrastructure.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "activity_entity_relations",
    indexes = [
        Index(name = "idx_activity_relations_activity", columnList = "activity_id"),
        Index(name = "idx_activity_relations_entity", columnList = "entity_type,entity_id")
    ]
)
class ActivityEntityRelationEntity(
    @Id
    @Column(nullable = false, length = 36)
    val id: String,

    @Column(name = "activity_id", nullable = false, length = 36)
    val activityId: String,

    @Column(name = "entity_id", nullable = false)
    val entityId: String,

    @Column(name = "entity_type", nullable = false)
    val entityType: String,

    @Column(name = "entity_name", nullable = false)
    val entityName: String
)