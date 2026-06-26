package fr.openllm.luciole.mains

object Extraction {
    // Mirrors Python's _CALL_PREFIX from dispatcher/intents.py exactly (verbs: appelle|appeler|appelles|appelez|
    // t[ée]l[ée]phone[rz]?|compose[rz]?|joins). 'appelles' placed before 'appelle' to prevent
    // the shorter alternative from matching first (alternation is ordered/leftmost-first).
    private val callPrefix = Regex(
        """^\s*(?:appelles|appeler|appelle|appelez|t[ée]l[ée]phone[rz]?|compose[rz]?|joins)\s*(?:à|au|aux|le|la|l[''])?\s*""",
        RegexOption.IGNORE_CASE
    )

    // Mirrors Python's _PHONE_RE: r"\+?\d[\d \-. ]{4,}\d"
    private val phoneRe = Regex("""\+?\d[\d \-. ]{4,}\d""")

    fun callTarget(phrase: String): String =
        phrase.replaceFirst(callPrefix, "").trim { it in " .,!?;:'\"«»" }

    fun extractPhone(phrase: String): String? {
        // Collect all matches, pick the one with the most digits (mirrors Python's extract_phone)
        var best = ""
        for (m in phoneRe.findAll(phrase)) {
            val v = m.value
            if (v.filter { it.isDigit() }.length > best.filter { it.isDigit() }.length) {
                best = v
            }
        }
        if (best.isEmpty()) return null
        val plus = best.startsWith("+")
        val digits = best.filter { it.isDigit() }
        if (digits.length < 6) return null
        return if (plus) "+$digits" else digits
    }
}
