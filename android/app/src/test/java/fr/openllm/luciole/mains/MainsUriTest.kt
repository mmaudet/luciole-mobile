package fr.openllm.luciole.mains

import android.content.Intent
import fr.openllm.luciole.model.Action
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Tests Uri-encoding réels (Itinéraire, Recherche).
 * Utilise Robolectric pour que android.net.Uri.encode() retourne une vraie valeur
 * au lieu de null (comportement du stub JVM avec isReturnDefaultValues=true).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MainsUriTest {

    @Test fun itineraireUriComplete() {
        val s = Mains.traiter(Action.Itineraire("Gare de Lyon", null), "itinéraire Gare de Lyon") { null }
        assertEquals(
            Sortie.Lancer(IntentSpec(Intent.ACTION_VIEW, "geo:0,0?q=Gare%20de%20Lyon")),
            s
        )
    }

    @Test fun rechercheUriComplete() {
        val s = Mains.traiter(Action.Recherche("tour Eiffel"), "cherche tour Eiffel") { null }
        assertEquals(
            Sortie.Lancer(IntentSpec(Intent.ACTION_VIEW, "https://www.qwant.com/?q=tour%20Eiffel")),
            s
        )
    }
}
