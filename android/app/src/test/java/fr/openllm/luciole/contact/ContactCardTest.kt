package fr.openllm.luciole.contact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactCardTest {
    @Test fun champsPartiels() {
        val c = ContactCard(phones = listOf("+33612345678"))
        assertEquals("", c.displayName())
        assertTrue(c.hasAnyField())
    }

    @Test fun displayNamePrioriseFullName() {
        val c = ContactCard(fullName = "Jean Dupont", firstName = "Jean", lastName = "Dupont")
        assertEquals("Jean Dupont", c.displayName())
    }

    @Test fun displayNameDepuisPrenomNom() {
        val c = ContactCard(firstName = "Jean", lastName = "Dupont")
        assertEquals("Jean Dupont", c.displayName())
    }

    @Test fun listesMultiples() {
        val c = ContactCard(
            phones = listOf("0612345678", "0145678901"),
            emails = listOf("a@b.fr", "c@d.fr"),
        )
        assertEquals(2, c.phones.size)
        assertEquals(2, c.emails.size)
    }

    @Test fun videSansChamp() {
        assertFalse(ContactCard().hasAnyField())
    }
}
