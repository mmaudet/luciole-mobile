package fr.openllm.luciole.moniteur

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MoniteurViewModel(private val fetch: suspend () -> String) : ViewModel() {
    private val _state = MutableStateFlow(MetricsSnapshot(0, 0, 0.0))
    val state: StateFlow<MetricsSnapshot> = _state

    private var dernierTotal: Long? = null
    private val _historique = MutableStateFlow<List<Int>>(emptyList())
    val historique: StateFlow<List<Int>> = _historique

    suspend fun rafraichir() {
        val snapshot = Metrics.parse(fetch())
        _state.value = snapshot
        val delta = dernierTotal?.let { (snapshot.tokensTotal - it).coerceAtLeast(0L).toInt() } ?: 0
        dernierTotal = snapshot.tokensTotal
        _historique.value = (_historique.value + delta).takeLast(60)
    }
}
