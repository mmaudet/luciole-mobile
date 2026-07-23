package fr.openllm.luciole.contact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VCardSerializerTest {
    @Test fun vcardMinimal() {
        val vcf = VCardSerializer.toVCard3(ContactCard(fullName = "Jean Dupont"))
        assertTrue(vcf.contains("BEGIN:VCARD"))
        assertTrue(vcf.contains("FN:Jean Dupont"))
        assertTrue(vcf.contains("END:VCARD"))
    }

    @Test fun accentsEtEchappement() {
        val vcf = VCardSerializer.toVCard3(
            ContactCard(
                fullName = "Élodie Martin",
                note = "Ligne1\nLigne2",
                company = "Société; Générale",
            )
        )
        assertTrue(vcf.contains("FN:Élodie Martin"))
        assertTrue(vcf.contains("NOTE:Ligne1\\nLigne2"))
        assertTrue(vcf.contains("ORG:Société\\; Générale"))
    }

    @Test fun multiTelEtEmail() {
        val vcf = VCardSerializer.toVCard3(
            ContactCard(
                fullName = "Test",
                phones = listOf("0612345678", "0145678901"),
                emails = listOf("a@b.fr", "c@d.fr"),
            )
        )
        assertEquals(2, vcf.split('\n').count { it.startsWith("TEL;") })
        assertEquals(2, vcf.split('\n').count { it.startsWith("EMAIL;") })
    }

    @Test fun escapeUnitaire() {
        assertEquals("a\\;b", VCardSerializer.escape("a;b"))
    }
}
