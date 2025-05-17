package com.carslab.crm.api.controller

import com.carslab.crm.api.model.*
import com.carslab.crm.domain.mail.MailService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/mail")
class MailController(private val mailService: MailService) {

    @PostMapping("/auth/login")
    fun login(@RequestBody request: MailLoginRequest): MailLoginResponse {
        return mailService.loginToMailServer(request)
    }

    @PostMapping("/auth/logout")
    fun logout(@RequestHeader("Authorization") authHeader: String): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.logout(token)
    }

    @GetMapping("/folders")
    fun getFolders(@RequestHeader("Authorization") authHeader: String): MailFoldersResponse {
        val token = extractToken(authHeader)
        return mailService.getFolders(token)
    }

    @GetMapping("/folders/summary")
    fun getFoldersSummary(@RequestHeader("Authorization") authHeader: String): MailFoldersSummaryResponse {
        val token = extractToken(authHeader)
        return mailService.getFoldersSummary(token)
    }

    @GetMapping("/emails")
    fun getEmails(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam(required = false, defaultValue = "INBOX") folder: String, // Zmiana z folders (List<String>) na folder (String)
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "20") maxResults: Int,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) pageToken: String?
    ): MailsResponse {
        val token = extractToken(authHeader)
        return if (query != null) {
            mailService.searchEmails(token, folder, query, page, maxResults)
        } else {
            mailService.getEmails(token, folder, page, maxResults, pageToken)
        }
    }

    @GetMapping("/emails/{id}")
    fun getEmail(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String
    ): EmailResponse {
        val token = extractToken(authHeader)
        return mailService.getEmail(token, id)
    }

    @GetMapping("/emails/{messageId}/attachments/{attachmentId}")
    fun getAttachment(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable messageId: String,
        @PathVariable attachmentId: String
    ): AttachmentResponse {
        val token = extractToken(authHeader)
        return mailService.getAttachment(token, messageId, attachmentId)
    }

    @PostMapping("/emails/send")
    fun sendEmail(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: SendEmailRequest
    ): SendEmailResponse {
        val token = extractToken(authHeader)
        return mailService.sendEmail(token, request)
    }

    @PostMapping("/emails/draft")
    fun saveDraft(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: SendEmailRequest
    ): SendEmailResponse {
        val token = extractToken(authHeader)
        return mailService.saveDraft(token, request)
    }

    @PatchMapping("/emails/{id}/read")
    fun markAsRead(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String,
        @RequestBody request: MarkAsReadRequest
    ): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.markAsRead(token, id, request.isRead)
    }

    @PatchMapping("/emails/{id}/flag")
    fun toggleFlag(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String,
        @RequestBody request: ToggleFlagRequest
    ): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.toggleFlag(token, id, request.flagName, request.value)
    }

    @PatchMapping("/emails/{id}/move")
    fun moveEmail(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String,
        @RequestBody request: MoveEmailRequest
    ): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.moveEmail(token, id, request.destinationFolder)
    }

    @PatchMapping("/emails/{id}/folders")
    fun updateFolders(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String,
        @RequestBody request: UpdateFoldersRequest
    ): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.updateFolders(token, id, request.addFolders, request.removeFolders)
    }

    @DeleteMapping("/emails/{id}")
    fun deleteEmail(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String
    ): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.permanentlyDelete(token, id)
    }

    @PostMapping("/folders")
    fun createFolder(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: CreateFolderRequest
    ): CreateFolderResponse {
        val token = extractToken(authHeader)
        return mailService.createFolder(token, request.name, request.color)
    }

    @DeleteMapping("/folders/{path}")
    fun deleteFolder(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable path: String
    ): MailActionResponse {
        val token = extractToken(authHeader)
        return mailService.deleteFolder(token, path)
    }

    private fun extractToken(authHeader: String): String {
        return authHeader.replace("Bearer ", "")
    }
}