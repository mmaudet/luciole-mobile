package fr.openllm.luciole.mains

import fr.openllm.luciole.contact.ContactCard
import fr.openllm.luciole.model.Action
import kotlin.test.Test
import kotlin.test.assertEquals

class MainsScanTest {
    @Test fun scannerCarteOuvreFluxScan() {
        val s = Mains.traiter(Action.ScannerCarte, "scanne une carte de visite") { null }
        assertEquals(Sortie.OuvrirScanCarte, s)
    }

    @Test fun creerContactOuvreCarnet() {
        val card = ContactCard(fullName = "Jean Dupont", phones = listOf("0612345678"))
        val s = Mains.traiter(Action.CreerContact(card), "ajoute Jean Dupont") { null }
        assertEquals(Sortie.CreerContact(card), s)
    }
}
