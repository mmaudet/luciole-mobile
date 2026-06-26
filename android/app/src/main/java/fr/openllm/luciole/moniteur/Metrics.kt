package fr.openllm.luciole.moniteur

data class MetricsSnapshot(val tokensTotal: Long, val requests: Long, val tokensPerSec: Double)

object Metrics {
    fun parse(prometheus: String): MetricsSnapshot {
        val vals = HashMap<String, Double>()
        for (line in prometheus.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val sp = l.lastIndexOf(' ')
            if (sp <= 0) continue
            val key = l.substring(0, sp).substringBefore('{').trim()
            l.substring(sp + 1).trim().toDoubleOrNull()?.let { vals[key] = it }
        }
        fun g(k: String) = vals[k] ?: 0.0
        return MetricsSnapshot(
            tokensTotal = g("llamacpp:tokens_predicted_total").toLong(),
            requests = g("llamacpp:requests_processing").toLong(),
            tokensPerSec = g("llamacpp:predicted_tokens_seconds"))
    }
}
