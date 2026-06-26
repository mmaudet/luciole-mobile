package fr.openllm.luciole.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.openllm.luciole.R
import fr.openllm.luciole.mains.AffichageType
import fr.openllm.luciole.mains.Sortie

@Composable
fun ChatScreen(vm: ChatViewModel, onEnvoyer: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var saisie by remember { mutableStateOf("") }
    val exemples = stringArrayResource(R.array.exemples).toList()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages) { m -> MessageCard(m) }
        }
        if (state.enCours) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            exemples.take(3).forEach { ex ->
                AssistChip(onClick = { saisie = ex }, label = { Text(ex, maxLines = 1) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            OutlinedTextField(
                value = saisie,
                onValueChange = { saisie = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.placeholder_saisie)) }
            )
            Button(
                onClick = { onEnvoyer(saisie); saisie = "" },
                enabled = !state.enCours,
                modifier = Modifier.padding(start = 6.dp)
            ) { Text(stringResource(R.string.envoyer)) }
        }
    }
}

@Composable
private fun MessageCard(m: Message) {
    val texte = when (val s = m.sortie) {
        is Sortie.Lancer -> stringResource(R.string.action_prefixe, s.spec.action.substringAfterLast('.'))
        is Sortie.Afficher -> when (s.type) {
            AffichageType.NOTE -> stringResource(R.string.note_prefixe, s.texte)
            AffichageType.TRADUCTION -> s.texte
            AffichageType.INCONNU -> stringResource(R.string.inconnu)
        }
        is Sortie.ContactIntrouvable -> stringResource(R.string.contact_introuvable, s.nom)
        null -> if (m.texte == "serveur_indisponible") stringResource(R.string.serveur_injoignable) else m.texte
    }
    Card(Modifier.fillMaxWidth()) { Text(texte, Modifier.padding(12.dp)) }
}
