package fr.openllm.luciole.contact

import fr.openllm.luciole.ocr.OcrResult

/** Fusionne la structuration Luciole et les extractions regex OCR. */
object ContactDraftMerge {
    fun merge(llm: ContactCard?, ocr: OcrResult): ContactCard {
        val base = llm ?: ContactCard()
        return base.copy(
            phones = mergeStrings(base.phones, ocr.phones),
            emails = mergeStrings(base.emails, ocr.emails),
            website = base.website?.takeIf { it.isNotBlank() } ?: ocr.urls.firstOrNull(),
        )
    }

    private fun mergeStrings(a: List<String>, b: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        (a + b).forEach { v ->
            val n = v.trim()
            if (n.isNotEmpty()) seen.add(n)
        }
        return seen.toList()
    }
}
