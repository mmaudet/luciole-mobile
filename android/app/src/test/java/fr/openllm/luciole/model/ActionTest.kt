package fr.openllm.luciole.model
import kotlin.test.Test
import kotlin.test.assertEquals
class ActionTest {
    @Test fun appelPorteLeDestinataire() {
        val a: Action = Action.Appel("0612345678")
        assertEquals("0612345678", (a as Action.Appel).destinataire)
    }
    @Test fun ouvrirUtiliseLEnumCible() {
        assertEquals(Cible.YOUTUBE, (Action.Ouvrir(Cible.YOUTUBE) as Action.Ouvrir).cible)
    }
}
