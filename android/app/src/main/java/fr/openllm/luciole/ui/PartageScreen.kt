package fr.openllm.luciole.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.openllm.luciole.R
import fr.openllm.luciole.partage.HotspotEtat
import fr.openllm.luciole.partage.PartageViewModel
import fr.openllm.luciole.partage.qrBitmap
import fr.openllm.luciole.partage.wifiQr
import fr.openllm.luciole.ui.theme.Bleu
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.Encre
import fr.openllm.luciole.ui.theme.Fond
import fr.openllm.luciole.ui.theme.PlexMono
import fr.openllm.luciole.ui.theme.Spectral
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteMuet
import fr.openllm.luciole.ui.theme.Vert
import fr.openllm.luciole.ui.theme.VertClair

@Composable
fun PartageScreen(expanded: Boolean, modifier: Modifier = Modifier) {
    val vm: PartageViewModel = viewModel()
    val etat by vm.etat.collectAsState()
    val context = LocalContext.current

    val permsRequises = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val demander = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.demarrer()
    }
    val activer: () -> Unit = {
        if (permsRequises.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) vm.demarrer()
        else demander.launch(permsRequises)
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Fond)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (expanded) 34.dp else 20.dp, vertical = 22.dp),
    ) {
        Text(stringResource(R.string.partage_titre), fontFamily = Spectral, fontWeight = FontWeight.SemiBold, fontSize = if (expanded) 26.sp else 22.sp, color = Encre)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.partage_intro), fontSize = 14.sp, color = TexteMuet)
        Spacer(Modifier.height(22.dp))

        when (val e = etat) {
            HotspotEtat.Inactif -> BoutonPrincipal(stringResource(R.string.partage_activer), activer)
            HotspotEtat.Demarrage -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Bleu, strokeWidth = 2.dp)
                Text(stringResource(R.string.partage_demarrage), color = TexteMuet, fontSize = 14.sp)
            }
            is HotspotEtat.Actif -> Actif(e, expanded, onArreter = { vm.arreter() })
            is HotspotEtat.Erreur -> Column {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).border(1.dp, Bordure, RoundedCornerShape(14.dp)).padding(16.dp),
                ) { Text(e.raison, color = Encre, fontSize = 14.sp) }
                Spacer(Modifier.height(14.dp))
                BoutonPrincipal(stringResource(R.string.partage_reessayer), activer)
            }
        }
    }
}

@Composable
private fun Actif(e: HotspotEtat.Actif, expanded: Boolean, onArreter: () -> Unit) {
    val url = "http://${e.ip}:8080"
    val qrWifi = remember(e.ssid, e.motDePasse) { qrBitmap(wifiQr(e.ssid, e.motDePasse)) }
    val qrUrl = remember(url) { qrBitmap(url) }
    if (expanded) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QrCarte(stringResource(R.string.partage_wifi_titre), qrWifi, e.ssid, true)
            QrCarte(stringResource(R.string.partage_url_titre), qrUrl, url, true)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QrCarte(stringResource(R.string.partage_wifi_titre), qrWifi, e.ssid, false)
            QrCarte(stringResource(R.string.partage_url_titre), qrUrl, url, false)
        }
    }
    Spacer(Modifier.height(20.dp))
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(VertClair).clickable { onArreter() }.padding(horizontal = 20.dp, vertical = 12.dp),
    ) { Text(stringResource(R.string.partage_arreter), color = Vert, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@Composable
private fun QrCarte(titre: String, qr: ImageBitmap, sousTitre: String, expanded: Boolean) {
    val base = if (expanded) Modifier.width(300.dp) else Modifier.fillMaxWidth()
    Column(
        base.clip(RoundedCornerShape(18.dp)).background(Surface).border(1.dp, Bordure, RoundedCornerShape(18.dp)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(titre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Encre)
        Spacer(Modifier.height(14.dp))
        Image(qr, contentDescription = titre, modifier = Modifier.size(if (expanded) 240.dp else 220.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.height(12.dp))
        Text(sousTitre, fontFamily = PlexMono, fontSize = 12.5.sp, color = TexteMuet)
    }
}

@Composable
private fun BoutonPrincipal(texte: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Bleu).clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 15.dp),
    ) { Text(texte, color = Surface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}
