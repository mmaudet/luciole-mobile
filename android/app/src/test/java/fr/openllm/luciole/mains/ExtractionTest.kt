package fr.openllm.luciole.mains

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtractionTest {
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
}
