package com.carslab.crm.modules.communication

import com.carslab.crm.modules.communication.domain.model.MailProviderType
import com.carslab.crm.modules.communication.domain.services.EmailCleaningService
import com.carslab.crm.modules.communication.domain.services.SubjectNormalizer
import com.carslab.crm.modules.communication.domain.services.ThreadingService
import com.carslab.crm.modules.communication.infrastructure.ai.HeuristicInquiryAnalyzer
import com.carslab.crm.modules.communication.infrastructure.mail.MailAutodiscoverService
import com.carslab.crm.modules.communication.domain.model.ThreadClassification
import com.carslab.crm.modules.communication.domain.ports.InquiryClassificationInput
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubjectNormalizerTest {

    @Test
    fun `strips polish and english reply prefixes, repeated and mixed`() {
        assertEquals("wycena bmw x5", SubjectNormalizer.normalize("Re: Odp: FW: Wycena BMW X5"))
        assertEquals("wycena bmw x5", SubjectNormalizer.normalize("ODP: wycena   BMW X5 "))
        assertEquals("wycena bmw x5", SubjectNormalizer.normalize("PD: Re[2]: Wycena BMW X5"))
    }

    @Test
    fun `returns null for blank subjects`() {
        assertNull(SubjectNormalizer.normalize(null))
        assertNull(SubjectNormalizer.normalize("  "))
        assertNull(SubjectNormalizer.normalize("Re:"))
    }
}

class MessageIdParsingTest {

    @Test
    fun `normalizes angle brackets and whitespace`() {
        assertEquals("abc@mail.com", ThreadingService.normalizeMessageId(" <abc@mail.com> "))
        assertEquals("abc@mail.com", ThreadingService.normalizeMessageId("abc@mail.com"))
        assertNull(ThreadingService.normalizeMessageId("<>"))
        assertNull(ThreadingService.normalizeMessageId(null))
    }

    @Test
    fun `parses references header into ordered ids`() {
        val refs = ThreadingService.parseReferences("<a@x.pl>\r\n <b@y.pl> <c@z.pl>")
        assertEquals(listOf("a@x.pl", "b@y.pl", "c@z.pl"), refs)
        assertTrue(ThreadingService.parseReferences(null).isEmpty())
    }

    @Test
    fun `synthetic id is deterministic`() {
        val at = LocalDateTime.of(2026, 8, 17, 10, 0)
        val a = ThreadingService.syntheticMessageId("jan@wp.pl", at, "Wycena")
        val b = ThreadingService.syntheticMessageId("jan@wp.pl", at, "Wycena")
        assertEquals(a, b)
    }
}

class EmailCleaningServiceTest {

    private val service = EmailCleaningService()

    @Test
    fun `strips gmail quote block and signature from html`() {
        val html = """
            <div dir="ltr">Dzień dobry, ile za ceramikę?<br></div>
            <div class="gmail_quote">W dniu pt., 15 sie 2026 Studio napisał(a):<blockquote>stara treść</blockquote></div>
        """.trimIndent()
        val cleaned = service.clean(html, null)
        assertTrue(cleaned.contains("ile za ceramikę"))
        assertFalse(cleaned.contains("stara treść"))
    }

    @Test
    fun `strips quoted lines and reply marker from plain text`() {
        val text = """
            Dzięki za wycenę, pasuje termin.

            W dniu 12.08.2026 Jan Kowalski napisał(a):
            > poprzednia wiadomość
            > jeszcze starsza linia
        """.trimIndent()
        val cleaned = service.clean(null, text)
        assertTrue(cleaned.contains("pasuje termin"))
        assertFalse(cleaned.contains("poprzednia wiadomość"))
        assertFalse(cleaned.contains("napisał"))
    }

    @Test
    fun `cuts signature after pozdrawiam marker`() {
        val cleaned = service.clean(null, "Proszę o wycenę.\n\nPozdrawiam\nJan Kowalski\ntel. 601 234 567")
        assertTrue(cleaned.contains("Proszę o wycenę"))
        assertFalse(cleaned.contains("601 234 567"))
    }

    @Test
    fun `caps output length`() {
        val cleaned = service.clean(null, "x".repeat(20_000))
        assertTrue(cleaned.length <= EmailCleaningService.MAX_CLEAN_LENGTH)
    }
}

class HeuristicInquiryAnalyzerTest {

    private val analyzer = HeuristicInquiryAnalyzer()

    @Test
    fun `detailing inquiry is classified as inquiry and extracted`() {
        val input = InquiryClassificationInput(
            fromEmail = "jan@wp.pl",
            subject = "Wycena",
            bodySnippet = "Dzień dobry, ile kosztuje powłoka ceramiczna na BMW X5 z 2021? Mój numer 601 234 567"
        )
        assertEquals(ThreadClassification.INQUIRY, analyzer.classify(input))
        val extracted = analyzer.extract(input)
        assertEquals("BMW", extracted.vehicleBrand)
        assertEquals("X5", extracted.vehicleModel)
        assertEquals(2021, extracted.vehicleYear)
        assertEquals("601234567", extracted.phone)
        assertTrue(extracted.services.contains("powłoka ceramiczna"))
    }

    @Test
    fun `invoice mail is not an inquiry`() {
        val input = InquiryClassificationInput(
            fromEmail = "faktury@operator.pl",
            subject = "Faktura za sierpień",
            bodySnippet = "W załączeniu przesyłamy fakturę VAT nr 8/2026."
        )
        assertEquals(ThreadClassification.OTHER, analyzer.classify(input))
    }
}

class AutoconfigParserTest {

    @Test
    fun `parses thunderbird autoconfig xml`() {
        val xml = """
            <clientConfig version="1.1"><emailProvider id="example.pl">
              <incomingServer type="imap"><hostname>imap.example.pl</hostname><port>993</port><socketType>SSL</socketType></incomingServer>
              <incomingServer type="pop3"><hostname>pop.example.pl</hostname><port>995</port></incomingServer>
              <outgoingServer type="smtp"><hostname>smtp.example.pl</hostname><port>465</port></outgoingServer>
            </emailProvider></clientConfig>
        """.trimIndent()
        val detection = MailAutodiscoverService().parseAutoconfig(xml)
        assertNotNull(detection)
        assertEquals(MailProviderType.IMAP_SMTP, detection.providerType)
        assertEquals("imap.example.pl", detection.imapHost)
        assertEquals(993, detection.imapPort)
        assertEquals("smtp.example.pl", detection.smtpHost)
        assertEquals(465, detection.smtpPort)
    }
}
