package fr.openllm.luciole.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.moniteur.MoniteurViewModel

@Composable
fun MoniteurScreen(vm: MoniteurViewModel) {
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🔦 le Pixel sert les tokens", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Tokens servis", s.tokensTotal.toString(), Modifier.weight(1f))
            Stat("Débit tok/s", "%.0f".format(s.tokensPerSec), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Requêtes en cours", s.requests.toString(), Modifier.weight(1f))
            Stat("Cloud", "0", Modifier.weight(1f))
        }
        Text("🔒 Aucune donnée ne quitte le téléphone")
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
