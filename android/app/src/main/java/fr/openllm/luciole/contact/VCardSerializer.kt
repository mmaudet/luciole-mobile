package fr.openllm.luciole.contact

object VCardSerializer {
    fun toVCard3(card: ContactCard): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        card.fullName?.takeIf { it.isNotBlank() }?.let { appendLine("FN:${escape(it)}") }
        val n = buildNField(card)
        if (n != null) appendLine("N:$n")
        card.company?.takeIf { it.isNotBlank() }?.let { appendLine("ORG:${escape(it)}") }
        card.jobTitle?.takeIf { it.isNotBlank() }?.let { appendLine("TITLE:${escape(it)}") }
        card.phones.forEach { appendLine("TEL;TYPE=CELL:${escape(it)}") }
        card.emails.forEach { appendLine("EMAIL;TYPE=INTERNET:${escape(it)}") }
        card.website?.takeIf { it.isNotBlank() }?.let { appendLine("URL:${escape(it)}") }
        card.address?.takeIf { it.isNotBlank() }?.let { appendLine("ADR;TYPE=WORK:;;${escape(it)};;;;") }
        card.note?.takeIf { it.isNotBlank() }?.let { appendLine("NOTE:${escape(it)}") }
        appendLine("END:VCARD")
    }.trimEnd()

    private fun buildNField(card: ContactCard): String? {
        val last = card.lastName?.takeIf { it.isNotBlank() } ?: return null
        val first = card.firstName.orEmpty()
        return "${escape(last)};${escape(first)};;;"
    }

    internal fun escape(value: String): String =
        value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
            .replace("\r", "")
}
