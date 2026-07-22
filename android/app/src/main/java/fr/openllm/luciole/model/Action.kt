package fr.openllm.luciole.model

enum class Canal { EMAIL, SMS }
enum class Cible { YOUTUBE, MAPS, CHROME, APPAREIL_PHOTO, PARAMETRES, BLUETOOTH, WIFI }
enum class LangueCible { ANGLAIS, ESPAGNOL, ALLEMAND, ITALIEN, PORTUGAIS }

sealed interface Action {
    data class Appel(val destinataire: String) : Action
    data class Alarme(val heure: String, val libelle: String) : Action
    data class Minuteur(val dureeMin: Int, val libelle: String?) : Action
    data class Agenda(val titre: String, val quand: String, val lieu: String?) : Action
    data class Message(val canal: Canal, val objet: String?, val corps: String) : Action
    data class Itineraire(val destination: String, val mode: String?) : Action
    data class Note(val texte: String) : Action
    data class Recherche(val requete: String) : Action
    data class Ouvrir(val cible: Cible) : Action
    data class Traduction(val texte: String, val cible: LangueCible, val resultat: String) : Action
    data object ScannerCarte : Action
    data object Inconnu : Action
}
