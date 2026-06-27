package fr.openllm.luciole

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import fr.openllm.luciole.cerveau.CerveauServeur
import fr.openllm.luciole.mains.Contacts
import fr.openllm.luciole.mains.Mains.lancer
import fr.openllm.luciole.mains.Sortie
import fr.openllm.luciole.moniteur.MoniteurViewModel
import fr.openllm.luciole.ui.ChatViewModel
import fr.openllm.luciole.ui.LucioleApp
import fr.openllm.luciole.ui.theme.LucioleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private val contactPermissionGranted = AtomicBoolean(false)
    private val contactPermissionDejaDemande = AtomicBoolean(false)

    private val requestContactPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            contactPermissionGranted.set(granted)
        }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
        val base = "http://127.0.0.1:8080"
        val cerveau = CerveauServeur(base, client)

        // Pré-chauffage silencieux : réchauffe le KV-cache du prompt système au lancement.
        lifecycleScope.launch { runCatching { cerveau.suggest("bonjour") } }

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

        setContent {
            // Bascule de langue FR/EN dans l'app : on fournit un Context dont la locale
            // est surchargée, ce qui réoriente tous les stringResource sans redémarrage.
            var langue by rememberSaveable { mutableStateOf("fr") }
            val baseCtx = LocalContext.current
            val ctxLocalise = remember(langue) {
                val conf = Configuration(baseCtx.resources.configuration)
                conf.setLocale(Locale(langue))
                baseCtx.createConfigurationContext(conf)
            }

            CompositionLocalProvider(LocalContext provides ctxLocalise) {
                LucioleTheme {
                    val expanded = calculateWindowSizeClass(this).widthSizeClass == WindowWidthSizeClass.Expanded

                    // Fire-once : on ne lance jamais le même intent deux fois (survit à la recréation).
                    val chatState by chatVm.state.collectAsState()
                    val messages = chatState.messages
                    var dernierIndex by rememberSaveable { mutableIntStateOf(-1) }
                    LaunchedEffect(messages.size) {
                        for (i in (dernierIndex + 1) until messages.size) {
                            val sortie = messages[i].sortie
                            if (sortie is Sortie.Lancer) this@MainActivity.lancer(sortie.spec)
                        }
                        if (messages.isNotEmpty()) dernierIndex = messages.size - 1
                    }

                    LucioleApp(
                        expanded = expanded,
                        chatVm = chatVm,
                        moniteurVm = moniteurVm,
                        langue = langue,
                        onSetLangue = { langue = it },
                        onEnvoyer = { texte ->
                            if (contactPermissionDejaDemande.compareAndSet(false, true)) {
                                requestContactPermission.launch(Manifest.permission.READ_CONTACTS)
                            }
                            chatVm.envoyer(texte)
                        },
                        onRelancer = { spec -> this@MainActivity.lancer(spec) },
                    )
                }
            }
        }
    }
}
