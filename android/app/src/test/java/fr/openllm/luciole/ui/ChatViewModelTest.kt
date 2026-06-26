package fr.openllm.luciole.ui

import fr.openllm.luciole.cerveau.Cerveau
import fr.openllm.luciole.cerveau.CerveauIndisponible
import fr.openllm.luciole.mains.AffichageType
import fr.openllm.luciole.mains.Sortie
import fr.openllm.luciole.model.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `envoyer happy path ajoute message user puis message luciole avec sortie`() = runTest {
        val vm = ChatViewModel(
            cerveau = object : Cerveau {
                override suspend fun suggest(phrase: String) = Action.Note("mémo test")
            },
            resoudreContact = { null }
        )

        vm.envoyer("note mémo test")

        val state = vm.state.value
        assertFalse(state.enCours, "enCours doit être false après complétion")
        assertEquals(2, state.messages.size)
        assertEquals(Role.USER, state.messages[0].role)
        assertEquals("note mémo test", state.messages[0].texte)
        assertEquals(Role.LUCIOLE, state.messages[1].role)
        assertEquals("", state.messages[1].texte, "texte VM vide, l'UI localise depuis sortie")
        assertEquals(Sortie.Afficher(AffichageType.NOTE, "mémo test"), state.messages[1].sortie)
    }

    @Test
    fun `envoyer CerveauIndisponible ajoute message erreur sans sortie`() = runTest {
        val vm = ChatViewModel(
            cerveau = object : Cerveau {
                override suspend fun suggest(phrase: String): Action =
                    throw CerveauIndisponible(RuntimeException("réseau indisponible"))
            },
            resoudreContact = { null }
        )

        vm.envoyer("bonjour")

        val state = vm.state.value
        assertFalse(state.enCours, "enCours doit être false après erreur")
        assertEquals(2, state.messages.size)
        assertEquals(Role.USER, state.messages[0].role)
        assertEquals("bonjour", state.messages[0].texte)
        assertEquals(Role.LUCIOLE, state.messages[1].role)
        assertEquals("serveur_indisponible", state.messages[1].texte, "clé stable pour localisation UI")
        assertNull(state.messages[1].sortie, "message d'erreur n'a pas de sortie")
    }

    @Test
    fun `envoyer phrase vide ou blancs ne modifie pas l'état`() = runTest {
        val vm = ChatViewModel(
            cerveau = object : Cerveau {
                override suspend fun suggest(phrase: String): Action =
                    error("ne doit pas être appelé")
            },
            resoudreContact = { null }
        )

        vm.envoyer("   ")

        val state = vm.state.value
        assertTrue(state.messages.isEmpty(), "aucun message ne doit être ajouté pour une phrase vide")
        assertFalse(state.enCours, "enCours doit rester false")
    }
}
