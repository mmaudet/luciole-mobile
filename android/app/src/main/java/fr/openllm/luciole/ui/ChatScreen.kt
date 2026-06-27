package fr.openllm.luciole.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import fr.openllm.luciole.R
import fr.openllm.luciole.mains.AffichageType
import fr.openllm.luciole.mains.Sortie

data class Gabarit(val labelRes: Int, val texte: String, val placeholder: String)

private val GABARITS = listOf(
    Gabarit(R.string.act_appel,      "appelle Marie Curie",                "Marie Curie"),
    Gabarit(R.string.act_minuteur,   "minuteur de 5 minutes",              "5"),
    Gabarit(R.string.act_alarme,     "réveille-moi à 7h30",               "7h30"),
    Gabarit(R.string.act_agenda,     "ajoute une réunion demain à 10h",    "une réunion"),
    Gabarit(R.string.act_message,    "envoie un SMS pour dire à bientôt",  "à bientôt"),
    Gabarit(R.string.act_itineraire, "itinéraire vers la gare de Lyon",    "la gare de Lyon"),
    Gabarit(R.string.act_recherche,  "cherche la capitale du Pérou",       "la capitale du Pérou"),
    Gabarit(R.string.act_traduction, "traduis bonjour en anglais",         "bonjour"),
    Gabarit(R.string.act_note,       "note : acheter du pain",             "acheter du pain"),
    Gabarit(R.string.act_ouvrir,     "ouvre YouTube",                      "YouTube"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel, onEnvoyer: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var saisie by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var showAide by remember { mutableStateOf(false) }
    var pendingFocus by remember { mutableStateOf(false) }

    LaunchedEffect(showAide, pendingFocus) {
        if (!showAide && pendingFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
            pendingFocus = false
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages) { m -> MessageCard(m) }
        }
        if (state.enCours) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = saisie,
                onValueChange = { saisie = it },
                modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.placeholder_saisie)) }
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Button(onClick = { showAide = true }) {
                    Text(stringResource(R.string.aide))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onEnvoyer(saisie.text)
                        saisie = TextFieldValue("")
                        keyboardController?.hide()
                        focus.clearFocus()
                    },
                    enabled = !state.enCours
                ) {
                    Text(stringResource(R.string.envoyer))
                }
            }
        }
    }

    if (showAide) {
        ModalBottomSheet(
            onDismissRequest = { showAide = false },
        ) {
            Text(
                stringResource(R.string.aide_titre),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            GABARITS.forEach { g ->
                ListItem(
                    headlineContent = { Text(stringResource(g.labelRes)) },
                    supportingContent = {
                        Text(g.texte, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.clickable {
                        val start = g.texte.indexOf(g.placeholder)
                        val sel = if (start >= 0) {
                            TextRange(start, start + g.placeholder.length)
                        } else {
                            TextRange(g.texte.length)
                        }
                        saisie = TextFieldValue(g.texte, sel)
                        showAide = false
                        pendingFocus = true
                    }
                )
                HorizontalDivider()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageCard(m: Message) {
    val texte = when (val s = m.sortie) {
        is Sortie.Lancer -> when (s.libelle) {
            "act_appel"      -> stringResource(R.string.act_appel)
            "act_alarme"     -> stringResource(R.string.act_alarme)
            "act_minuteur"   -> stringResource(R.string.act_minuteur)
            "act_agenda"     -> stringResource(R.string.act_agenda)
            "act_message"    -> stringResource(R.string.act_message)
            "act_itineraire" -> stringResource(R.string.act_itineraire)
            "act_recherche"  -> stringResource(R.string.act_recherche)
            "act_ouvrir"     -> stringResource(R.string.act_ouvrir)
            else             -> s.libelle
        }
        is Sortie.Afficher -> when (s.type) {
            AffichageType.NOTE        -> stringResource(R.string.note_prefixe, s.texte)
            AffichageType.TRADUCTION  -> s.texte
            AffichageType.INCONNU     -> stringResource(R.string.inconnu)
        }
        is Sortie.ContactIntrouvable -> stringResource(R.string.contact_introuvable, s.nom)
        null -> if (m.texte == "serveur_indisponible") stringResource(R.string.serveur_injoignable) else m.texte
    }
    val texteAvecDuree = if (m.dureeMs != null && m.sortie != null) {
        "$texte · ${stringResource(R.string.duree_traitement, "%.1f".format(m.dureeMs / 1000.0))}"
    } else {
        texte
    }
    Card(Modifier.fillMaxWidth()) { Text(texteAvecDuree, Modifier.padding(12.dp)) }
}
