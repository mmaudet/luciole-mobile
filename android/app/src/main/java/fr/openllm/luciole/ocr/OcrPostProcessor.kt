package fr.openllm.luciole.ocr

object OcrPostProcessor {
    private val emailRegex = Regex("""[\w.+-]+@[\w.-]+\.\w{2,}""", RegexOption.IGNORE_CASE)
    private val urlRegex = Regex(
        """(?:https?://[\w./?#&=%+-]+)|(?:www\.[\w.-]+\.\w{2,}(?:/[\w./?#&=%+-]*)?)""",
        RegexOption.IGNORE_CASE,
    )
    private val phoneRegex = Regex("""(?:\+?\d[\d\s.\-()]{6,}\d)""")

    fun process(rawText: String): OcrResult {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val joined = lines.joinToString("\n")
        // Corrige O/0 uniquement dans les séquences numériques (erreurs OCR fréquentes).
        val phoneSource = joined
            .replace(Regex("""(?<=\d)O(?=\d)"""), "0")
            .replace(Regex("""\bO(?=\d)"""), "0")
        val phones = phoneRegex.findAll(phoneSource)
            .map { normalizePhone(it.value) }
            .filter { isPlausiblePhone(it) }
            .distinct()
            .toList()
        val emails = emailRegex.findAll(joined).map { it.value.lowercase() }.distinct().toList()
        val urls = urlRegex.findAll(joined)
            .map { normalizeUrl(it.value) }
            .distinct()
            .toList()
        return OcrResult(
            rawText = joined,
            lines = lines,
            phones = phones,
            emails = emails,
            urls = urls,
        )
    }

    fun enrich(base: OcrResult, rawText: String): OcrResult {
        val extra = process(rawText)
        return base.copy(
            rawText = extra.rawText.ifBlank { base.rawText },
            lines = extra.lines.ifEmpty { base.lines },
            phones = (base.phones + extra.phones).distinct(),
            emails = (base.emails + extra.emails).distinct(),
            urls = (base.urls + extra.urls).distinct(),
        )
    }

    internal fun normalizePhone(raw: String): String {
        var s = raw.filter { it.isDigit() || it == '+' }
        // +33(0)6... → +336...
        if (s.startsWith("+33") && s.length > 4 && s[3] == '0') {
            s = "+33" + s.substring(4)
        }
        if (s.startsWith("0033")) {
            s = "+33" + s.removePrefix("0033").removePrefix("0")
        }
        return s
    }

    private fun isPlausiblePhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length in 10..15
    }

    private fun normalizeUrl(raw: String): String {
        val t = raw.trim().trimEnd('.', ',', ';')
        return if (t.startsWith("http://", true) || t.startsWith("https://", true)) t
        else "https://$t"
    }
}
