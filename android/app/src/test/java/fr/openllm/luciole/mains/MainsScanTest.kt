package fr.openllm.luciole.mains

import fr.openllm.luciole.model.Action
import kotlin.test.Test
import kotlin.test.assertEquals

class MainsScanTest {
    @Test fun scannerCarteOuvreFluxScan() {
        val s = Mains.traiter(Action.ScannerCarte, "scanne une carte de visite") { null }
        assertEquals(Sortie.OuvrirScanCarte, s)
    }
}
