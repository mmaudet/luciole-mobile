package fr.openllm.luciole.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.openllm.luciole.R
import fr.openllm.luciole.mains.IntentSpec
import fr.openllm.luciole.moniteur.MoniteurViewModel
import fr.openllm.luciole.ui.theme.BleuClair
import fr.openllm.luciole.ui.theme.Bordure
import fr.openllm.luciole.ui.theme.FondChamp
import fr.openllm.luciole.ui.theme.TexteMuet
import fr.openllm.luciole.ui.theme.Surface
import fr.openllm.luciole.ui.theme.TexteFaible
import fr.openllm.luciole.ui.theme.Vert
import fr.openllm.luciole.ui.theme.VertClair

private const val CHAT = 0
private const val AIDE = 1
private const val STATS = 2
private const val SCAN = 3
private const val PARTAGE = 4

@Composable
fun LucioleApp(
    expanded: Boolean,
    chatVm: ChatViewModel,
    moniteurVm: MoniteurViewModel,
    scanVm: ScanCarteViewModel,
    langue: String,
    onSetLangue: (String) -> Unit,
    onEnvoyer: (String) -> Unit,
    onRelancer: (IntentSpec) -> Unit,
    onOuvrirScanCarte: () -> Unit,
    onCreateContact: (fr.openllm.luciole.contact.ContactCard) -> Unit,
    onExportVcf: (fr.openllm.luciole.contact.ContactCard) -> Unit,
    requestOpenScan: Boolean = false,
    onScanOpened: () -> Unit = {},
) {
    var entered by rememberSaveable { mutableStateOf(false) }
    if (!entered) {
        OnboardingScreen(expanded, langue, onSetLangue, onCommencer = { entered = true })
        return
    }

    var showScan by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(requestOpenScan) {
        if (requestOpenScan) {
            showScan = true
            onScanOpened()
        }
    }
    if (showScan) {
        ScanCarteScreen(
            viewModel = scanVm,
            onBack = { showScan = false; scanVm.reset() },
            onCreateContact = { card -> onCreateContact(card); showScan = false; scanVm.reset() },
            onExportVcf = { card -> onExportVcf(card) },
        )
        return
    }

    val state by chatVm.state.collectAsState()
    var dest by rememberSaveable { mutableIntStateOf(CHAT) }
    var saisie by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var pendingFocus by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(pendingFocus) {
        if (pendingFocus) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
            pendingFocus = false
        }
    }

    val inserer: (String, String) -> Unit = { texte, placeholder ->
        val start = texte.indexOf(placeholder)
        val sel = if (start >= 0) TextRange(start, start + placeholder.length) else TextRange(texte.length)
        saisie = TextFieldValue(texte, sel)
        if (!expanded) dest = CHAT
        pendingFocus = true
    }

    val chatPane: @Composable (Modifier, Boolean) -> Unit = { mod, withAideButton ->
        ChatPane(
            expanded = expanded,
            messages = state.messages,
            enCours = state.enCours,
            saisie = saisie,
            onSaisieChange = { saisie = it },
            focusRequester = focusRequester,
            onEnvoyer = onEnvoyer,
            onRelancer = onRelancer,
            onEffacer = { chatVm.effacer() },
            onOuvrirAide = if (withAideButton) ({ dest = AIDE }) else null,
            onOuvrirScanCarte = { showScan = true },
            modifier = mod,
        )
    }

    if (expanded) {
        Row(Modifier.fillMaxSize()) {
            NavRail(dest, onSelect = { dest = it })
            when (dest) {
                STATS -> StatistiquesScreen(moniteurVm, expanded = true, modifier = Modifier.weight(1f).fillMaxHeight())
                SCAN -> Box(Modifier.weight(1f).fillMaxHeight()) {
                    ScanCarteScreen(
                        viewModel = scanVm,
                        onBack = { dest = CHAT },
                        onCreateContact = onCreateContact,
                        onExportVcf = onExportVcf,
                    )
                }
                PARTAGE -> PartageScreen(expanded = true, modifier = Modifier.weight(1f).fillMaxHeight())
                else -> Row(Modifier.weight(1f).fillMaxHeight()) {
                    chatPane(Modifier.weight(1f).fillMaxHeight(), false)
                    Box(
                        Modifier.width(384.dp).fillMaxHeight().background(Surface)
                            .border(width = 1.dp, color = Bordure, shape = RoundedCornerShape(0.dp)),
                    ) {
                        AideScreen(expanded = true, onInserer = inserer)
                    }
                }
            }
        }
    } else {
        Scaffold(bottomBar = { BottomNav(dest, onSelect = { dest = it }) }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (dest) {
                    AIDE -> AideScreen(expanded = false, onInserer = inserer)
                    STATS -> StatistiquesScreen(moniteurVm, expanded = false)
                    SCAN -> ScanCarteScreen(
                        viewModel = scanVm,
                        onBack = { dest = CHAT },
                        onCreateContact = onCreateContact,
                        onExportVcf = onExportVcf,
                    )
                    PARTAGE -> PartageScreen(expanded = false)
                    else -> chatPane(Modifier.fillMaxSize(), true)
                }
            }
        }
    }
}

