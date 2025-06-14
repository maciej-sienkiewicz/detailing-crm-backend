package com.carslab.crm.modules.visits.api

import com.carslab.crm.domain.model.ProtocolComment
import com.carslab.crm.domain.model.ProtocolId
import com.carslab.crm.modules.visits.domain.ports.ProtocolCommentsRepository
import com.carslab.crm.infrastructure.persistence.entity.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/receptions/comments")
class CarReceptionCommentsController(
    private val commentsRepository: ProtocolCommentsRepository
) {

    @GetMapping("/{protocolId}")
    fun getComments(@PathVariable protocolId: String): ResponseEntity<List<CommentDto>> {
        val comments = commentsRepository.findById(ProtocolId(protocolId))
            .map { CommentDto(
                id = it.id.toString(),
                protocolId = it.protocolId.value,
                author = it.author,
                content = it.content,
                timestamp = it.timestamp,
                type = it.type
            ) }
        return ResponseEntity.ok(comments)
    }

    @PostMapping
    fun addComment(@RequestBody comment: CommentDto): ResponseEntity<CommentDto> {
        // W prawdziwej implementacji, tutaj dodawałbyś komentarz do bazy danych
        // Zwracam przesłany komentarz z dodanym ID i timestampem
        val user = (SecurityContextHolder.getContext().authentication.principal as UserEntity)
        val savedComment = comment.copy(
            id = UUID.randomUUID().toString(),
            timestamp = LocalDateTime.now().toString()).also {
            commentsRepository.save(ProtocolComment(
                protocolId = ProtocolId(comment.protocolId),
                author = "${user.firstName} ${user.lastName}",
                content = it.content,
                timestamp = it.timestamp ?: LocalDateTime.now().toString(),
                type = it.type
            ))
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment)
    }
}

data class CommentDto(
    val id: String? = null,
    val protocolId: String = "",
    val author: String = "",
    val content: String = "",
    val timestamp: String? = null,
    val type: String = "" // internal, customer, system
)