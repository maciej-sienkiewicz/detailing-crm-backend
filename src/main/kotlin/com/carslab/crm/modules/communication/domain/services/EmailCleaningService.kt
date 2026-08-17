package com.carslab.crm.modules.communication.domain.services

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Service

/**
 * Deterministic e-mail body denoising, run once at ingest and persisted as
 * body_text_clean. The UI renders this text as chat bubbles and the LLM
 * pipeline reads it, so every quoted history block and signature removed here
 * is paid for exactly once (CPU) instead of on every prompt (tokens).
 */
@Service
class EmailCleaningService {

    private val quoteMarkers = listOf(
        Regex("^W dniu .{0,120}napisa[łl](a)?\\s*(:|\\(a\\):)", setOf(RegexOption.IGNORE_CASE)),
        Regex("^Dnia .{0,120}(napisa[łl](a)?|pisze)\\s*:", setOf(RegexOption.IGNORE_CASE)),
        Regex("^On .{0,120}wrote:", setOf(RegexOption.IGNORE_CASE)),
        Regex("^-{2,}\\s*(Original Message|Wiadomość oryginalna|Forwarded message|Wiadomość przekazana)", setOf(RegexOption.IGNORE_CASE)),
        Regex("^(pt\\.|sob\\.|niedz\\.|pon\\.|wt\\.|śr\\.|czw\\.),? .{0,120}napisa[łl](a)?\\s*(:|\\(a\\):)", setOf(RegexOption.IGNORE_CASE))
    )

    private val signatureMarkers = listOf(
        Regex("^--\\s*$"),
        Regex("^(Pozdrawiam|Z poważaniem|Z wyrazami szacunku|Best regards|Kind regards|Serdecznie pozdrawiam)[,.!]?\\s*$", setOf(RegexOption.IGNORE_CASE)),
        Regex("^(Wysłane z|Sent from) ", setOf(RegexOption.IGNORE_CASE))
    )

    fun clean(html: String?, plainText: String?): String {
        val text = when {
            !html.isNullOrBlank() -> htmlToText(html)
            !plainText.isNullOrBlank() -> plainText
            else -> return ""
        }
        return stripQuotesAndSignature(text).take(MAX_CLEAN_LENGTH).trim()
    }

    private fun htmlToText(html: String): String {
        val doc: Document = Jsoup.parse(html)
        doc.select("style, script, head, img[width=1], img[height=1]").remove()
        // Quoted-history containers of the major clients.
        doc.select("div.gmail_quote, blockquote, div#divRplyFwdMsg, div.moz-cite-prefix, div.yahoo_quoted").remove()
        doc.select("[style~=(?i)display:\\s*none]").remove()
        doc.select("br").before("\\n")
        doc.select("p, div, li, tr").before("\\n")
        val cleaned = Jsoup.clean(doc.html().replace("\\n", "\n"), "", Safelist.none(),
            Document.OutputSettings().prettyPrint(false))
        return org.jsoup.parser.Parser.unescapeEntities(cleaned, false)
    }

    private fun stripQuotesAndSignature(text: String): String {
        val lines = text.replace("\r\n", "\n").split('\n')
        val kept = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith(">")) continue
            if (quoteMarkers.any { it.containsMatchIn(trimmed) }) break
            if (signatureMarkers.any { it.matches(trimmed) }) break
            kept.add(line.trimEnd())
        }
        return kept.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
    }

    companion object {
        const val MAX_CLEAN_LENGTH = 4000
    }
}
