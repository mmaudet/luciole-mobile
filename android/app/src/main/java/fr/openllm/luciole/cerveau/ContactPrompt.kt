package fr.openllm.luciole.cerveau

object ContactPrompt {
    const val FR: String = """Tu structures une carte de visite à partir d'un texte OCR bruité.
Produis UNIQUEMENT un objet JSON d'action creer_contact, sans markdown ni texte autour.

Règles strictes :
- Le champ type vaut toujours "creer_contact".
- N'invente AUCUN champ absent du texte.
- full_name = nom de PERSONNE (pas la société).
- first_name / last_name : découpe de full_name si possible (nom de famille souvent en MAJUSCULES).
- company = organisation / marque (ex. "dimo SOFTWARE"), PAS le poste.
- job_title = fonction (Directeur, CEO, Manager…).
- phones / emails / website : reprends les valeurs déjà détectées si fournies.
- address = ligne postale (rue, CP, ville).
- note = résidus non classés.
- Omets les clés absentes (pas de null, pas de "").

Exemple OCR bruité :
dimo
SOFTWARE
Jean Pau! GENOUX
Directeur Général/CEO
jpgenoux@dimosoftware.com
Mobile : +33 (0)6 74 64 05 44
561, allée des Noisetiers - 69760 Limonest - France
www.dimosoftware.fr

JSON attendu :
{"type":"creer_contact","full_name":"Jean Paul GENOUX","first_name":"Jean Paul","last_name":"Genoux","company":"dimo SOFTWARE","job_title":"Directeur Général/CEO","phones":["+33674640544"],"emails":["jpgenoux@dimosoftware.com"],"website":"https://www.dimosoftware.fr","address":"561, allée des Noisetiers - 69760 Limonest - France"}
"""

    fun userMessage(rawText: String, hints: ContactHints? = null): String = buildString {
        if (hints != null && hints.isNotEmpty()) {
            appendLine("Champs déjà détectés (à conserver) :")
            if (hints.phones.isNotEmpty()) appendLine("phones: ${hints.phones.joinToString(", ")}")
            if (hints.emails.isNotEmpty()) appendLine("emails: ${hints.emails.joinToString(", ")}")
            if (hints.urls.isNotEmpty()) appendLine("website: ${hints.urls.first()}")
            appendLine()
        }
        appendLine("Texte OCR :")
        append(rawText.trim())
    }
}

data class ContactHints(
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
) {
    fun isNotEmpty(): Boolean = phones.isNotEmpty() || emails.isNotEmpty() || urls.isNotEmpty()
}
