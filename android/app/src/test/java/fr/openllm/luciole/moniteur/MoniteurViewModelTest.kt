package fr.openllm.luciole.moniteur
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
class MoniteurViewModelTest {
    @Test fun rafraichirMetLesStats() = runTest {
        val vm = MoniteurViewModel { "llamacpp:tokens_predicted_total 100\nllamacpp:predicted_tokens_seconds 12.0" }
        vm.rafraichir()
        assertEquals(100L, vm.state.value.tokensTotal)
        assertEquals(12.0, vm.state.value.tokensPerSec, 0.01)
    }
}
