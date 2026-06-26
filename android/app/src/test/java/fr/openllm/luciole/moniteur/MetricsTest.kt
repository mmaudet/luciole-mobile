package fr.openllm.luciole.moniteur
import kotlin.test.Test
import kotlin.test.assertEquals
class MetricsTest {
    @Test fun parseLesCompteurs() {
        val txt = """
            # HELP llamacpp:tokens_predicted_total ...
            llamacpp:tokens_predicted_total 847392
            llamacpp:prompt_tokens_total 12000
            llamacpp:requests_processing 3
            llamacpp:predicted_tokens_seconds 41.5
        """.trimIndent()
        val s = Metrics.parse(txt)
        assertEquals(847392L, s.tokensTotal)
        assertEquals(41.5, s.tokensPerSec, 0.01)
    }
    @Test fun cleAbsenteDonneZero() {
        assertEquals(0L, Metrics.parse("").tokensTotal)
    }
}
