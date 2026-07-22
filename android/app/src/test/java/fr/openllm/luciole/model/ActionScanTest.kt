package fr.openllm.luciole.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ActionScanTest {
    @Test fun parseScannerCarte() {
        assertEquals(Action.ScannerCarte, ActionJson.parse("""{"type":"scanner_carte"}"""))
    }
}
