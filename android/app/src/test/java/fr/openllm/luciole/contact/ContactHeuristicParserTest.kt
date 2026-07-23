package fr.openllm.luciole.contact

import fr.openllm.luciole.ocr.OcrPostProcessor
import fr.openllm.luciole.ocr.OcrResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContactHeuristicParserTest {

    @Test fun parseCarteDimoBruitee() {
        val text = """
            dimo
            SOFTWARE &—
            Jean Pau! GENOUX
            Directeur Général/CEO
            jpgenoux@dimosoftware.com
            Mobile : +33 (0)6 74 64 05 44 / Tél. : +33 (0)4 72 86 O1 92
            Siège Social
            561, alée des Noisetiers - 69 760 Limonest - France
            Lyon - Paris - Nantes - Biarritz - Madrid — Toronto
            www.dimosoftware.fr
        """.trimIndent()
        val ocr = OcrPostProcessor.process(text)
        val card = ContactHeuristicParser.parse(ocr)

        assertEquals("Jean Paul Genoux", card.fullName)
        assertEquals("Jean Paul", card.firstName)
        assertEquals("Genoux", card.lastName)
        assertTrue(card.jobTitle!!.contains("Directeur", ignoreCase = true))
        assertTrue(card.company!!.contains("Software", ignoreCase = true) || card.company!!.contains("dimo", ignoreCase = true))
        assertTrue(card.emails.contains("jpgenoux@dimosoftware.com"))
        assertTrue(card.phones.any { it.endsWith("674640544") })
        assertTrue(card.phones.any { it.contains("472860192") })
        assertTrue(card.website!!.contains("dimosoftware.fr"))
        assertNotNull(card.address)
        assertTrue(card.address!!.contains("Noisetiers") || card.address!!.contains("Limonest"))
    }

    @Test fun splitNomAvecNomFamilleMajuscules() {
        val (first, last, full) = ContactHeuristicParser.splitPersonName("Marie CURIE")
        assertEquals("Marie", first)
        assertEquals("Curie", last)
        assertEquals("Marie Curie", full)
    }

    @Test fun societeDepuisEmail() {
        assertEquals("Dimo Software", ContactHeuristicParser.companyFromEmailDomain("jp@dimosoftware.com"))
    }

    @Test fun mergeSansLlmRemplitQuandMemeLesChamps() {
        val ocr = OcrPostProcessor.process(
            """
            Acme SAS
            Alice Martin
            CEO
            alice@acme.fr
            01 23 45 67 89
            10 rue de Paris 75001 Paris
            """.trimIndent()
        )
        val merged = ContactDraftMerge.merge(null, ocr)
        assertEquals("Alice Martin", merged.fullName)
        assertTrue(merged.emails.contains("alice@acme.fr"))
        assertTrue(merged.jobTitle!!.contains("CEO", ignoreCase = true))
        assertNotNull(merged.company)
    }
}
