package fr.openllm.luciole.contact

import fr.openllm.luciole.ocr.OcrResult

/**
 * Structuration déterministe d'un texte OCR de carte de visite.
 * Complète / remplace Luciole quand le LLM est indisponible ou bridé
 * (ex. serveur llama.cpp avec grammaire d'actions GBNF).
 */
object ContactHeuristicParser {

    private val jobKeywords = Regex(
        """(?i)\b(directeur|directrice|ceo|cto|cfo|coo|président|presidente|""" +
            """manager|responsable|ingénieur|ingenieur|commercial|consultan[te]?|""" +
            """associé|associe|fondateur|founder|head of|chef de|avocat|docteur|dr\.?)\b""",
    )
    private val addressKeywords = Regex(
        """(?i)\b(rue|avenue|av\.?|bd\.?|boulevard|all[ée]e|chemin|place|impasse|""" +
            """cedex|cs\b|bp\b|boîte postale|france|siège|siege)\b|\b\d{5}\b""",
    )
    private val companyKeywords = Regex(
        """(?i)\b(sas|sarl|sa\b|sasu|eurl|sci|ltd|inc|gmbh|software|solutions|""" +
            """group|groupe|technologies|tech|consulting)\b""",
    )
    private val skipLine = Regex(
        """(?i)^(mobile|tél\.?|tel\.?|phone|fax|email|e-mail|mail|www\.|http|""" +
            """siège social|siege social|linkedin|twitter)\b""",
    )
    private val emailOrUrlOrPhone = Regex(
        """@|www\.|https?://|\+?\d[\d\s.\-()]{7,}\d""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(ocr: OcrResult): ContactCard {
        val lines = ocr.lines.ifEmpty { ocr.rawText.lines().map { it.trim() }.filter { it.isNotEmpty() } }
        val residual = lines.toMutableList()

        fun takeMatching(pred: (String) -> Boolean): String? {
            val idx = residual.indexOfFirst(pred)
            if (idx < 0) return null
            return residual.removeAt(idx)
        }

        val jobTitle = takeMatching { jobKeywords.containsMatchIn(it) && !emailOrUrlOrPhone.containsMatchIn(it) }
        val address = residual
            .filter { addressKeywords.containsMatchIn(it) && !emailOrUrlOrPhone.containsMatchIn(it) }
            .maxByOrNull { it.length }
            ?.also { residual.remove(it) }

        val personLine = residual
            .asSequence()
            .filter { looksLikePersonName(it) }
            .maxByOrNull { personScore(it) }
            ?.also { residual.remove(it) }

        val (firstName, lastName, fullName) = splitPersonName(personLine)

        val companyFromEmail = ocr.emails.firstOrNull()?.let { companyFromEmailDomain(it) }
        val companyLine = residual
            .asSequence()
            .filter { looksLikeCompany(it) }
            .maxByOrNull { companyScore(it) }
            ?.also { residual.remove(it) }
        val company = cleanCompany(companyLine) ?: companyFromEmail

        val website = ocr.urls.firstOrNull()
        val noteParts = residual.filter { line ->
            !skipLine.containsMatchIn(line)
                && !emailOrUrlOrPhone.containsMatchIn(line)
                && line.length > 2
        }

        return ContactCard(
            fullName = fullName,
            firstName = firstName,
            lastName = lastName,
            company = company,
            jobTitle = cleanJob(jobTitle),
            phones = ocr.phones,
            emails = ocr.emails,
            website = website,
            address = cleanAddress(address),
            note = noteParts.take(3).joinToString(" · ").ifBlank { null },
        )
    }

    internal fun looksLikePersonName(line: String): Boolean {
        val t = line.replace(Regex("""[^\p{L}\s\-']"""), " ").trim()
        if (t.length !in 4..60) return false
        if (jobKeywords.containsMatchIn(t) || companyKeywords.containsMatchIn(t)) return false
        if (addressKeywords.containsMatchIn(t)) return false
        if (emailOrUrlOrPhone.containsMatchIn(line)) return false
        val parts = t.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (parts.size !in 2..4) return false
        // Au moins un token capitalisé / majuscules
        val caps = parts.count { it.first().isUpperCase() }
        return caps >= 2
    }

    internal fun personScore(line: String): Int {
        val parts = line.replace(Regex("""[^\p{L}\s\-']"""), " ").trim().split(Regex("""\s+"""))
        var s = parts.size * 10
        if (parts.any { it.length >= 2 && it == it.uppercase() && it.any { c -> c.isLetter() } }) s += 40
        if (parts.size in 2..3) s += 15
        return s
    }

    internal fun splitPersonName(line: String?): Triple<String?, String?, String?> {
        if (line.isNullOrBlank()) return Triple(null, null, null)
        val cleaned = line
            .replace('!', 'l') // Pau! → Paul (erreur OCR fréquente)
            .replace(Regex("""[^\p{L}\s\-']"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val parts = cleaned.split(' ').filter { it.isNotBlank() }
        if (parts.isEmpty()) return Triple(null, null, null)
        val lastCaps = parts.indexOfLast { it.length >= 2 && it == it.uppercase() && it.all { c -> !c.isLetter() || c.isUpperCase() } }
        fun titleWord(w: String) = w.lowercase().replaceFirstChar { it.titlecase() }
        return if (lastCaps > 0) {
            val last = titleWord(parts[lastCaps])
            val first = parts.take(lastCaps).joinToString(" ") { titleWord(it) }
            // Conserve les prénoms composés sans forcer une casse bizarre si déjà mixtes
            val firstKeep = parts.take(lastCaps).joinToString(" ")
            Triple(firstKeep, last, "$firstKeep $last")
        } else if (parts.size >= 2) {
            val last = titleWord(parts.last())
            val first = parts.dropLast(1).joinToString(" ")
            Triple(first, last, "$first $last")
        } else {
            Triple(null, null, cleaned)
        }
    }

    private fun looksLikeCompany(line: String): Boolean {
        if (line.length !in 2..50) return false
        if (emailOrUrlOrPhone.containsMatchIn(line)) return false
        if (jobKeywords.containsMatchIn(line)) return false
        if (addressKeywords.containsMatchIn(line)) return false
        if (looksLikePersonName(line)) return false
        return companyKeywords.containsMatchIn(line)
            || line == line.uppercase()
            || line.split(Regex("""\s+""")).size <= 3
    }

    private fun companyScore(line: String): Int {
        var s = 0
        if (companyKeywords.containsMatchIn(line)) s += 50
        if (line == line.uppercase()) s += 20
        if (line.contains("software", ignoreCase = true)) s += 30
        return s + (40 - line.length).coerceAtLeast(0)
    }

    private fun cleanCompany(line: String?): String? {
        if (line.isNullOrBlank()) return null
        return line
            .replace(Regex("""[&—–\-_|]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { null }
    }

    private fun cleanJob(line: String?): String? =
        line?.replace(Regex("""\s+"""), " ")?.trim()?.ifBlank { null }

    private fun cleanAddress(line: String?): String? =
        line?.replace(Regex("""\s+"""), " ")?.trim()?.ifBlank { null }

    internal fun companyFromEmailDomain(email: String): String? {
        val domain = email.substringAfter('@', "").substringBefore('.')
        if (domain.length < 3) return null
        // dimosoftware → Dimo Software (best-effort)
        val spaced = domain
            .replace(Regex("""(?<=[a-z])(?=[A-Z])"""), " ")
            .replace("software", " Software", ignoreCase = true)
            .replace("solutions", " Solutions", ignoreCase = true)
            .trim()
        return spaced.replaceFirstChar { it.titlecase() }.ifBlank { null }
    }
}
