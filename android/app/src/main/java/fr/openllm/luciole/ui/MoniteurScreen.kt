package fr.openllm.luciole.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.moniteur.MoniteurViewModel

@Composable
fun MoniteurScreen(vm: MoniteurViewModel) {
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.moniteur_titre), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(stringResource(R.string.stat_tokens), s.tokensTotal.toString(), Modifier.weight(1f))
            Stat(stringResource(R.string.stat_debit), "%.0f".format(s.tokensPerSec), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(stringResource(R.string.stat_requetes), s.requests.toString(), Modifier.weight(1f))
            Stat(stringResource(R.string.pas_de_cloud), "0", Modifier.weight(1f))
        }
        Text(stringResource(R.string.aucune_donnee))
    }
}

@Composable
private fun Stat(label: String, valeur: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp)
            Text(valeur, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
    }
}
