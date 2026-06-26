package fr.openllm.luciole.mains

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtractionTest {
    // --- existing tests ---

    @Test fun callTargetRetireLeVerbe() {
        assertEquals("Paul Maudet", Extraction.callTarget("appelle Paul Maudet"))
        assertEquals("Marie", Extraction.callTarget("téléphone à Marie"))
    }

    @Test fun extractPhoneLitLeNumero() {
        assertEquals("0711223344", Extraction.extractPhone("appelle le 07 11 22 33 44"))
        assertEquals("+33612345678", Extraction.extractPhone("appelle le +33 6 12 34 56 78"))
    }

    @Test fun extractPhoneNullSansNumero() {
        assertNull(Extraction.extractPhone("appelle Paul"))
    }

    // --- new tests covering previously hidden bugs ---

    @Test fun callTargetApelles() {
        // 'appelles' (forme tu) — was typo'd as 'appeles' (single-l)
        assertEquals("Marie", Extraction.callTarget("appelles Marie"))
    }

    @Test fun callTargetComposeJoins() {
        // verbs compose and joins from Python's _CALL_PREFIX
        assertEquals("Marie", Extraction.callTarget("compose Marie"))
        assertEquals("Paul", Extraction.callTarget("joins Paul"))
    }

    @Test fun extractPhoneMeilleurMatch() {
        // Two numbers; the SECOND has more digits → must return the longer one (not the first)
        assertEquals("0711223344", Extraction.extractPhone("entre 07 11 22 et 07 11 22 33 44"))
    }

    @Test fun callTargetPonctuation() {
        // Trailing punctuation stripped like Python's .strip(" .,!?;:'\"«»")
        assertEquals("Marie", Extraction.callTarget("appelle Marie !"))
        assertEquals("Marie", Extraction.callTarget("appelle Marie."))
    }

    @Test fun extractPhoneBoundary6Chiffres() {
        // Exactly 6 digits: should return the number
        assertEquals("123456", Extraction.extractPhone("compose le 123456"))
        // 5 digits: below floor, must return null
        assertNull(Extraction.extractPhone("1 2345"))
    }
}
