package fr.openllm.luciole.contact

import android.content.Intent
import android.provider.ContactsContract
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ContactInsertIntentTest {
    @Test fun extrasPrenomNomEtTelephone() {
        val card = ContactCard(
            fullName = "Jean Dupont",
            company = "Acme",
            jobTitle = "Directeur",
            phones = listOf("0612345678"),
            emails = listOf("jean@acme.fr"),
            address = "Paris",
            note = "note test",
        )
        val intent = ContactInsertIntent.build(card)
        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals("Jean Dupont", intent.getStringExtra(ContactsContract.Intents.Insert.NAME))
        assertEquals("0612345678", intent.getStringExtra(ContactsContract.Intents.Insert.PHONE))
        assertEquals("jean@acme.fr", intent.getStringExtra(ContactsContract.Intents.Insert.EMAIL))
        assertEquals("Acme", intent.getStringExtra(ContactsContract.Intents.Insert.COMPANY))
    }
}
