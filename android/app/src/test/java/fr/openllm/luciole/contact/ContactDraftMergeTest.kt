package fr.openllm.luciole.contact

import fr.openllm.luciole.ocr.OcrResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactDraftMergeTest {
    @Test fun fusionneRegexEtLlm() {
        val llm = ContactCard(fullName = "Jean Dupont", phones = listOf("0612345678"))
        val ocr = OcrResult(
            rawText = "Jean Dupont\n0612345678\njean@acme.fr",
            phones = listOf("0612345678", "0145678901"),
            emails = listOf("jean@acme.fr"),
            urls = listOf("https://acme.fr"),
        )
        val merged = ContactDraftMerge.merge(llm, ocr)
        assertEquals("Jean Dupont", merged.fullName)
        assertEquals(listOf("0612345678", "0145678901"), merged.phones)
        assertEquals(listOf("jean@acme.fr"), merged.emails)
        assertEquals("https://acme.fr", merged.website)
    }

    @Test fun fallbackRegexSiLlmNull() {
        val ocr = OcrResult(
            rawText = "x",
            phones = listOf("0611111111"),
            emails = listOf("a@b.fr"),
            urls = listOf("https://x.fr"),
        )
        val merged = ContactDraftMerge.merge(null, ocr)
        assertEquals(listOf("0611111111"), merged.phones)
        assertEquals("https://x.fr", merged.website)
    }
}
