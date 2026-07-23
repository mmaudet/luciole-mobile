package fr.openllm.luciole.contact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactCardJsonTest {
    @Test fun parseComplet() {
        val json = """
            {
              "full_name": "Jean Dupont",
              "first_name": "Jean",
              "last_name": "Dupont",
              "company": "Acme",
              "job_title": "Directeur",
              "phones": ["+33612345678"],
              "emails": ["jean.dupont@acme.fr"],
              "website": "https://acme.fr",
              "address": "12 rue Exemple, 75000 Paris",
              "note": "texte ambigu"
            }
        """.trimIndent()
        val c = ContactCardJson.parse(json)!!
        assertEquals("Jean Dupont", c.fullName)
        assertEquals("Acme", c.company)
        assertEquals(listOf("+33612345678"), c.phones)
    }

    @Test fun parsePartiel() {
        val c = ContactCardJson.parse("""{"emails":["x@y.fr"]}""")!!
        assertEquals(listOf("x@y.fr"), c.emails)
        assertNull(c.fullName)
    }

    @Test fun jsonInvalideRetourneNull() {
        assertNull(ContactCardJson.parse("pas du json"))
    }

    @Test fun parseDepuisFenceMarkdown() {
        val raw = """
            Voici le contact :
            ```json
            {"full_name":"Marie Curie","company":"Radium"}
            ```
        """.trimIndent()
        val c = ContactCardJson.parseFromLlm(raw)!!
        assertEquals("Marie Curie", c.fullName)
    }

    @Test fun parseCreerContactAvecType() {
        val c = ContactCardJson.parse(
            """{"type":"creer_contact","full_name":"Jean Dupont","phones":["0612345678"]}""",
        )!!
        assertEquals("Jean Dupont", c.fullName)
        assertEquals(listOf("0612345678"), c.phones)
    }

    @Test fun refuseActionAppel() {
        assertNull(ContactCardJson.parse("""{"type":"appel","destinataire":"Paul"}"""))
    }
}
