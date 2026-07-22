package fr.openllm.luciole.contact

import fr.openllm.luciole.ocr.OcrResult

/**
 * Fusionne trois sources, par priorité de fiabilité :
 * 1. Regex OCR (tél / email / url) — toujours prioritaire
 * 2. Luciole (sémantique) si présent et non vide
 * 3. Heuristiques déterministes (nom / société / poste / adresse)
 */
object ContactDraftMerge {
    fun merge(llm: ContactCard?, ocr: OcrResult): ContactCard {
        val heuristic = ContactHeuristicParser.parse(ocr)
        val base = llm ?: ContactCard()
        return ContactCard(
            fullName = prefer(base.fullName, heuristic.fullName),
            firstName = prefer(base.firstName, heuristic.firstName),
            lastName = prefer(base.lastName, heuristic.lastName),
            company = prefer(base.company, heuristic.company),
            jobTitle = prefer(base.jobTitle, heuristic.jobTitle),
            phones = mergeStrings(ocr.phones, base.phones + heuristic.phones),
            emails = mergeStrings(ocr.emails, base.emails + heuristic.emails),
            website = prefer(ocr.urls.firstOrNull(), prefer(base.website, heuristic.website)),
            address = prefer(base.address, heuristic.address),
            note = prefer(base.note, heuristic.note),
        )
    }

    private fun prefer(primary: String?, fallback: String?): String? =
        primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }

    private fun mergeStrings(preferred: List<String>, extra: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        (preferred + extra).forEach { v ->
            val n = v.trim()
            if (n.isNotEmpty()) seen.add(n)
        }
        return seen.toList()
    }
}
