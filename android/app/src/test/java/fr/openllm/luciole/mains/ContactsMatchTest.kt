package fr.openllm.luciole.mains

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactsMatchTest {
    private val carnet = listOf(
        Contact("Paul Maudet", "0612345678"),
        Contact("Marie Curie", "+33700000000"))

    @Test fun trouveParNomComplet() {
        assertEquals("0612345678", Contacts.matchContact("Paul Maudet", carnet))
    }

    @Test fun trouveParPrenom() {
        assertEquals("0612345678", Contacts.matchContact("Paul", carnet))
    }

    @Test fun gardeLePlus() {
        assertEquals("+33700000000", Contacts.matchContact("Marie Curie", carnet))
    }

    @Test fun inconnuRenvoieNull() {
        assertNull(Contacts.matchContact("Zorglub", carnet))
    }

    // Parité Python : le tiret dans un nom de contact est traité comme séparateur,
    // identique à _norm_name qui remplace tout non-alphanum par un espace.
    @Test fun traiteTiretCommeEspace() {
        val carnetHyphen = listOf(Contact("Michel-Marie Dupont", "0699887766"))
        assertEquals("0699887766", Contacts.matchContact("Michel Marie", carnetHyphen))
    }
}
