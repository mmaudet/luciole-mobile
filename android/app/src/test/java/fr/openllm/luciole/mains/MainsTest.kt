package fr.openllm.luciole.mains
import fr.openllm.luciole.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
class MainsTest {
    @Test fun appelAvecNumeroDansLaPhrase() {
        val s = Mains.traiter(Action.Appel("ignoré"), "appelle le 06 12 34 56 78") { null }
        assertEquals(Sortie.Lancer(IntentSpec("android.intent.action.DIAL", "tel:0612345678")), s)
    }
    @Test fun appelParNomResolu() {
        val s = Mains.traiter(Action.Appel("Paul Maudet"), "appelle Paul Maudet") { "0612345678" }
        assertEquals(Sortie.Lancer(IntentSpec("android.intent.action.DIAL", "tel:0612345678")), s)
    }
    @Test fun appelContactIntrouvable() {
        val s = Mains.traiter(Action.Appel("Zorglub"), "appelle Zorglub") { null }
        assertEquals(Sortie.ContactIntrouvable("Zorglub"), s)
    }
    @Test fun noteEstUnAffichage() {
        assertEquals(Sortie.Afficher(AffichageType.NOTE, "acheter du pain"),
            Mains.traiter(Action.Note("acheter du pain"), "note d'acheter du pain") { null })
    }

    // --- Alarme ---
    @Test fun alarmeHeureEtExtras() {
        val s = Mains.traiter(Action.Alarme("14:30", "réveil"), "alarme 14h30") { null }
        assertEquals(Sortie.Lancer(IntentSpec(
            "android.intent.action.SET_ALARM",
            extras = mapOf(
                "android.intent.extra.alarm.HOUR" to 14,
                "android.intent.extra.alarm.MINUTES" to 30,
                "android.intent.extra.alarm.MESSAGE" to "réveil"
            )
        )), s)
    }

    // --- Minuteur ---
    @Test fun minuteurConversionSecondes() {
        val s = Mains.traiter(Action.Minuteur(5, "cuisson"), "minuteur 5 minutes") { null }
        assertEquals(Sortie.Lancer(IntentSpec(
            "android.intent.action.SET_TIMER",
            extras = mapOf(
                "android.intent.extra.alarm.LENGTH" to 300,
                "android.intent.extra.alarm.MESSAGE" to "cuisson"
            )
        )), s)
    }

    // --- Agenda ---
    @Test fun agendaTitreEtLieu() {
        val s = Mains.traiter(Action.Agenda("Dentiste", "demain", "Paris"), "agenda dentiste demain") { null }
        assertEquals(Sortie.Lancer(IntentSpec(
            "android.intent.action.INSERT",
            "content://com.android.calendar/events",
            extras = mapOf(
                "title" to "Dentiste",
                "eventLocation" to "Paris"
            )
        )), s)
    }

    // --- Message email ---
    @Test fun messageEmailSujetEtCorps() {
        val s = Mains.traiter(Action.Message(Canal.EMAIL, "Réunion", "Bonjour, à demain."), "email") { null }
        assertEquals(Sortie.Lancer(IntentSpec(
            "android.intent.action.SENDTO",
            "mailto:",
            extras = mapOf(
                "android.intent.extra.SUBJECT" to "Réunion",
                "android.intent.extra.TEXT" to "Bonjour, à demain."
            )
        )), s)
    }

    // --- Message SMS ---
    @Test fun messageSmsCorps() {
        val s = Mains.traiter(Action.Message(Canal.SMS, null, "J'arrive dans 10 min"), "sms") { null }
        assertEquals(Sortie.Lancer(IntentSpec(
            "android.intent.action.SENDTO",
            "smsto:",
            extras = mapOf("sms_body" to "J'arrive dans 10 min")
        )), s)
    }

    // --- Ouvrir ---
    @Test fun ouvrirBluetooth() {
        val s = Mains.traiter(Action.Ouvrir(Cible.BLUETOOTH), "ouvre bluetooth") { null }
        assertEquals(Sortie.Lancer(IntentSpec("android.settings.BLUETOOTH_SETTINGS")), s)
    }

    // --- Traduction ---
    @Test fun traductionAffichageResultat() {
        val s = Mains.traiter(Action.Traduction("hello", LangueCible.ANGLAIS, "bonjour"), "traduis") { null }
        assertEquals(Sortie.Afficher(AffichageType.TRADUCTION, "bonjour"), s)
    }

    // --- Inconnu ---
    @Test fun inconnu() {
        val s = Mains.traiter(Action.Inconnu, "bla bla incompréhensible") { null }
        assertEquals(Sortie.Afficher(AffichageType.INCONNU, ""), s)
    }
}
