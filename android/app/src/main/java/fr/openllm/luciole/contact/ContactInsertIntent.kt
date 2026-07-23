package fr.openllm.luciole.contact

import android.content.Intent
import android.provider.ContactsContract

object ContactInsertIntent {
    fun build(card: ContactCard): Intent {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
        card.fullName?.takeIf { it.isNotBlank() }?.let {
            intent.putExtra(ContactsContract.Intents.Insert.NAME, it)
        }
        card.company?.takeIf { it.isNotBlank() }?.let {
            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, it)
        }
        card.jobTitle?.takeIf { it.isNotBlank() }?.let {
            intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it)
        }
        card.phones.firstOrNull()?.let {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, it)
        }
        card.emails.firstOrNull()?.let {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, it)
        }
        card.address?.takeIf { it.isNotBlank() }?.let {
            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, it)
        }
        card.note?.takeIf { it.isNotBlank() }?.let {
            intent.putExtra(ContactsContract.Intents.Insert.NOTES, it)
        }
        return intent
    }
}
