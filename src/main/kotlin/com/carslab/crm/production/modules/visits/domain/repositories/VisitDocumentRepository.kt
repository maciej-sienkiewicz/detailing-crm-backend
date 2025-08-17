package com.carslab.crm.production.modules.visits.domain.repositories

import com.carslab.crm.production.modules.visits.domain.models.entities.VisitDocument
import com.carslab.crm.production.modules.visits.domain.models.value_objects.VisitId

interface VisitDocumentRepository {
    fun save(document: VisitDocument): VisitDocument
    fun findByVisitId(visitId: VisitId): List<VisitDocument>
    fun findById(documentId: String): VisitDocument?
    fun existsVisitByIdAndCompanyId(visitId: VisitId, companyId: Long): Boolean
    fun deleteById(documentId: String): Boolean
    fun getFileData(documentId: String): ByteArray?
}