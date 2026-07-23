package fr.openllm.luciole.model

import fr.openllm.luciole.contact.ContactCard
import fr.openllm.luciole.contact.ContactCardJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object ActionJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String): Action {
        val obj = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
            ?: return Action.Inconnu
        fun s(k: String): String? = obj[k]?.jsonPrimitive?.contentOrNull
        fun i(k: String): Int? = obj[k]?.jsonPrimitive?.intOrNull
        return when (s("type")) {
            "appel" -> Action.Appel(s("destinataire").orEmpty())
            "alarme" -> Action.Alarme(s("heure").orEmpty(), s("libelle").orEmpty())
            "minuteur" -> Action.Minuteur(i("duree_min") ?: 0, s("libelle"))
            "agenda" -> Action.Agenda(s("titre").orEmpty(), s("quand").orEmpty(), s("lieu"))
            "message" -> Action.Message(
                if (s("canal") == "sms") Canal.SMS else Canal.EMAIL, s("objet"), s("corps").orEmpty())
            "itineraire" -> Action.Itineraire(s("destination").orEmpty(), s("mode"))
            "note" -> Action.Note(s("texte").orEmpty())
            "recherche" -> Action.Recherche(s("requete").orEmpty())
            "ouvrir" -> runCatching { Cible.valueOf(s("cible").orEmpty().uppercase()) }
                .getOrNull()?.let { Action.Ouvrir(it) } ?: Action.Inconnu
            "traduction" -> runCatching { LangueCible.valueOf(s("cible").orEmpty().uppercase()) }
                .getOrNull()?.let { Action.Traduction(s("texte").orEmpty(), it, s("resultat").orEmpty()) }
                ?: Action.Inconnu
            "scanner_carte" -> Action.ScannerCarte
            "creer_contact" -> Action.CreerContact(ContactCardJson.parse(raw) ?: ContactCard())
            else -> Action.Inconnu
        }
    }
}
