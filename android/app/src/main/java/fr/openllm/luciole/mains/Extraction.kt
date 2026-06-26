package fr.openllm.luciole.mains

object Extraction {
    // Extended regex to match Python's _CALL_PREFIX from dispatcher/intents.py
    // Covers: appelle[rz]?, appeler, appelles, appelez, t[ée]l[ée]phone[rz]?, compose[rz]?, joins
    // Plus the brief's new verbs: passe[rz]? un appel, contacte[rz]?
    private val callPrefix = Regex(
        """^\s*(?:appelle[rz]?|appeler|appeles|appelez|t[ée]l[ée]phone[rz]?|compose[rz]?|joins|passe[rz]?\s+un\s+appel|contacte[rz]?)\s*(?:à|au|aux|le|la|l[''])?\s*""",
        RegexOption.IGNORE_CASE
    )

    fun callTarget(phrase: String): String = phrase.replaceFirst(callPrefix, "").trim()

    fun extractPhone(phrase: String): String? {
        // Match phone patterns: optional +, digit, then 4+ chars of digits/spaces/dots/dashes, end with digit
        val m = Regex("""\+?\d[\d \.\-]{4,}\d""").find(phrase) ?: return null
        val raw = m.value
        val plus = raw.trimStart().startsWith("+")
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 6) return null
        return if (plus) "+$digits" else digits
    }
}
