package fr.openllm.luciole.mains
import fr.openllm.luciole.model.*
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

    // --- Agenda avec date/heure résolue ---

    @Test fun agendaAvecQuandResolvable() {
        // now fixé : samedi 27 juin 2026 09:00 ; quand = "demain 10:00" → 28 juin 2026 10:00
        val now = LocalDateTime.of(2026, 6, 27, 9, 0)
        val epochAttendu = LocalDateTime.of(2026, 6, 28, 10, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val s = Mains.traiter(Action.Agenda("Réunion", "demain 10:00", null), "réunion demain", now) { null }
        assertTrue(s is Sortie.Lancer)
        val extras = s.spec.extras
        assertEquals("Réunion", extras["title"])
        // Clés CalendarContract.EXTRA_EVENT_BEGIN_TIME / END_TIME = "beginTime" / "endTime"
        assertEquals(epochAttendu, extras["beginTime"] as? Long)
        assertEquals(epochAttendu + 3_600_000L, extras["endTime"] as? Long)
    }

    @Test fun agendaQuandVideResteTitleOnly() {
        // quand non parseable → pas d'extras begin/end
        val s = Mains.traiter(Action.Agenda("Note", "", null), "agenda note") { null }
        assertTrue(s is Sortie.Lancer)
        val extras = s.spec.extras
        assertEquals("Note", extras["title"])
        assertFalse(extras.containsKey("beginTime"))
        assertFalse(extras.containsKey("endTime"))
    }

    @Test fun agendaQuandNonParseable() {
        // quand garbage → pas d'extras begin/end
        val s = Mains.traiter(Action.Agenda("Dentiste", "garbage xyz", null), "agenda dentiste") { null }
        assertTrue(s is Sortie.Lancer)
        val extras = s.spec.extras
        assertEquals("Dentiste", extras["title"])
        assertFalse(extras.containsKey("beginTime"))
        assertFalse(extras.containsKey("endTime"))
    }
}
