package fr.openllm.luciole.cerveau

import fr.openllm.luciole.contact.ContactCard
import fr.openllm.luciole.model.Action

interface Cerveau {
    suspend fun suggest(phrase: String): Action
    suspend fun extractContact(rawText: String): ContactCard?
}

class CerveauIndisponible(cause: Throwable) : Exception(cause)
