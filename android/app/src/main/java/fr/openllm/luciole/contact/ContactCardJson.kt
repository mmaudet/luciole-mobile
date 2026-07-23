package fr.openllm.luciole.contact

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object ContactCardJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Types d'action Luciole qui ne sont pas un contact. */
    private val ACTION_TYPES = setOf(
        "alarme", "agenda", "message", "itineraire", "appel", "inconnu",
        "minuteur", "note", "recherche", "ouvrir", "traduction", "scanner_carte",
    )

    fun parse(raw: String): ContactCard? {
        val obj = runCatching { json.parseToJsonElement(raw.trim()) as? JsonObject }.getOrNull()
            ?: return null
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
        if (type != null && type != "creer_contact" && type in ACTION_TYPES) return null
        fun s(k: String): String? = obj[k]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        fun strings(k: String): List<String> =
            obj[k]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf { v -> v.isNotBlank() } }
                ?: emptyList()
        return ContactCard(
            fullName = s("full_name"),
            firstName = s("first_name"),
            lastName = s("last_name"),
            company = s("company"),
            jobTitle = s("job_title"),
            phones = strings("phones"),
            emails = strings("emails"),
            website = s("website"),
            address = s("address"),
            note = s("note"),
        )
    }

    /** Extrait un objet JSON de la réponse LLM (avec éventuels fences markdown). */
    fun extractJsonFromLlm(raw: String): String {
        val trimmed = raw.trim()
        val fence = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        fence.find(trimmed)?.groupValues?.get(1)?.trim()?.let { return it }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }

    fun parseFromLlm(raw: String): ContactCard? = parse(extractJsonFromLlm(raw))
}
