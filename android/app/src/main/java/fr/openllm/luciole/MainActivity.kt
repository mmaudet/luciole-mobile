package fr.openllm.luciole

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.openllm.luciole.cerveau.CerveauServeur
import fr.openllm.luciole.mains.Contacts
import fr.openllm.luciole.mains.Mains.lancer
import fr.openllm.luciole.mains.Sortie
import fr.openllm.luciole.moniteur.MoniteurViewModel
import fr.openllm.luciole.ui.ChatScreen
import fr.openllm.luciole.ui.ChatViewModel
import fr.openllm.luciole.ui.MoniteurScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    // Permission READ_CONTACTS — doit être enregistrée avant onStart()
    private val contactPermissionGranted = AtomicBoolean(false)

    private val requestContactPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            contactPermissionGranted.set(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = OkHttpClient()
        val base = "http://127.0.0.1:8080"

        val cerveau = CerveauServeur(base, client)

        // Le résolveur de contact lit la valeur atomique au moment de l'appel.
        // Si la permission est refusée ou si la requête échoue, on renvoie null
        // → ChatViewModel produit un Sortie.ContactIntrouvable, pas de crash.
        val chatVm = ChatViewModel(cerveau) { nom ->
            if (!contactPermissionGranted.get()) return@ChatViewModel null
            try { Contacts.resolve(nom, contentResolver) } catch (e: Exception) { null }
        }

        val moniteurVm = MoniteurViewModel {
            withContext(Dispatchers.IO) {
                client.newCall(Request.Builder().url("$base/metrics").build())
                    .execute()
                    .use { it.body?.string().orEmpty() }
            }
        }

        // Demander la permission READ_CONTACTS au lancement
        requestContactPermission.launch(Manifest.permission.READ_CONTACTS)

        setContent {
            MaterialTheme {
                var onglet by rememberSaveable { mutableIntStateOf(0) }
                val chatState by chatVm.state.collectAsState()
                val messages = chatState.messages

                // Mécanisme fire-once : on ne lance jamais le même intent deux fois,
                // même si l'Activity est recréée (rememberSaveable survit à la rotation).
                var dernierIndex by rememberSaveable { mutableIntStateOf(-1) }
                LaunchedEffect(messages.size) {
                    for (i in (dernierIndex + 1) until messages.size) {
                        val sortie = messages[i].sortie
                        if (sortie is Sortie.Lancer) {
                            lancer(sortie.spec)
                        }
                    }
                    // Avancer l'index même s'il n'y avait pas de Lancer,
                    // pour ne pas réexaminer les anciens messages.
                    if (messages.isNotEmpty()) {
                        dernierIndex = messages.size - 1
                    }
                }

                // Boucle de polling du Moniteur — une erreur réseau est avalée et
                // relancée à l'itération suivante (1 s plus tard).
                LaunchedEffect(Unit) {
                    while (true) {
                        try {
                            moniteurVm.rafraichir()
                        } catch (e: Exception) {
                            // poll échoué : le /metrics n'est pas encore disponible ou
                            // le serveur est arrêté — on réessaie dans 1 s
                        }
                        delay(1_000)
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = onglet == 0,
                                onClick = { onglet = 0 },
                                icon = {},
                                label = { Text(stringResource(R.string.nav_chat)) }
                            )
                            NavigationBarItem(
                                selected = onglet == 1,
                                onClick = { onglet = 1 },
                                icon = {},
                                label = { Text(stringResource(R.string.nav_moniteur)) }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(Modifier.padding(paddingValues)) {
                        if (onglet == 0) {
                            ChatScreen(chatVm, EXEMPLES)
                        } else {
                            MoniteurScreen(moniteurVm)
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** Phrases d'exemple affichées comme chips dans ChatScreen. */
        val EXEMPLES = listOf(
            "mets un minuteur de 5 minutes",
            "appelle Paul Maudet",
            "itinéraire vers la gare"
        )
    }
}
