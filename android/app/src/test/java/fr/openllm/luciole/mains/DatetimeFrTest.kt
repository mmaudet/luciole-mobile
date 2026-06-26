package fr.openllm.luciole.mains

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests du port Kotlin de dispatcher/datetime_fr.py.
 * now fixé au samedi 27 juin 2026 09:00 pour reproductibilité.
 *
 * Attendus calculés avec la même formule que l'implémentation
 * (ZoneId.systemDefault()) — timezone-agnostique.
 */
class DatetimeFrTest {

    private val now = LocalDateTime.of(2026, 6, 27, 9, 0) // Samedi

    private fun epochOf(ldt: LocalDateTime): Long =
        ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ── dans N minutes / heures ────────────────────────────────────────────

    @Test fun dansNMinutes() {
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 9, 30)),
            DatetimeFr.resolveEpochMs("dans 30 minutes", now)
        )
    }

    @Test fun dansUneMinute() {
        // singulier accepté
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 9, 1)),
            DatetimeFr.resolveEpochMs("dans 1 minute", now)
        )
    }

    @Test fun dansNHeures() {
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 11, 0)),
            DatetimeFr.resolveEpochMs("dans 2 heures", now)
        )
    }

    @Test fun dansUneHeure() {
        // singulier accepté
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 10, 0)),
            DatetimeFr.resolveEpochMs("dans 1 heure", now)
        )
    }

    // ── HH:MM bare et préfixes de jour ────────────────────────────────────

    @Test fun bareHHMM() {
        // Pas de correction si l'heure est passée — fidèle au Python
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 10, 0)),
            DatetimeFr.resolveEpochMs("10:00", now)
        )
    }

    @Test fun bareHHMMSingleDigit() {
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 9, 15)),
            DatetimeFr.resolveEpochMs("9:15", now)
        )
    }

    @Test fun aujourdhuiHHMM() {
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 27, 14, 0)),
            DatetimeFr.resolveEpochMs("aujourd'hui 14:00", now)
        )
    }

    @Test fun demainHHMM() {
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 28, 10, 0)),
            DatetimeFr.resolveEpochMs("demain 10:00", now)
        )
    }

    @Test fun apresDemainHHMM() {
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 29, 14, 30)),
            DatetimeFr.resolveEpochMs("après-demain 14:30", now)
        )
    }

    // ── Jours de la semaine ───────────────────────────────────────────────
    // now = samedi (ordinal 5 dans 0=lundi…6=dim)

    @Test fun lundiHHMM() {
        // delta = (0-5+7)%7 = 2 → lundi 29 juin 2026
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 29, 9, 15)),
            DatetimeFr.resolveEpochMs("lundi 09:15", now)
        )
    }

    @Test fun mardiHHMM() {
        // delta = (1-5+7)%7 = 3 → mardi 30 juin 2026
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 30, 8, 0)),
            DatetimeFr.resolveEpochMs("mardi 08:00", now)
        )
    }

    @Test fun vendrediHHMM() {
        // delta = (4-5+7)%7 = 6 → vendredi 3 juillet 2026
        assertEquals(
            epochOf(LocalDateTime.of(2026, 7, 3, 8, 0)),
            DatetimeFr.resolveEpochMs("vendredi 08:00", now)
        )
    }

    @Test fun dimancheHHMM() {
        // delta = (6-5+7)%7 = 1 → dimanche 28 juin 2026
        assertEquals(
            epochOf(LocalDateTime.of(2026, 6, 28, 18, 0)),
            DatetimeFr.resolveEpochMs("dimanche 18:00", now)
        )
    }

    @Test fun samediEstToujoursFutur() {
        // Même jour → delta=0 → forcé à 7 (comme le Python) → samedi 4 juillet 2026
        assertEquals(
            epochOf(LocalDateTime.of(2026, 7, 4, 12, 0)),
            DatetimeFr.resolveEpochMs("samedi 12:00", now)
        )
    }

    // ── Cas non parseable → null ───────────────────────────────────────────

    @Test fun nonParseable() {
        assertNull(DatetimeFr.resolveEpochMs("n'importe quoi", now))
    }

    @Test fun chaineVide() {
        assertNull(DatetimeFr.resolveEpochMs("", now))
    }

    @Test fun demainSansHeure() {
        // "demain" seul ne matche pas — le modèle doit toujours émettre "demain HH:MM"
        assertNull(DatetimeFr.resolveEpochMs("demain", now))
    }

    @Test fun heureSansSeparateur() {
        assertNull(DatetimeFr.resolveEpochMs("1000", now))
    }
}
