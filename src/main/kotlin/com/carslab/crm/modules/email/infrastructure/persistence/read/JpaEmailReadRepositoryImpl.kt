package com.carslab.crm.modules.email.infrastructure.persistence.read

import com.carslab.crm.modules.email.application.queries.models.EmailHistoryResponse
import com.carslab.crm.modules.email.infrastructure.persistence.repository.EmailHistoryJpaRepository
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.api.model.response.PaginatedResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.format.DateTimeFormatter

@Repository
class JpaEmailReadRepositoryImpl(
    private val emailHistoryJpaRepository: EmailHistoryJpaRepository,
    private val securityContext: SecurityContext
) : EmailReadRepository {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    override fun getEmailHistory(
        protocolId: String?,
        page: Int,
        size: Int
    ): PaginatedResponse<EmailHistoryResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"))

        val emailPage = emailHistoryJpaRepository.findByCompanyIdAndOptionalProtocolId(
            companyId = companyId,
            protocolId = protocolId,
            pageable = pageable
        )

        val emailHistory = emailPage.content.map { entity ->
            EmailHistoryResponse(
                id = entity.id,
                protocolId = entity.protocolId,
                recipientEmail = entity.recipientEmail,
                subject = entity.subject,
                status = entity.status.name,
                sentAt = entity.sentAt.format(dateTimeFormatter),
                errorMessage = entity.errorMessage
            )
        }

        return PaginatedResponse(
            data = emailHistory,
            page = page,
            size = size,
            totalItems = emailPage.totalElements,
            totalPages = emailPage.totalPages.toLong()
        )
    }
}