@Composable
private fun NavRail(dest: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier.width(96.dp).fillMaxHeight().background(Surface)
            .border(width = 1.dp, color = Bordure, shape = RoundedCornerShape(0.dp))
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LucioleLogo(40.dp)
        Spacer(Modifier.size(34.dp))
        RailItem(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.nav_chat), dest == CHAT) { onSelect(CHAT) }
        Spacer(Modifier.size(8.dp))
        RailItem(Icons.Outlined.BarChart, stringResource(R.string.nav_stats), dest == STATS) { onSelect(STATS) }
        Spacer(Modifier.size(8.dp))
        RailItem(Icons.Outlined.DocumentScanner, stringResource(R.string.nav_scan), dest == SCAN) { onSelect(SCAN) }
        Spacer(Modifier.size(8.dp))
        RailItem(Icons.Outlined.QrCode2, stringResource(R.string.nav_partage), dest == PARTAGE) { onSelect(PARTAGE) }
        Spacer(Modifier.weight(1f))
        val connecte = rememberEstConnecte()
        val teinteConn = if (connecte) Vert else TexteMuet
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (connecte) VertClair else FondChamp), contentAlignment = Alignment.Center) {
            Icon(if (connecte) Icons.Outlined.Wifi else Icons.Outlined.WifiOff, null, tint = teinteConn, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.size(7.dp))
        Text(stringResource(if (connecte) R.string.reseau_ok else R.string.aucun_reseau), color = teinteConn, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
    }
}

@Composable
private fun RailItem(icone: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) BleuClair else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icone, null, tint = if (selected) MaterialTheme.colorScheme.primary else TexteFaible, modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else TexteFaible, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun BottomNav(dest: Int, onSelect: (Int) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Bordure))
        Row(Modifier.fillMaxWidth().background(Surface).padding(vertical = 8.dp)) {
            BottomItem(Modifier.weight(1f), Icons.Outlined.ChatBubbleOutline, stringResource(R.string.nav_chat), dest == CHAT) { onSelect(CHAT) }
            BottomItem(Modifier.weight(1f), Icons.AutoMirrored.Outlined.HelpOutline, stringResource(R.string.nav_aide), dest == AIDE) { onSelect(AIDE) }
            BottomItem(Modifier.weight(1f), Icons.Outlined.BarChart, stringResource(R.string.nav_stats), dest == STATS) { onSelect(STATS) }
            BottomItem(Modifier.weight(1f), Icons.Outlined.DocumentScanner, stringResource(R.string.nav_scan), dest == SCAN) { onSelect(SCAN) }
            BottomItem(Modifier.weight(1f), Icons.Outlined.QrCode2, stringResource(R.string.nav_partage), dest == PARTAGE) { onSelect(PARTAGE) }
        }
    }
}

@Composable
private fun BottomItem(modifier: Modifier, icone: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icone, null, tint = if (selected) MaterialTheme.colorScheme.primary else TexteFaible, modifier = Modifier.size(22.dp))
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else TexteFaible, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 10.5.sp)
    }
}
