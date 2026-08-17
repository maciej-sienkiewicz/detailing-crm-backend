package com.carslab.crm.modules.communication.api

import com.carslab.crm.api.controller.base.BaseController
import com.carslab.crm.infrastructure.security.SecurityContext
import com.carslab.crm.modules.communication.api.dto.AddNoteRequest
import com.carslab.crm.modules.communication.api.dto.ConnectMailAccountRequest
import com.carslab.crm.modules.communication.api.dto.DetectProviderRequest
import com.carslab.crm.modules.communication.api.dto.DetectProviderResponse
import com.carslab.crm.modules.communication.api.dto.FactDto
import com.carslab.crm.modules.communication.api.dto.LeadDetailDto
import com.carslab.crm.modules.communication.api.dto.LeadListResponse
import com.carslab.crm.modules.communication.api.dto.MailAccountDto
import com.carslab.crm.modules.communication.api.dto.ReplyRequest
import com.carslab.crm.modules.communication.api.dto.ReplyResponse
import com.carslab.crm.modules.communication.api.dto.ReviewDecisionRequest
import com.carslab.crm.modules.communication.api.dto.ReviewListResponse
import com.carslab.crm.modules.communication.api.dto.StageChangeRequest
import com.carslab.crm.modules.communication.api.dto.TimelineItemDto
import com.carslab.crm.modules.communication.api.dto.UpdateFactRequest
import com.carslab.crm.modules.communication.application.LeadCommunicationService
import com.carslab.crm.modules.communication.application.LeadNotFoundException
import com.carslab.crm.modules.communication.application.MailAccountConnectionException
import com.carslab.crm.modules.communication.application.MailAccountService
import com.carslab.crm.modules.communication.domain.model.LeadStage
import com.carslab.crm.modules.communication.infrastructure.persistence.entity.MailAccountEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/communication")
@Tag(name = "Communication", description = "Unified inbox, threads and leads")
class CommunicationController(
    private val leadService: LeadCommunicationService,
    private val securityContext: SecurityContext
) : BaseController() {

    @GetMapping("/leads")
    @Operation(summary = "List lead cards, optionally filtered by stage")
    fun listLeads(@RequestParam(required = false) stage: String?): ResponseEntity<LeadListResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val stageEnum = stage?.let { runCatching { LeadStage.valueOf(it) }.getOrNull() }
        return ok(LeadListResponse(leadService.listLeads(companyId, stageEnum)))
    }

    @GetMapping("/leads/{id}")
    @Operation(summary = "Lead detail with facts and conversation timeline")
    fun getLead(@PathVariable id: Long): ResponseEntity<LeadDetailDto> {
        val companyId = securityContext.getCurrentCompanyId()
        val detail = leadService.getLeadDetail(companyId, id)
        leadService.markRead(companyId, id)
        return ok(detail)
    }

    @PostMapping("/leads/{id}/reply")
    @Operation(summary = "Send a reply from the owner's mailbox, with optional quote block")
    fun reply(@PathVariable id: Long, @Valid @RequestBody request: ReplyRequest): ResponseEntity<ReplyResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        val messageId = leadService.reply(companyId, id, request, securityContext.getCurrentUserName())
        return ok(ReplyResponse(messageId))
    }

    @PatchMapping("/leads/{id}/stage")
    @Operation(summary = "Change lead stage")
    fun changeStage(@PathVariable id: Long, @Valid @RequestBody request: StageChangeRequest): ResponseEntity<Void> {
        val companyId = securityContext.getCurrentCompanyId()
        val stage = runCatching { LeadStage.valueOf(request.stage) }.getOrElse {
            return ResponseEntity.badRequest().build()
        }
        leadService.changeStage(companyId, id, stage, request.reason)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/leads/{id}/notes")
    @Operation(summary = "Add an internal note to the lead timeline")
    fun addNote(@PathVariable id: Long, @Valid @RequestBody request: AddNoteRequest): ResponseEntity<TimelineItemDto> {
        val companyId = securityContext.getCurrentCompanyId()
        return created(leadService.addNote(companyId, id, request.text, securityContext.getCurrentUserName()))
    }

    @PutMapping("/leads/{id}/facts")
    @Operation(summary = "Edit and thereby verify a single extracted fact")
    fun updateFact(@PathVariable id: Long, @Valid @RequestBody request: UpdateFactRequest): ResponseEntity<FactDto> {
        val companyId = securityContext.getCurrentCompanyId()
        return ok(leadService.updateFact(companyId, id, request.factId, request.value))
    }

    @PostMapping("/leads/{id}/facts/verify")
    @Operation(summary = "Verify all extracted facts of a lead")
    fun verifyFacts(@PathVariable id: Long): ResponseEntity<Void> {
        val companyId = securityContext.getCurrentCompanyId()
        leadService.getLeadDetail(companyId, id) // ownership check
        leadService.verifyAllFacts(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/review")
    @Operation(summary = "Threads the classifier was unsure about")
    fun reviewQueue(): ResponseEntity<ReviewListResponse> {
        val companyId = securityContext.getCurrentCompanyId()
        return ok(ReviewListResponse(leadService.listReviewQueue(companyId)))
    }

    @PostMapping("/review/{threadId}/decision")
    @Operation(summary = "Human decision for an unsure thread; feeds sender rules")
    fun reviewDecision(
        @PathVariable threadId: Long,
        @Valid @RequestBody request: ReviewDecisionRequest
    ): ResponseEntity<Void> {
        val companyId = securityContext.getCurrentCompanyId()
        leadService.reviewDecision(companyId, threadId, request.decision)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(LeadNotFoundException::class)
    fun handleNotFound(e: LeadNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (e.message ?: "Not found")))
}

@RestController
@RequestMapping("/api/communication/mail-accounts")
@Tag(name = "Mail accounts", description = "Mailbox onboarding and sync")
class MailAccountController(
    private val mailAccountService: MailAccountService,
    private val securityContext: SecurityContext
) : BaseController() {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @PostMapping("/detect")
    @Operation(summary = "Detect provider and connection settings from an e-mail address")
    fun detect(@Valid @RequestBody request: DetectProviderRequest): ResponseEntity<DetectProviderResponse> {
        val d = mailAccountService.detect(request.email)
        return ok(
            DetectProviderResponse(
                providerType = d.providerType.name,
                authType = d.authType.name,
                imapHost = d.imapHost,
                imapPort = d.imapPort,
                smtpHost = d.smtpHost,
                smtpPort = d.smtpPort,
                requiresAppPassword = d.requiresAppPassword,
                guideUrl = d.guideUrl
            )
        )
    }

    @PostMapping
    @Operation(summary = "Connect a mailbox over IMAP/SMTP (tests the login first)")
    fun connect(@Valid @RequestBody request: ConnectMailAccountRequest): ResponseEntity<*> {
        val companyId = securityContext.getCurrentCompanyId()
        return try {
            val account = mailAccountService.connect(
                companyId, request.email, request.password,
                request.imapHost, request.imapPort, request.smtpHost, request.smtpPort
            )
            created(toDto(account))
        } catch (e: MailAccountConnectionException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping
    @Operation(summary = "List connected mailboxes")
    fun list(): ResponseEntity<List<MailAccountDto>> {
        val companyId = securityContext.getCurrentCompanyId()
        return ok(mailAccountService.list(companyId).map { toDto(it) })
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disconnect a mailbox")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        val companyId = securityContext.getCurrentCompanyId()
        mailAccountService.delete(companyId, id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "Trigger a manual sync")
    fun sync(@PathVariable id: Long): ResponseEntity<Void> {
        val companyId = securityContext.getCurrentCompanyId()
        mailAccountService.requestSync(companyId, id)
        return ResponseEntity.accepted().build()
    }

    @ExceptionHandler(MailAccountConnectionException::class)
    fun handleConnectionError(e: MailAccountConnectionException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.badRequest().body(mapOf("error" to e.message))

    private fun toDto(account: MailAccountEntity): MailAccountDto = MailAccountDto(
        id = requireNotNull(account.id),
        emailAddress = account.emailAddress,
        providerType = account.providerType.name,
        status = account.status.name,
        lastSyncAt = account.lastSyncAt?.format(isoFormatter)
    )
}
