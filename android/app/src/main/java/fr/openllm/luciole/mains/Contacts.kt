package fr.openllm.luciole.mains

import android.content.ContentResolver
import android.provider.ContactsContract
import java.text.Normalizer

data class Contact(val name: String, val number: String)

object Contacts {
    /**
     * Mirror de Python _norm_name :
     *  - NFKD (pas NFD) pour les équivalences de compatibilité
     *  - supprime les combinaisons (accents)
     *  - remplace tout caractère non alphanumérique (tiret, apostrophe…) par un espace
     *  - minuscules, espaces intérieurs réduits, trim
     *
     * Différence avec le brief (qui utilisait NFD + trim seulement) :
     * le tiret dans "Michel-Marie" devient un espace → deux tokens, idem Python.
     */
    private fun norm(s: String): String {
        val decomposed = Normalizer.normalize(s, Normalizer.Form.NFKD)
        val noAccents = decomposed.replace(Regex("\\p{Mn}+"), "")
        val spaced = noAccents.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
        return spaced.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun tokens(s: String): Set<String> = norm(s).split(" ").filter { it.isNotBlank() }.toSet()

    private fun digits(number: String): String {
        val plus = number.trimStart().startsWith("+")
        val d = number.filter { it.isDigit() }
        return if (plus) "+$d" else d
    }

    /**
     * Meilleur numéro (chiffres, + initial conservé) pour un nom prononcé parmi [contacts].
     * Renvoie null si aucun contact n'atteint le plancher de confiance 0.5.
     *
     * Tiers identiques à Python match_contact :
     *   exact  → 1.0
     *   tous les tokens de la requête présents dans le nom → 0.9
     *   recouvrement partiel → 0.7 × |intersection| / |query_tokens|   ← facteur 0.7 comme Python
     *   aucun recouvrement  → 0.0
     *
     * Note : le brief omettait le facteur 0.7 sur le recouvrement partiel (divergence corrigée).
     */
    fun matchContact(query: String, contacts: List<Contact>): String? {
        val q = norm(query)
        if (q.isBlank()) return null
        val qtok = tokens(query)
        var best: Contact? = null
        var bestScore = 0.0
        for (c in contacts) {
            val n = norm(c.name)
            if (n.isBlank() || c.number.isBlank()) continue
            val ntok = tokens(c.name)
            val common = (qtok intersect ntok).size
            val score = when {
                n == q -> 1.0
                qtok.isNotEmpty() && ntok.containsAll(qtok) -> 0.9
                common > 0 -> 0.7 * common.toDouble() / qtok.size
                else -> 0.0
            }
            if (score > bestScore) { bestScore = score; best = c }
        }
        return if (bestScore >= 0.5) best?.number?.let(::digits) else null
    }

    /**
     * Résout [name] via Android ContactsContract.
     * Interroge tous les numéros de téléphone, délègue la sélection à [matchContact].
     * (Non testable en unit test JVM pur ; vérifié sur émulateur / appareil.)
     */
    fun resolve(name: String, resolver: ContentResolver): String? {
        val out = ArrayList<Contact>()
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            proj, null, null, null
        )?.use { cur ->
            while (cur.moveToNext()) {
                out.add(Contact(cur.getString(0) ?: "", cur.getString(1) ?: ""))
            }
        }
        return matchContact(name, out)
    }
}
