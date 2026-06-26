package fr.openllm.luciole.cerveau

import fr.openllm.luciole.model.Action

interface Cerveau {
    suspend fun suggest(phrase: String): Action
}

class CerveauIndisponible(cause: Throwable) : Exception(cause)
