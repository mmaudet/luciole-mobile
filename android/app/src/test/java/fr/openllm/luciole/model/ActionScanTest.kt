package fr.openllm.luciole.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ActionScanTest {
    @Test fun parseScannerCarte() {
        assertEquals(Action.ScannerCarte, ActionJson.parse("""{"type":"scanner_carte"}"""))
    }

    @Test fun parseCreerContact() {
        val action = ActionJson.parse(
            """{"type":"creer_contact","full_name":"Jean Dupont","phones":["0612345678"]}""",
        )
        assertEquals(
            Action.CreerContact(
                fr.openllm.luciole.contact.ContactCard(
                    fullName = "Jean Dupont",
                    phones = listOf("0612345678"),
                ),
            ),
            action,
        )
    }
}
