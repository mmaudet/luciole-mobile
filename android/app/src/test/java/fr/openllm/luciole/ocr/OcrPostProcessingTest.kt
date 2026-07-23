package fr.openllm.luciole.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrPostProcessingTest {
    @Test fun extraitEmailEtTelephone() {
        val text = """
            Jean Dupont
            Directeur
            06 12 34 56 78
            jean.dupont@acme.fr
            https://acme.fr
        """.trimIndent()
        val r = OcrPostProcessor.process(text)
        assertTrue(r.emails.contains("jean.dupont@acme.fr"))
        assertTrue(r.phones.any { it.contains("0612345678") })
        assertTrue(r.urls.contains("https://acme.fr"))
    }

    @Test fun multiEmails() {
        val r = OcrPostProcessor.process("a@b.fr et c@d.fr")
        assertEquals(2, r.emails.size)
    }

    @Test fun lignesNettoyees() {
        val r = OcrPostProcessor.process("  ligne1  \n\n  ligne2  ")
        assertEquals(listOf("ligne1", "ligne2"), r.lines)
    }

    @Test fun extraitCarteDimoCommeSurAppareil() {
        val text = """
            dimo
            SOFTWARE
            Jean Pau! GENOUX
            Directeur Général/CEO
            jpgenoux@dimosoftware.com
            Mobile : +33 (0)6 74 64 05 44 / Tél. : +33 (0)4 72 86 O1 92
            Siège Social
            561, alée des Noisetiers - 69 760 Limonest - France
            www.dimosoftware.fr
        """.trimIndent()
        val r = OcrPostProcessor.process(text)
        assertEquals(listOf("jpgenoux@dimosoftware.com"), r.emails)
        assertTrue(r.phones.any { it == "+33674640544" || it.endsWith("674640544") })
        assertTrue(r.phones.any { it.contains("472860192") || it.endsWith("472860192") })
        assertTrue(r.urls.any { it.contains("dimosoftware.fr") })
    }
}
