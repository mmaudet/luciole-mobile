package fr.openllm.luciole.mains

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Résoud les expressions temporelles françaises définies dans le system_prompt.
 * Port direct de dispatcher/datetime_fr.py — mêmes sémantiques.
 *
 * Formes supportées (exactement celles du prompt) :
 *   "HH:MM"                     → aujourd'hui à HH:MM (sans avancement si passé)
 *   "aujourd'hui HH:MM"         → idem
 *   "demain HH:MM"              → demain à HH:MM
 *   "après-demain HH:MM"        → après-demain à HH:MM
 *   "<jour de la semaine> HH:MM" → prochaine occurrence (jamais aujourd'hui si delta==0)
 *   "dans N minutes"            → now + N minutes
 *   "dans N heures"             → now + N heures
 *
 * Tout autre texte → null.
 */
object DatetimeFr {

    // Ordinals 0-based identiques à Python's datetime.weekday()
    private val WEEKDAY_ORDINALS = mapOf(
        "lundi" to 0, "mardi" to 1, "mercredi" to 2, "jeudi" to 3,
        "vendredi" to 4, "samedi" to 5, "dimanche" to 6,
    )

    private val DANS_RE = Regex("""^dans (\d+) (minute|heure)s?$""")
    private val DAYTIME_RE = Regex("""^(?:(aujourd'hui|demain|après-demain) )?(\d{1,2}:\d{2})$""")
    private val WEEKDAY_RE = Regex("""^(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) (\d{1,2}:\d{2})$""")

    /**
     * @param quand  expression issue du modèle (ex. "demain 10:00", "dans 30 minutes")
     * @param now    instant de référence (paramétré pour la testabilité)
     * @return epoch-millis de l'occurrence future, ou null si non parseable
     */
    fun resolveEpochMs(quand: String, now: LocalDateTime = LocalDateTime.now()): Long? {
        val e = quand.trim().lowercase()

        // "dans N minutes/heures"
        DANS_RE.matchEntire(e)?.let { m ->
            val n = m.groupValues[1].toLong()
            val result = if (m.groupValues[2] == "minute") now.plusMinutes(n) else now.plusHours(n)
            return result.toEpochMs()
        }

        // "(aujourd'hui|demain|après-demain)? HH:MM"
        DAYTIME_RE.matchEntire(e)?.let { m ->
            val day = m.groupValues[1].takeIf { it.isNotEmpty() }
            val (h, mi) = parseHHMM(m.groupValues[2]) ?: return null
            var base = now.withHour(h).withMinute(mi).withSecond(0).withNano(0)
            when (day) {
                "demain"        -> base = base.plusDays(1)
                "après-demain"  -> base = base.plusDays(2)
                // "aujourd'hui" ou null → today, pas de correction si l'heure est passée (fidèle au Python)
            }
            return base.toEpochMs()
        }

        // "<jour de la semaine> HH:MM"
        WEEKDAY_RE.matchEntire(e)?.let { m ->
            val targetOrdinal = WEEKDAY_ORDINALS[m.groupValues[1]]!! // 0=lundi…6=dimanche
            val (h, mi) = parseHHMM(m.groupValues[2]) ?: return null
            val nowOrdinal = now.dayOfWeek.ordinal          // 0=Mon…6=Sun (Java ISO identique)
            var delta = (targetOrdinal - nowOrdinal + 7) % 7
            if (delta == 0) delta = 7                       // toujours futur — même règle que le Python
            return now.plusDays(delta.toLong())
                .withHour(h).withMinute(mi).withSecond(0).withNano(0)
                .toEpochMs()
        }

        return null
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun parseHHMM(s: String): Pair<Int, Int>? {
        val parts = s.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val mi = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (h !in 0..23 || mi !in 0..59) return null
        return h to mi
    }

    private fun LocalDateTime.toEpochMs(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
