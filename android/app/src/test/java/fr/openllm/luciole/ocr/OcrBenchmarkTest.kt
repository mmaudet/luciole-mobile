package fr.openllm.luciole.ocr

import kotlin.test.Test
import kotlin.test.assertTrue

/** Harness minimal pour mesurer l'extraction regex sur corpus texte. */
class OcrBenchmarkTest {
    private val samples = listOf(
        "Jean Dupont\nDirecteur\nAcme\n06 12 34 56 78\njean@acme.fr\nhttps://acme.fr",
        "Marie Martin · +33 1 45 67 89 01 · marie@corp.io",
        "Bob Lee\nSales\nbob.lee@example.com\n0611223344",
    )

    @Test fun benchmarkExtractionRegex() {
        var phones = 0
        var emails = 0
        var urls = 0
        samples.forEach { text ->
            val r = OcrPostProcessor.process(text)
            phones += r.phones.size
            emails += r.emails.size
            urls += r.urls.size
        }
        assertTrue(phones >= 3)
        assertTrue(emails >= 3)
        assertTrue(urls >= 1)
    }
}
