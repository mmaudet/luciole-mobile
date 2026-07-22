package fr.openllm.luciole.ocr

import kotlin.test.Test
import kotlin.test.assertTrue

class OcrOrientationTest {
    @Test fun scorePrefereEmailEtTelephone() {
        val weak = OcrResult(rawText = "abc", phones = emptyList(), emails = emptyList())
        val strong = OcrResult(
            rawText = "Jean Paul GENOUX\njpgenoux@dimosoftware.com\n+33674640544",
            phones = listOf("+33674640544"),
            emails = listOf("jpgenoux@dimosoftware.com"),
            urls = listOf("https://www.dimosoftware.fr"),
        )
        assertTrue(OcrOrientation.score(strong) > OcrOrientation.score(weak))
    }
}
