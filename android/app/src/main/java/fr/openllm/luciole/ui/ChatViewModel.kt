package fr.openllm.luciole.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.openllm.luciole.cerveau.Cerveau
import fr.openllm.luciole.cerveau.CerveauIndisponible
import fr.openllm.luciole.mains.Mains
import fr.openllm.luciole.mains.Sortie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Role { USER, LUCIOLE }

data class Message(
    val role: Role,
    val texte: String,
    val sortie: Sortie? = null
)

data class ChatState(
    val messages: List<Message> = emptyList(),
    val enCours: Boolean = false
)

class ChatViewModel(
    private val cerveau: Cerveau,
    private val resoudreContact: (String) -> String?
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun envoyer(phrase: String) {
        _state.update { it.copy(
            messages = it.messages + Message(Role.USER, phrase),
            enCours = true
        )}

        viewModelScope.launch {
            try {
                val action = cerveau.suggest(phrase)
                val sortie = Mains.traiter(action, phrase, resoudreContact)
                val texte = when (sortie) {
                    is Sortie.Afficher -> sortie.texte
                    is Sortie.Lancer -> sortie.spec.action
                    is Sortie.ContactIntrouvable -> "Contact introuvable : ${sortie.nom}"
                }
                _state.update { it.copy(
                    messages = it.messages + Message(Role.LUCIOLE, texte, sortie),
                    enCours = false
                )}
            } catch (e: CerveauIndisponible) {
                _state.update { it.copy(
                    messages = it.messages + Message(Role.LUCIOLE, "Cerveau indisponible"),
                    enCours = false
                )}
            }
        }
    }
}
