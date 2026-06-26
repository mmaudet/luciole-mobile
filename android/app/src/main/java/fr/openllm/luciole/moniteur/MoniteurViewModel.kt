package fr.openllm.luciole.moniteur

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MoniteurViewModel(private val fetch: suspend () -> String) : ViewModel() {
    private val _state = MutableStateFlow(MetricsSnapshot(0, 0, 0.0))
    val state: StateFlow<MetricsSnapshot> = _state
    suspend fun rafraichir() { _state.value = Metrics.parse(fetch()) }
}
