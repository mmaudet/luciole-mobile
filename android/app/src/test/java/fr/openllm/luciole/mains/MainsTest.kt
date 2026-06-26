package fr.openllm.luciole.mains
import fr.openllm.luciole.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
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
    @Test fun itineraireGeo() {
        val s = Mains.traiter(Action.Itineraire("Gare de Lyon", null), "itinéraire Gare de Lyon") { null }
        assertTrue(s is Sortie.Lancer && (s as Sortie.Lancer).spec.data!!.startsWith("geo:0,0?q="))
    }
}
